package com.homesweet.notification.domain.broadcast.controller;

import com.homesweet.notification.domain.broadcast.dto.BroadcastNotificationResponse;
import com.homesweet.notification.domain.broadcast.dto.CreateBroadcastNotificationRequest;
import com.homesweet.notification.domain.broadcast.service.BroadcastNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 단체 알림 컨트롤러 (최소 설계)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/broadcast-notifications")
@RequiredArgsConstructor
public class AdminBroadcastNotificationController {

    private final BroadcastNotificationService broadcastNotificationService;

    /**
     * 단체 알림 생성 및 즉시 발행
     */
    @PostMapping
    public ResponseEntity<BroadcastNotificationResponse> createBroadcastNotification(
            @Valid @RequestBody CreateBroadcastNotificationRequest request) {

        log.info("관리자 단체 알림 생성 요청 수신: audienceType={}", request.getAudience().getType());
        BroadcastNotificationResponse response = broadcastNotificationService.createBroadcastNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 단체 알림 단건 조회
     */
    @GetMapping("/{broadcastNotificationId}")
    public ResponseEntity<BroadcastNotificationResponse> getBroadcastNotification(
            @PathVariable Long broadcastNotificationId) {

        BroadcastNotificationResponse response = broadcastNotificationService.getBroadcastNotification(broadcastNotificationId);
        return ResponseEntity.ok(response);
    }
}
