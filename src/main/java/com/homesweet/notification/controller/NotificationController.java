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
import com.homesweet.notification.domain.broadcast.service.BroadcastNotificationService;
import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.service.impl.NotificationAPIService;
import com.homesweet.notification.service.impl.NotificationProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationAPIService notificationAPIService;
    private final KafkaTemplate<String, TemplateNotificationEvent> kafkaTemplate;
    private final NotificationProcessor notificationProcessor;
    private final BroadcastNotificationService broadcastNotificationService;

    /**
     * SSE 알림 테스트
     * 
     **/
    @GetMapping("/test/{range}")
    public void testMessage(@PathVariable Long range) {
        var notification = OrderNotification.OrderCompleted.builder()
                .userName("test")
                .orderId(12345L)
                .build();
        Long userId = ThreadLocalRandom.current().nextLong(1, range);
        // kafkaTemplate.send("notification", new TemplateNotificationEvent(userId, notification));
        notificationProcessor.processTemplateNotification(new TemplateNotificationEvent(userId, notification));
    }

    @GetMapping("/test/multiple/{range}")
    public void testMultipleMessage(@PathVariable Long range) {
        var notification = OrderNotification.OrderCompleted.builder()
                .userName("test")
                .orderId(12345L)
                .build();
        List<Long> userIds = IntStream.rangeClosed(1, range.intValue()).mapToObj(i -> (long) i)
                .collect(Collectors.toList());
        //kafkaTemplate.send("notification", new TemplateNotificationEvent(userIds, notification));
        notificationProcessor.processTemplateNotification(new TemplateNotificationEvent(userIds, notification));
    }

    /**
     * 사용자의 알림 목록 조회 (최대 20개)
     */
    @GetMapping
    public ResponseEntity<List<PushNotificationDTO>> getNotifications(
            @AuthenticationPrincipal Object principal) {
        OAuth2UserPrincipal authenticatedPrincipal = requirePrincipal(principal);
        log.info("알림 목록 조회: userId={}", authenticatedPrincipal.getUserId());
        List<PushNotificationDTO> notifications = notificationAPIService.getAllNotifications(
                authenticatedPrincipal.getUserId(), authenticatedPrincipal.getCreatedAt());
        return ResponseEntity.ok(notifications);
    }

    /**
     * 알림 읽음 처리 (단일 및 여러 개 모두 처리)
     * 
     * RequestBody 예시:
     * - 단일: [1]
     * - 여러 개: [1, 2, 3]
     */
    @PatchMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Object principal,
            @RequestBody List<Long> notificationIds) {
        OAuth2UserPrincipal authenticatedPrincipal = requirePrincipal(principal);
        log.info("알림 읽음 처리: userId={}, notificationIds={}", authenticatedPrincipal.getUserId(), notificationIds);
        notificationAPIService.markAsRead(authenticatedPrincipal.getUserId(), notificationIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 알림 삭제 처리 (단일 및 여러 개 모두 처리)
     * 
     * RequestBody 예시:
     * - 단일: [1]
     * - 여러 개: [1, 2, 3]
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(
            @AuthenticationPrincipal Object principal,
            @RequestBody List<Long> notificationIds) {
        OAuth2UserPrincipal authenticatedPrincipal = requirePrincipal(principal);
        log.info("알림 삭제 처리: userId={}, notificationIds={}", authenticatedPrincipal.getUserId(), notificationIds);
        notificationAPIService.markAsDeleted(authenticatedPrincipal.getUserId(), notificationIds);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/broadcast/{broadcastNotificationId}/read")
    public ResponseEntity<Void> markBroadcastAsRead(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long broadcastNotificationId) {
        OAuth2UserPrincipal authenticatedPrincipal = requirePrincipal(principal);
        broadcastNotificationService.markAsRead(
                authenticatedPrincipal.getUserId(), authenticatedPrincipal.getCreatedAt(), broadcastNotificationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/broadcast/{broadcastNotificationId}")
    public ResponseEntity<Void> deleteBroadcastNotification(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long broadcastNotificationId) {
        OAuth2UserPrincipal authenticatedPrincipal = requirePrincipal(principal);
        broadcastNotificationService.markAsDeleted(
                authenticatedPrincipal.getUserId(), authenticatedPrincipal.getCreatedAt(), broadcastNotificationId);
        return ResponseEntity.ok().build();
    }

    private OAuth2UserPrincipal requirePrincipal(Object principal) {
        if (principal instanceof OAuth2UserPrincipal authenticatedPrincipal
                && authenticatedPrincipal.getUserId() != null
                && authenticatedPrincipal.getCreatedAt() != null) {
            return authenticatedPrincipal;
        }
        throw new NotificationException(ErrorCode.TOKEN_MISSING);
    }
}
