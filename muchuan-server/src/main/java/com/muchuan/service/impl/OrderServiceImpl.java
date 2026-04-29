package com.muchuan.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.muchuan.constant.MessageConstant;
import com.muchuan.context.BaseContext;
import com.muchuan.dto.*;
import com.muchuan.entity.*;
import com.muchuan.event.OrderLifecycleEvent;
import com.muchuan.exception.AddressBookBusinessException;
import com.muchuan.exception.OrderBusinessException;
import com.muchuan.exception.ShoppingCartBusinessException;
import com.muchuan.mapper.*;
import com.muchuan.result.PageResult;
import com.muchuan.service.OrderService;
import com.muchuan.service.support.OrderNumberGenerator;
import com.muchuan.service.support.OrderTimelineService;
import com.muchuan.service.support.RedisLockService;
import com.muchuan.utils.HttpClientUtil;
import com.muchuan.utils.WeChatPayUtil;
import com.muchuan.vo.OrderOperationLogVO;
import com.muchuan.vo.OrderPaymentVO;
import com.muchuan.vo.OrderStatisticsVO;
import com.muchuan.vo.OrderSubmitVO;
import com.muchuan.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_SUBMIT_LOCK_KEY_PREFIX = "order:submit:lock:";
    private static final String ORDER_SUBMIT_RESULT_KEY_PREFIX = "order:submit:result:";
    private static final String ORDER_PAY_CALLBACK_LOCK_KEY_PREFIX = "order:pay:callback:";

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private RedisLockService redisLockService;
    @Autowired
    private OrderNumberGenerator orderNumberGenerator;
    @Autowired
    private OrderTimelineService orderTimelineService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Value("${muchuan.shop.address}")
    private String shopAddress;

    @Value("${muchuan.baidu.ak}")
    private String ak;

    @Value("${muchuan.order.submit.lock-seconds:5}")
    private long orderSubmitLockSeconds;

    @Value("${muchuan.order.submit.result-ttl-seconds:10}")
    private long orderSubmitResultTtlSeconds;

    @Value("${muchuan.order.pay.callback-lock-seconds:10}")
    private long payCallbackLockSeconds;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();
        String fingerprint = buildSubmitFingerprint(userId, ordersSubmitDTO);

        OrderSubmitVO recentResult = getRecentSubmitResult(userId, fingerprint);
        if (recentResult != null) {
            return recentResult;
        }

        String lockKey = ORDER_SUBMIT_LOCK_KEY_PREFIX + userId;
        String lockValue = UUID.randomUUID().toString();
        boolean locked = redisLockService.tryLock(lockKey, lockValue, Duration.ofSeconds(orderSubmitLockSeconds));
        if (!locked) {
            recentResult = getRecentSubmitResult(userId, fingerprint);
            if (recentResult != null) {
                return recentResult;
            }
            throw new OrderBusinessException(MessageConstant.ORDER_SUBMIT_IN_PROGRESS);
        }

        try {
            recentResult = getRecentSubmitResult(userId, fingerprint);
            if (recentResult != null) {
                return recentResult;
            }

            AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
            if (addressBook == null) {
                throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
            }

            //检查用户的收货地址是否超出配送范围
            //checkOutOfRange(addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());

            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userId);
            List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
            if (CollectionUtils.isEmpty(shoppingCartList)) {
                throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
            }

            Orders orders = new Orders();
            BeanUtils.copyProperties(ordersSubmitDTO, orders);
            orders.setOrderTime(LocalDateTime.now());
            orders.setPayStatus(Orders.UN_PAID);
            orders.setStatus(Orders.PENDING_PAYMENT);
            orders.setNumber(orderNumberGenerator.nextNumber());
            orders.setAddress(addressBook.getDetail());
            orders.setPhone(addressBook.getPhone());
            orders.setConsignee(addressBook.getConsignee());
            orders.setUserId(userId);

            orderMapper.insert(orders);

            List<OrderDetail> orderDetailList = new ArrayList<>();
            for (ShoppingCart cart : shoppingCartList) {
                OrderDetail orderDetail = new OrderDetail();
                BeanUtils.copyProperties(cart, orderDetail);
                orderDetail.setOrderId(orders.getId());
                orderDetailList.add(orderDetail);
            }
            orderDetailMapper.insertBatch(orderDetailList);

            shoppingCartMapper.deleteByUserId(userId);

            OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                    .id(orders.getId())
                    .orderTime(orders.getOrderTime())
                    .orderNumber(orders.getNumber())
                    .orderAmount(orders.getAmount())
                    .build();

            cacheRecentSubmitResultAfterCommit(userId, fingerprint, orderSubmitVO);
            publishLifecycleEvent(orders.getId(), orders.getNumber(), null, orders.getStatus(),
                    "提交订单", "用户", userId, resolveUserLabel(userId), "订单创建成功", null, null);

            return orderSubmitVO;
        } finally {
            redisLockService.unlock(lockKey, lockValue);
        }
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     *
     * @param address
     */
    private void checkOutOfRange(String address) {
        Map<String, String> map = new HashMap<>();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("店铺地址解析失败");
        }

        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        String shopLngLat = lat + "," + lng;

        map.put("address", address);
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }

        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        String userLngLat = lat + "," + lng;

        map.put("origin", shopLngLat);
        map.put("destination", userLngLat);
        map.put("steps_info", "0");

        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if (!jsonObject.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if (distance > 5000) {
            throw new OrderBusinessException("超出配送范围");
        }
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Orders orders = orderMapper.getByNumberAndUserId(ordersPaymentDTO.getOrderNumber(), userId);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        User user = userMapper.getById(userId);
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(),
                new BigDecimal("0.01"),
                "沐川餐饮订单",
                user.getOpenid()
        );

        if ("ORDERPAID".equals(jsonObject.getString("code"))) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Transactional
    public void paySuccess(String outTradeNo) {
        String lockKey = ORDER_PAY_CALLBACK_LOCK_KEY_PREFIX + outTradeNo;
        String lockValue = UUID.randomUUID().toString();
        boolean locked = redisLockService.tryLock(lockKey, lockValue, Duration.ofSeconds(payCallbackLockSeconds));
        if (!locked) {
            log.warn("支付回调正在处理，忽略重复请求：{}", outTradeNo);
            return;
        }

        try {
            Orders ordersDB = orderMapper.getByNumber(outTradeNo);
            if (ordersDB == null) {
                throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
            }

            if (Orders.PAID.equals(ordersDB.getPayStatus())) {
                log.info("订单已完成支付回调处理，忽略重复通知：{}", outTradeNo);
                return;
            }
            if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())) {
                log.warn("订单状态不是待支付，忽略支付回调，orderNumber={}, status={}", outTradeNo, ordersDB.getStatus());
                return;
            }

            Orders orders = Orders.builder()
                    .id(ordersDB.getId())
                    .status(Orders.TO_BE_CONFIRMED)
                    .payStatus(Orders.PAID)
                    .checkoutTime(LocalDateTime.now())
                    .build();

            orderMapper.update(orders);

            publishLifecycleEvent(ordersDB.getId(), ordersDB.getNumber(), ordersDB.getStatus(), Orders.TO_BE_CONFIRMED,
                    "支付成功", "系统", null, "微信支付回调", "支付成功，等待商家接单", 1, "订单号：" + outTradeNo);
        } finally {
            redisLockService.unlock(lockKey, lockValue);
        }
    }

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                Long orderId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        Orders orders = getOrderByIdOrThrow(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    @Transactional
    public void userCancelById(Long id) throws Exception {
        Orders ordersDB = getOrderByIdOrThrow(id);

        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

        Long userId = BaseContext.getCurrentId();
        publishLifecycleEvent(ordersDB.getId(), ordersDB.getNumber(), ordersDB.getStatus(), Orders.CANCELLED,
                "取消订单", "用户", userId, resolveUserLabel(userId), "用户取消订单", null, null);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> orderDishList = orderDetailList.stream().map(x -> x.getName() + "*" + x.getNumber() + ";")
                .collect(Collectors.toList());

        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    /**
     * 接单
     *
     * @param ordersConfirmDTO
     */
    @Transactional
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders ordersDB = getOrderByIdOrThrow(ordersConfirmDTO.getId());
        if (!Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);

        Long employeeId = BaseContext.getCurrentId();
        publishLifecycleEvent(ordersDB.getId(), ordersDB.getNumber(), ordersDB.getStatus(), Orders.CONFIRMED,
                "商家接单", "商家", employeeId, resolveEmployeeLabel(employeeId), "商家已确认接单", null, null);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Transactional
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        Orders ordersDB = getOrderByIdOrThrow(ordersRejectionDTO.getId());

        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        if (ordersDB.getPayStatus() == Orders.PAID) {
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));
            log.info("申请退款：{}", refund);
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

        Long employeeId = BaseContext.getCurrentId();
        publishLifecycleEvent(ordersDB.getId(), ordersDB.getNumber(), ordersDB.getStatus(), Orders.CANCELLED,
                "商家拒单", "商家", employeeId, resolveEmployeeLabel(employeeId), ordersRejectionDTO.getRejectionReason(), null, null);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    @Transactional
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders ordersDB = getOrderByIdOrThrow(ordersCancelDTO.getId());
        if (Orders.COMPLETED.equals(ordersDB.getStatus()) || Orders.CANCELLED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        if (ordersDB.getPayStatus() == Orders.PAID) {
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal("0.01"),
                    new BigDecimal("0.01"));
            log.info("申请退款：{}", refund);
            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);

        Long employeeId = BaseContext.getCurrentId();
        publishLifecycleEvent(ordersDB.getId(), ordersDB.getNumber(), ordersDB.getStatus(), Orders.CANCELLED,
                "商家取消订单", "商家", employeeId, resolveEmployeeLabel(employeeId), ordersCancelDTO.getCancelReason(), null, null);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Transactional
    public void delivery(Long id) {
        Orders ordersDB = getOrderByIdOrThrow(id);

        if (!ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);

        Long employeeId = BaseContext.getCurrentId();
        publishLifecycleEvent(ordersDB.getId(), ordersDB.getNumber(), ordersDB.getStatus(), Orders.DELIVERY_IN_PROGRESS,
                "开始派送", "商家", employeeId, resolveEmployeeLabel(employeeId), "订单进入配送中", null, null);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Transactional
    public void complete(Long id) {
        Orders ordersDB = getOrderByIdOrThrow(id);

        if (!ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);

        Long employeeId = BaseContext.getCurrentId();
        publishLifecycleEvent(ordersDB.getId(), ordersDB.getNumber(), ordersDB.getStatus(), Orders.COMPLETED,
                "完成订单", "商家", employeeId, resolveEmployeeLabel(employeeId), "订单已完成", null, null);
    }

    /**
     * 客户催单
     *
     * @param id
     */
    public void reminder(Long id) {
        Orders ordersDB = getOrderByIdOrThrow(id);
        Long userId = BaseContext.getCurrentId();

        publishLifecycleEvent(ordersDB.getId(), ordersDB.getNumber(), ordersDB.getStatus(), ordersDB.getStatus(),
                "用户催单", "用户", userId, resolveUserLabel(userId), "用户发起催单", 2, "订单号：" + ordersDB.getNumber());
    }

    /**
     * 查询订单操作时间线
     *
     * @param id
     * @return
     */
    public List<OrderOperationLogVO> getOrderTimeline(Long id) {
        getOrderByIdOrThrow(id);
        return orderTimelineService.listByOrderId(id);
    }

    private Orders getOrderByIdOrThrow(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return orders;
    }

    private OrderSubmitVO getRecentSubmitResult(Long userId, String fingerprint) {
        String cacheValue = stringRedisTemplate.opsForValue().get(buildSubmitResultKey(userId, fingerprint));
        if (cacheValue == null) {
            return null;
        }
        return JSON.parseObject(cacheValue, OrderSubmitVO.class);
    }

    private void cacheRecentSubmitResultAfterCommit(Long userId, String fingerprint, OrderSubmitVO orderSubmitVO) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cacheRecentSubmitResult(userId, fingerprint, orderSubmitVO);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheRecentSubmitResult(userId, fingerprint, orderSubmitVO);
            }
        });
    }

    private void cacheRecentSubmitResult(Long userId, String fingerprint, OrderSubmitVO orderSubmitVO) {
        stringRedisTemplate.opsForValue().set(buildSubmitResultKey(userId, fingerprint), JSON.toJSONString(orderSubmitVO),
                Duration.ofSeconds(orderSubmitResultTtlSeconds));
    }

    private String buildSubmitResultKey(Long userId, String fingerprint) {
        return ORDER_SUBMIT_RESULT_KEY_PREFIX + userId + ":" + fingerprint;
    }

    private String buildSubmitFingerprint(Long userId, OrdersSubmitDTO ordersSubmitDTO) {
        String raw = userId + "|" +
                valueOf(ordersSubmitDTO.getAddressBookId()) + "|" +
                valueOf(ordersSubmitDTO.getPayMethod()) + "|" +
                valueOf(ordersSubmitDTO.getRemark()) + "|" +
                valueOf(ordersSubmitDTO.getEstimatedDeliveryTime()) + "|" +
                valueOf(ordersSubmitDTO.getDeliveryStatus()) + "|" +
                valueOf(ordersSubmitDTO.getTablewareNumber()) + "|" +
                valueOf(ordersSubmitDTO.getTablewareStatus()) + "|" +
                valueOf(ordersSubmitDTO.getPackAmount()) + "|" +
                valueOf(ordersSubmitDTO.getAmount());
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String resolveUserLabel(Long userId) {
        User user = userMapper.getById(userId);
        if (user == null) {
            return "用户#" + userId;
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            return "用户#" + userId;
        }
        return user.getName() + "(用户#" + userId + ")";
    }

    private String resolveEmployeeLabel(Long employeeId) {
        Employee employee = employeeMapper.getById(employeeId);
        if (employee == null) {
            return "商家#" + employeeId;
        }
        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            return "商家#" + employeeId;
        }
        return employee.getName() + "(商家#" + employeeId + ")";
    }

    private void publishLifecycleEvent(Long orderId, String orderNumber, Integer beforeStatus, Integer afterStatus,
                                       String action, String operatorType, Long operatorId, String operatorLabel,
                                       String remark, Integer notifyType, String notifyContent) {
        OrderOperationLogVO operationLogVO = OrderOperationLogVO.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .action(action)
                .beforeStatus(beforeStatus)
                .beforeStatusDesc(getStatusDesc(beforeStatus))
                .afterStatus(afterStatus)
                .afterStatusDesc(getStatusDesc(afterStatus))
                .operatorType(operatorType)
                .operatorId(operatorId)
                .operatorLabel(operatorLabel)
                .remark(remark)
                .operateTime(LocalDateTime.now())
                .build();

        applicationEventPublisher.publishEvent(new OrderLifecycleEvent(operationLogVO, notifyType, notifyContent));
    }

    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未创建";
        }
        switch (status) {
            case 1:
                return "待付款";
            case 2:
                return "待接单";
            case 3:
                return "已接单";
            case 4:
                return "派送中";
            case 5:
                return "已完成";
            case 6:
                return "已取消";
            default:
                return "未知状态";
        }
    }
}

