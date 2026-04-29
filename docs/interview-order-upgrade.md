# 订单链路稳定性升级说明

## 这次改造解决了什么问题

### 1. 订单重复提交
- 问题：用户在弱网或双击场景下，可能重复触发下单请求。
- 方案：基于 Redis 分布式锁限制同一用户的并发下单，并用短期结果缓存实现“重试返回上次结果”。
- 价值：避免重复创建订单、重复扣减购物车，提升幂等性和用户体验。

### 2. 支付回调不可靠
- 问题：原实现依赖 `BaseContext` 获取登录用户，但微信支付回调属于异步通知，不存在登录态。
- 方案：改为通过订单号直接查询订单，并用 Redis 锁保证支付回调在并发场景下只处理一次。
- 价值：修复真实线上隐患，支付链路更稳。

### 3. 订单流程缺乏审计
- 问题：订单状态流转只能改库，缺少可追踪的操作日志，排查问题困难。
- 方案：引入订单生命周期事件，在事务提交后记录 Redis 时间线，并保留 WebSocket 通知能力。
- 价值：便于排障，也方便在面试里讲“事件驱动解耦”和“可观测性建设”。

## 本次新增能力

### Redis 分布式锁
- `order:submit:lock:{userId}`：控制同一用户重复下单
- `order:pay:callback:{orderNumber}`：控制支付回调幂等

### 短期幂等结果缓存
- `order:submit:result:{userId}:{fingerprint}`
- 默认保留 10 秒
- 适用于前端超时重试、用户重复点击等场景

### 订单号生成器
- 基于 Redis 自增序列生成订单号
- 相比 `System.currentTimeMillis()`，更适合高并发和多实例场景

### 订单时间线接口
- 新增能力：后台可查询订单时间线
- 返回订单的提交、支付、接单、拒单、取消、派送、完成、催单等操作轨迹

## 关键配置

配置位置：`muchuan-server/src/main/resources/application.yml`

- `muchuan.order.submit.lock-seconds`
- `muchuan.order.submit.result-ttl-seconds`
- `muchuan.order.pay.callback-lock-seconds`
- `muchuan.order.timeline.ttl-days`
- `muchuan.order.timeline.max-size`

## 面试可以怎么讲

### 你可以从“线上问题”切入
- 用户双击下单会造成重复订单，我通过 Redis 锁和幂等结果缓存做了链路保护。
- 微信支付回调本质是异步系统通知，不能依赖用户登录态，所以我把回调处理改成了按订单号查询并加幂等锁。

### 你可以从“架构设计”切入
- 我没有把审计日志、通知逻辑直接写死在订单主流程里，而是通过 Spring 事务事件在 `AFTER_COMMIT` 阶段统一处理。
- 这样做的好处是订单主链路更聚焦，审计、通知、后续扩展短信/消息队列都更容易接入。

### 你可以从“工程化”切入
- 为了适配当前 JDK，我把 Lombok 升级到了 `1.18.32`，让项目可以稳定 `clean compile`。
- 这类基础依赖治理，也是面试里很加分的点。

