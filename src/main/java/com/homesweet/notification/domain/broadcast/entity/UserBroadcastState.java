package com.homesweet.notification.domain.broadcast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_broadcast_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBroadcastState {

    @EmbeddedId
    private UserBroadcastStateId id;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    private UserBroadcastState(UserBroadcastStateId id, boolean isRead, boolean isDeleted) {
        this.id = id;
        this.isRead = isRead;
        this.isDeleted = isDeleted;
    }

    public static UserBroadcastState of(Long userId, Long broadcastNotificationId,
            boolean isRead, boolean isDeleted) {
        return new UserBroadcastState(
                new UserBroadcastStateId(userId, broadcastNotificationId), isRead, isDeleted);
    }

    public Long getUserId() {
        return id == null ? null : id.getUserId();
    }

    public Long getBroadcastNotificationId() {
        return id == null ? null : id.getBroadcastNotificationId();
    }
}
