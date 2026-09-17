package com.homesweet.notification.domain.broadcast.repository;

import com.homesweet.notification.domain.broadcast.domain.AudienceType;
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

    @Query("""
            select b from BroadcastNotification b
            left join fetch b.template t
            left join fetch t.category
            where b.createdAt >= :userCreatedAt
              and (b.audienceType = :all
                   or (:userId >= b.targetMinUserId and :userId <= b.targetMaxUserId))
              and not exists (
                    select s from UserBroadcastState s
                    where s.id.userId = :userId
                      and s.id.broadcastNotificationId = b.id
                      and s.isDeleted = true
              )
            order by b.createdAt desc, b.id desc
            """)
    List<BroadcastNotification> findCandidates(
            @Param("userId") Long userId,
            @Param("userCreatedAt") LocalDateTime userCreatedAt,
            @Param("all") AudienceType all,
            Pageable pageable);
}
