package com.homesweet.notification.domain.event;

import java.util.List;

import com.homesweet.notification.domain.notification.CustomNotification;
import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.exception.NotificationException;

/**
 * 커스텀 알림 이벤트
 * 
 * @author dogyungkim
 */
public record CustomNotificationEvent(
        List<Long> userIds,
        CustomNotification notification) {
    public CustomNotificationEvent {
        if (userIds == null || userIds.isEmpty()) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }
        if (notification == null) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }
    }

    /**
     * 단일 사용자용 생성자
     */
    public CustomNotificationEvent(
            Long userId,
            CustomNotification notification) {
        this(List.of(userId), notification);
    }
}
