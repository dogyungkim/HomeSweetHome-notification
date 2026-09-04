package com.homesweet.notification.dto;

import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.broadcast.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 개인 및 단체 통합 알림 아이템 DTO (Section 7, 7.1)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationItemDTO {

    private NotificationType notificationType;
    private Long notificationId;
    private String title;
    private String content;
    private String redirectUrl;
    private Map<String, Object> contextData;
    private boolean isRead;
    private NotificationCategoryType categoryType;
    private LocalDateTime createdAt;
}
