package com.homesweet.notification.domain.broadcast.entity;

import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.entity.NotificationTemplate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 단체 알림 엔티티 (Fan-out on Read 최소 설계)
 */
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

    @CreationTimestamp
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "created_at", insertable = false, updatable = false)
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
        this.contextData = contextData != null ? contextData : Map.of();
        this.createdAt = createdAt;
    }

    /**
     * 사용자 대상 포함 여부 판정 (현재 간편 ver)
     * user.created_at <= broadcast_notification.created_at
     */
    public boolean isTarget(Long userId, LocalDateTime userCreatedAt) {
        if (userId == null) {
            return false;
        }

        // 미래 가입자 제외
        if (userCreatedAt != null && this.createdAt != null && userCreatedAt.isAfter(this.createdAt)) {
            return false;
        }

        if (this.audienceType == AudienceType.ALL) {
            return true;
        }

        if (this.audienceType == AudienceType.USER_ID_RANGE) {
            return this.targetMinUserId != null
                    && this.targetMaxUserId != null
                    && this.targetMinUserId <= userId
                    && userId <= this.targetMaxUserId;
        }

        return false;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
