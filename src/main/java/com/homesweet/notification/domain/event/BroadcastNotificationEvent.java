package com.homesweet.notification.domain.event;

import com.homesweet.notification.domain.broadcast.domain.AudienceType;

import java.util.Map;

/**
 * Kafka command for creating a fan-out-on-read broadcast notification.
 */
public record BroadcastNotificationEvent(
        AudienceType audienceType,
        Long targetMinUserId,
        Long targetMaxUserId,
        Long templateId,
        String title,
        String content,
        String redirectUrl,
        Map<String, Object> contextData) {
}
