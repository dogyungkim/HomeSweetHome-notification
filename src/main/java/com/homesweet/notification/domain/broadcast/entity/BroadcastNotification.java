package com.homesweet.notification.domain.broadcast.entity;

import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.entity.NotificationTemplate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "broadcast_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BroadcastNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_template_id")
    private NotificationTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 20)
    private AudienceType audienceType;

    @Column(name = "target_min_user_id")
    private Long targetMinUserId;

    @Column(name = "target_max_user_id")
    private Long targetMaxUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> contextData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public BroadcastNotification(
            NotificationTemplate template,
            AudienceType audienceType,
            Long targetMinUserId,
            Long targetMaxUserId,
            Map<String, Object> contextData,
            LocalDateTime createdAt) {
        this.template = template;
        this.audienceType = audienceType;
        this.targetMinUserId = targetMinUserId;
        this.targetMaxUserId = targetMaxUserId;
        this.contextData = contextData == null ? new HashMap<>() : new HashMap<>(contextData);
        this.createdAt = createdAt;
    }

    @PrePersist
    private void setCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isTarget(Long userId, LocalDateTime userCreatedAt) {
        if (userId == null || createdAt == null || userCreatedAt == null
                || userCreatedAt.isAfter(createdAt)) {
            return false;
        }

        return audienceType == AudienceType.ALL
                || (audienceType == AudienceType.USER_ID_RANGE
                && targetMinUserId != null
                && targetMaxUserId != null
                && userId >= targetMinUserId
                && userId <= targetMaxUserId);
    }
}
