package com.homesweet.notification.domain.broadcast.repository;

import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BroadcastNotificationRepository extends JpaRepository<BroadcastNotification, Long> {

       /**
        * 통합 알림함 조회를 위한 대상 단체 알림 상위 후보 조회
        * userCreatedAt <= b.createdAt (미래 가입자 제외)
        * audienceType = ALL 또는 userId 범위 포함
        * 삭제되지 않은 것만 (s.id.userId IS NULL OR s.isDeleted = false)
        * template, category fetch join으로 N+1 방지
        */
       @Query("SELECT b FROM BroadcastNotification b " +
                     "LEFT JOIN FETCH b.template t " +
                     "LEFT JOIN FETCH t.category " +
                     "LEFT JOIN UserBroadcastState s " +
                     "    ON s.id.broadcastNotificationId = b.id AND s.id.userId = :userId " +
                     "WHERE (:userCreatedAt IS NULL OR :userCreatedAt <= b.createdAt) " +
                     "  AND (b.audienceType = com.homesweet.notification.domain.broadcast.domain.AudienceType.ALL " +
                     "       OR (:userId BETWEEN b.targetMinUserId AND b.targetMaxUserId)) " +
                     "  AND (s.id.userId IS NULL OR s.isDeleted = false) " +
                     "ORDER BY b.createdAt DESC, b.id DESC")
       List<BroadcastNotification> findCandidateBroadcastNotifications(
                     @Param("userId") Long userId,
                     @Param("userCreatedAt") LocalDateTime userCreatedAt,
                     Pageable pageable);
}
