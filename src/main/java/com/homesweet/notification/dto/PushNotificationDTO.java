package com.homesweet.notification.dto;

import java.util.Map;
import java.time.LocalDateTime;

import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.broadcast.domain.NotificationType;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PushNotificationDTO {
    Long notificationId;
    String title;
    String content;
    String redirectUrl;
    Map<String, Object> contextData;
    boolean isRead;
    NotificationCategoryType categoryType;
    LocalDateTime createdAt;
    NotificationType notificationType;

}
