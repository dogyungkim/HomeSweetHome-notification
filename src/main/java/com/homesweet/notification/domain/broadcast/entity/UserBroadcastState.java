package com.homesweet.notification.domain.broadcast.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 사용자별 단체 알림 상태 (읽음 / 삭제 여부)
 */
@Entity
@Table(name = "user_broadcast_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
public class UserBroadcastState {

    @EmbeddedId
    private UserBroadcastStateId id;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    public UserBroadcastState(UserBroadcastStateId id, boolean isRead, boolean isDeleted) {
        this.id = id;
        this.isRead = isRead;
        this.isDeleted = isDeleted;
    }

    public static UserBroadcastState of(Long userId, Long broadcastNotificationId, boolean isRead, boolean isDeleted) {
        return UserBroadcastState.builder()
                .id(new UserBroadcastStateId(userId, broadcastNotificationId))
                .isRead(isRead)
                .isDeleted(isDeleted)
                .build();
    }

    public Long getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public Long getBroadcastNotificationId() {
        return id != null ? id.getBroadcastNotificationId() : null;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsDeleted() {
        this.isRead = true;
        this.isDeleted = true;
    }
}
