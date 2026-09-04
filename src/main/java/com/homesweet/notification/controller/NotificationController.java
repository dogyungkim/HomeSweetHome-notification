package com.homesweet.notification.controller;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homesweet.notification.auth.entity.OAuth2UserPrincipal;
import com.homesweet.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.notification.domain.notification.OrderNotification;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.service.impl.NotificationAPIService;
import com.homesweet.notification.service.impl.NotificationProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.domain.broadcast.service.BroadcastNotificationService;
import com.homesweet.notification.dto.NotificationFeedResponse;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationAPIService notificationAPIService;
    private final BroadcastNotificationService broadcastNotificationService;
    private final KafkaTemplate<String, TemplateNotificationEvent> kafkaTemplate;
    private final NotificationProcessor notificationProcessor;

    /**
     * SSE 알림 테스트
     */
    @GetMapping("/test/{range}")
    public void testMessage(@PathVariable Long range) {
        var notification = OrderNotification.OrderCompleted.builder()
                .userName("test")
                .orderId(12345L)
                .build();
        Long userId = ThreadLocalRandom.current().nextLong(1, range);
        notificationProcessor.processTemplateNotification(new TemplateNotificationEvent(userId, notification));
    }

    @GetMapping("/test/multiple/{range}")
    public void testMultipleMessage(@PathVariable Long range) {
        long effectiveRange = Math.min(range, 1000L);
        var notification = OrderNotification.OrderCompleted.builder()
                .userName("test")
                .orderId(12345L)
                .build();
        List<Long> userIds = IntStream.rangeClosed(1, (int) effectiveRange).mapToObj(i -> (long) i)
                .collect(Collectors.toList());
        notificationProcessor.processTemplateNotification(new TemplateNotificationEvent(userIds, notification));
    }

    /**
     * 통합 알림함 목록 조회 (개인 알림 + 대상 단체 알림 최신 20건 병합, Section 7)
     */
    @GetMapping
    public ResponseEntity<NotificationFeedResponse> getNotifications(
            @AuthenticationPrincipal Object principal) {
        Long userId = extractUserId(principal);
        LocalDateTime userCreatedAt = extractUserCreatedAt(principal);

        log.info("통합 알림 목록 조회: userId={}", userId);
        NotificationFeedResponse response = notificationAPIService.getIntegratedFeed(userId, userCreatedAt);
        return ResponseEntity.ok(response);
    }

    /**
     * 개인 알림 읽음 처리 (기존 경로 유지, Section 8)
     */
    @PatchMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Object principal,
            @RequestBody List<Long> notificationIds) {
        Long userId = extractUserId(principal);
        log.info("개인 알림 읽음 처리: userId={}, notificationIds={}", userId, notificationIds);
        notificationAPIService.markAsRead(userId, notificationIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 개인 알림 삭제 처리 (기존 경로 유지, Section 8)
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(
            @AuthenticationPrincipal Object principal,
            @RequestBody List<Long> notificationIds) {
        Long userId = extractUserId(principal);
        log.info("개인 알림 삭제 처리: userId={}, notificationIds={}", userId, notificationIds);
        notificationAPIService.markAsDeleted(userId, notificationIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 단체 알림 읽음 처리 (Section 8)
     * PATCH /api/v1/notifications/broadcast/{broadcastNotificationId}/read
     */
    @PatchMapping("/broadcast/{broadcastNotificationId}/read")
    public ResponseEntity<Void> markBroadcastAsRead(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long broadcastNotificationId) {
        Long userId = extractUserId(principal);
        LocalDateTime userCreatedAt = extractUserCreatedAt(principal);

        log.info("단체 알림 읽음 처리: userId={}, broadcastNotificationId={}", userId, broadcastNotificationId);
        broadcastNotificationService.markBroadcastAsRead(userId, userCreatedAt, broadcastNotificationId);
        return ResponseEntity.ok().build();
    }

    /**
     * 단체 알림 삭제 처리 (Section 8)
     * DELETE /api/v1/notifications/broadcast/{broadcastNotificationId}
     */
    @DeleteMapping("/broadcast/{broadcastNotificationId}")
    public ResponseEntity<Void> deleteBroadcastNotification(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long broadcastNotificationId) {
        Long userId = extractUserId(principal);
        LocalDateTime userCreatedAt = extractUserCreatedAt(principal);

        log.info("단체 알림 삭제 처리: userId={}, broadcastNotificationId={}", userId, broadcastNotificationId);
        broadcastNotificationService.markBroadcastAsDeleted(userId, userCreatedAt, broadcastNotificationId);
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(Object principal) {
        if (principal instanceof OAuth2UserPrincipal p) {
            return p.getUserId();
        } else if (principal instanceof User u) {
            return u.getId();
        }
        return 0L;
    }

    private LocalDateTime extractUserCreatedAt(Object principal) {
        if (principal instanceof OAuth2UserPrincipal p) {
            return p.getCreatedAt();
        } else if (principal instanceof User u) {
            return u.getCreatedAt();
        }
        return null;
    }
}
