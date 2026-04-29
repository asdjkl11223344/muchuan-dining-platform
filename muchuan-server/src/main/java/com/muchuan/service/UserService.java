package com.muchuan.service;

import com.muchuan.dto.UserLoginDTO;
import com.muchuan.entity.User;

public interface UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}

