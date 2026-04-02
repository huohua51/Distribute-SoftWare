package com.example.user.service.impl;

import com.example.user.entity.UserDO;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDO getOrCreateGuestUser(Long userId) {
        UserDO user = userMapper.findByUserId(userId);
        if (user != null) {
            return user;
        }

        UserDO candidate = new UserDO();
        candidate.setUserId(userId);
        candidate.setUsername("guest-" + userId);
        candidate.setStatus("ACTIVE");
        try {
            userMapper.insert(candidate);
        } catch (DuplicateKeyException ignored) {
            // Concurrent requests may insert the same synthetic user.
        }
        return userMapper.findByUserId(userId);
    }
}
