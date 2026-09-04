package com.homesweet.notification.domain.broadcast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/**
 * user_broadcast_state 복합키 (user_id, broadcast_notification_id)
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class UserBroadcastStateId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "broadcast_notification_id", nullable = false)
    private Long broadcastNotificationId;
}
