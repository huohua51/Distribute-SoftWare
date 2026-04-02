package com.example.user.service;

import com.example.user.entity.UserDO;

public interface UserService {

    UserDO getOrCreateGuestUser(Long userId);
}
