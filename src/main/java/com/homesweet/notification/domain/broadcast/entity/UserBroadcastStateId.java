package com.homesweet.notification.domain.broadcast.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

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
