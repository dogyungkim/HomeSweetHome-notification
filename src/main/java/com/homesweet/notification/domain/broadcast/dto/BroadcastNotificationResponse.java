package com.homesweet.notification.domain.broadcast.dto;

import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastNotificationResponse {

    private Long broadcastNotificationId;
    private Long templateId;
    private AudienceType audienceType;
    private Long targetMinUserId;
    private Long targetMaxUserId;
    private Map<String, Object> contextData;
    private LocalDateTime createdAt;

    public static BroadcastNotificationResponse from(BroadcastNotification entity) {
        if (entity == null) {
            return null;
        }
        return BroadcastNotificationResponse.builder()
                .broadcastNotificationId(entity.getId())
                .templateId(entity.getTemplate() != null ? entity.getTemplate().getId() : null)
                .audienceType(entity.getAudienceType())
                .targetMinUserId(entity.getTargetMinUserId())
                .targetMaxUserId(entity.getTargetMaxUserId())
                .contextData(entity.getContextData())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
