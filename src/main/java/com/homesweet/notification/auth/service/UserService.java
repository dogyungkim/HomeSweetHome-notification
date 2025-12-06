package com.homesweet.notification.auth.service;

import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.repository.UserRepository;
import com.homesweet.notification.exception.BusinessException;
import com.homesweet.notification.exception.ErrorCode;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<User> getManyUsersById(List<Long> userIds) {
        return userRepository.findAllByIdIn(userIds);
    }

}
