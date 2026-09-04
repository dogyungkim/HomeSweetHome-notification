package com.homesweet.notification.domain.broadcast.repository;

import com.homesweet.notification.domain.broadcast.entity.UserBroadcastState;
import com.homesweet.notification.domain.broadcast.entity.UserBroadcastStateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBroadcastStateRepository extends JpaRepository<UserBroadcastState, UserBroadcastStateId> {

    /**
     * 특정 사용자의 복수 단체 알림 상태 목록 조회
     */
    List<UserBroadcastState> findByIdUserIdAndIdBroadcastNotificationIdIn(Long userId,
            List<Long> broadcastNotificationIds);

    /**
     * 특정 사용자의 단일 단체 알림 상태 조회
     */
    Optional<UserBroadcastState> findByIdUserIdAndIdBroadcastNotificationId(Long userId, Long broadcastNotificationId);

    /**
     * 단체 알림 읽음 처리 Upsert
     */
    @Modifying
    @Query(value = "INSERT INTO user_broadcast_state (user_id, broadcast_notification_id, is_read, is_deleted) " +
            "VALUES (:userId, :broadcastNotificationId, TRUE, FALSE) " +
            "ON DUPLICATE KEY UPDATE is_read = TRUE", nativeQuery = true)
    void upsertRead(@Param("userId") Long userId, @Param("broadcastNotificationId") Long broadcastNotificationId);

    /**
     * 단체 알림 삭제 처리 Upsert
     */
    @Modifying
    @Query(value = "INSERT INTO user_broadcast_state (user_id, broadcast_notification_id, is_read, is_deleted) " +
            "VALUES (:userId, :broadcastNotificationId, TRUE, TRUE) " +
            "ON DUPLICATE KEY UPDATE is_read = TRUE, is_deleted = TRUE", nativeQuery = true)
    void upsertDeleted(@Param("userId") Long userId, @Param("broadcastNotificationId") Long broadcastNotificationId);
}
