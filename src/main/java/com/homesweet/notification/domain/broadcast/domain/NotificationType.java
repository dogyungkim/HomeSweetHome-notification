package com.homesweet.notification.domain.broadcast.domain;

import lombok.Getter;

/**
 * 알림 출처 및 유형
 * PERSONAL = 1, BROADCAST = 0 (동일 시각 정렬 시 PERSONAL 우선)
 */
@Getter
public enum NotificationType {
    PERSONAL(1),
    BROADCAST(0);

    private final int rank;

    NotificationType(int rank) {
        this.rank = rank;
    }
}
