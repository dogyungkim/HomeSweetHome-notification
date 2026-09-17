package com.homesweet.notification.domain.broadcast.repository;

import com.homesweet.notification.domain.broadcast.entity.UserBroadcastState;
import com.homesweet.notification.domain.broadcast.entity.UserBroadcastStateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBroadcastStateRepository extends JpaRepository<UserBroadcastState, UserBroadcastStateId> {

    List<UserBroadcastState> findByIdUserIdAndIdBroadcastNotificationIdIn(
            Long userId, List<Long> broadcastNotificationIds);

    @Modifying
    @Query(value = """
            insert into user_broadcast_state
                (user_id, broadcast_notification_id, is_read, is_deleted)
            values (:userId, :broadcastNotificationId, true, false)
            on duplicate key update is_read = true
            """, nativeQuery = true)
    void upsertRead(@Param("userId") Long userId,
            @Param("broadcastNotificationId") Long broadcastNotificationId);

    @Modifying
    @Query(value = """
            insert into user_broadcast_state
                (user_id, broadcast_notification_id, is_read, is_deleted)
            values (:userId, :broadcastNotificationId, true, true)
            on duplicate key update is_read = true, is_deleted = true
            """, nativeQuery = true)
    void upsertDeleted(@Param("userId") Long userId,
            @Param("broadcastNotificationId") Long broadcastNotificationId);
}
