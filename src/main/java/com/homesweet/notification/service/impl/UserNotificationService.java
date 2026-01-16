package com.homesweet.notification.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.domain.NotificationTemplateType;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.repository.NotificationTemplateRepository;
import com.homesweet.notification.repository.UserNotificationJdbcRepository;
import com.homesweet.notification.repository.UserNotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * 사용자 알림을 생성하고 저장하는 서비스입니다.
 * 
 * @author dogyungkim
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserNotificationService {

    private final NotificationTemplateRepository notificationTemplateRepository;
    private final UserNotificationJdbcRepository userNotificationJdbcRepository;
    private final UserNotificationRepository userNotificationRepository;

    /**
     * 사용자 알림을 대량으로 저장합니다.
     * 
     * @param userNotifications 사용자 알림 리스트
     * @throws IllegalArgumentException 알림 리스트가 null인 경우
     */
    @Transactional
    public void bulkInsertUserNotifications(List<UserNotification> userNotifications) {
        if (userNotifications == null) {
            throw new IllegalArgumentException("사용자 알림 리스트는 null일 수 없습니다.");
        }

        if (userNotifications.isEmpty()) {
            log.warn("저장할 사용자 알림이 없습니다.");
            return;
        }

        userNotificationJdbcRepository.saveAll(userNotifications);
    }

    /**
     * 사용자 알림을 단건 저장합니다.
     *
     * @param userNotification 사용자 알림
     * @throws IllegalArgumentException 알림이 null인 경우
     */
    @Transactional
    public void saveUserNotification(UserNotification userNotification) {
        if (userNotification == null) {
            throw new IllegalArgumentException("사용자 알림은 null일 수 없습니다.");
        }
        userNotificationRepository.save(userNotification);
    }

    /**
     * 사용자 알림을 생성합니다.
     * 
     * @param user        사용자
     * @param template    알림 템플릿 (Custom 알림의 경우 null 가능)
     * @param contextData 알림 컨텍스트 데이터
     * @return 생성된 사용자 알림
     * @throws IllegalArgumentException user가 null인 경우
     */
    public UserNotification createUserNotification(
            User user,
            NotificationTemplate template,
            Map<String, Object> contextData) {
        if (user == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }

        return UserNotification.builder()
                .user(user)
                .template(template)
                .contextData(contextData != null ? contextData : Map.of())
                .isRead(false)
                .isDeleted(false)
                .build();
    }


    /**
     * 알림 템플릿 조회
     * 
     * @param eventType 알림 템플릿 타입
     * @return 조회된 알림 템플릿
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "notificationTemplateCache", key = "#eventType", cacheManager = "localCacheManager")
    public NotificationTemplate getNotificationTemplate(NotificationTemplateType eventType) {
        return notificationTemplateRepository
                .findByTemplateType(eventType)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                        "알림 템플릿을 찾을 수 없습니다. eventType: " + eventType));
    }
}
