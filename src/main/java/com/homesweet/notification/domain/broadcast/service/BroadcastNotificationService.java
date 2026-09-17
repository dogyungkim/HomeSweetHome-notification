package com.homesweet.notification.domain.broadcast.service;

import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import com.homesweet.notification.domain.broadcast.repository.BroadcastNotificationRepository;
import com.homesweet.notification.domain.broadcast.repository.UserBroadcastStateRepository;
import com.homesweet.notification.domain.event.BroadcastNotificationEvent;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BroadcastNotificationService {

    private final BroadcastNotificationRepository broadcastNotificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserBroadcastStateRepository userBroadcastStateRepository;

    @Transactional
    public BroadcastNotification create(BroadcastNotificationEvent event) {
        validate(event);

        NotificationTemplate template = event.templateId() == null
                ? null
                : templateRepository.findById(event.templateId())
                        .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND));

        Map<String, Object> contextData = event.contextData() == null
                ? new HashMap<>()
                : new HashMap<>(event.contextData());
        if (template == null) {
            contextData.put("title", event.title());
            contextData.put("content", event.content());
            contextData.put("redirectUrl", event.redirectUrl());
        }

        BroadcastNotification notification = BroadcastNotification.builder()
                .template(template)
                .audienceType(event.audienceType())
                .targetMinUserId(event.audienceType() == AudienceType.USER_ID_RANGE
                        ? event.targetMinUserId() : null)
                .targetMaxUserId(event.audienceType() == AudienceType.USER_ID_RANGE
                        ? event.targetMaxUserId() : null)
                .contextData(contextData)
                .build();

        return broadcastNotificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(Long userId, LocalDateTime userCreatedAt, Long broadcastNotificationId) {
        BroadcastNotification notification = getTargetNotification(
                userId, userCreatedAt, broadcastNotificationId);
        userBroadcastStateRepository.upsertRead(userId, notification.getId());
    }

    @Transactional
    public void markAsDeleted(Long userId, LocalDateTime userCreatedAt, Long broadcastNotificationId) {
        BroadcastNotification notification = getTargetNotification(
                userId, userCreatedAt, broadcastNotificationId);
        userBroadcastStateRepository.upsertDeleted(userId, notification.getId());
    }

    private BroadcastNotification getTargetNotification(Long userId, LocalDateTime userCreatedAt,
            Long broadcastNotificationId) {
        if (userId == null || userCreatedAt == null) {
            throw new NotificationException(ErrorCode.TOKEN_MISSING);
        }

        BroadcastNotification notification = broadcastNotificationRepository.findById(broadcastNotificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isTarget(userId, userCreatedAt)) {
            throw new NotificationException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }
        return notification;
    }

    private void validate(BroadcastNotificationEvent event) {
        if (event == null || event.audienceType() == null) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }

        if (event.templateId() != null
                && (event.title() != null || event.content() != null || event.redirectUrl() != null)) {
            throw new NotificationException(ErrorCode.INVALID_DATA);
        }

        if (event.templateId() == null
                && isBlank(event.title())
                && isBlank(event.content())
                && isBlank(event.redirectUrl())) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }

        if (event.templateId() == null
                && (isBlank(event.title()) || isBlank(event.content()) || isBlank(event.redirectUrl()))) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }

        if (event.audienceType() == AudienceType.ALL) {
            if (event.targetMinUserId() != null || event.targetMaxUserId() != null) {
                throw new NotificationException(ErrorCode.INVALID_DATA);
            }
            return;
        }

        if (event.targetMinUserId() == null || event.targetMaxUserId() == null
                || event.targetMinUserId() < 1
                || event.targetMaxUserId() < event.targetMinUserId()) {
            throw new NotificationException(ErrorCode.INVALID_DATA);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
