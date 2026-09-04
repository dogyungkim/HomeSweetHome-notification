package com.homesweet.notification.domain.broadcast.service;

import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.domain.broadcast.dto.BroadcastAudienceRequest;
import com.homesweet.notification.domain.broadcast.dto.BroadcastNotificationResponse;
import com.homesweet.notification.domain.broadcast.dto.CreateBroadcastNotificationRequest;
import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import com.homesweet.notification.domain.broadcast.entity.UserBroadcastState;
import com.homesweet.notification.domain.broadcast.repository.BroadcastNotificationRepository;
import com.homesweet.notification.domain.broadcast.repository.UserBroadcastStateRepository;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 단체 알림 비즈니스 서비스 (Fan-out on Read 최소 설계)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastNotificationService {

    private final BroadcastNotificationRepository broadcastNotificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserBroadcastStateRepository userBroadcastStateRepository;

    /**
     * 관리자 API용 단체 알림 생성
     */
    @Transactional
    public BroadcastNotificationResponse createBroadcastNotification(CreateBroadcastNotificationRequest request) {
        if (request.getTemplateId() != null) {
            return sendTemplateBroadcast(request.getAudience(), request.getTemplateId(), request.getContextData());
        } else {
            return sendCustomBroadcast(
                    request.getAudience(),
                    request.getTitle(),
                    request.getContent(),
                    request.getRedirectUrl(),
                    request.getContextData()
            );
        }
    }

    /**
     * 템플릿 기반 단체 알림 생성 (Section 3.3, 6)
     */
    @Transactional
    public BroadcastNotificationResponse sendTemplateBroadcast(
            BroadcastAudienceRequest audience,
            Long templateId,
            Map<String, Object> contextData) {

        validateAudience(audience);

        if (templateId == null) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }

        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND));

        Map<String, Object> finalContextData = contextData != null ? new HashMap<>(contextData) : new HashMap<>();

        BroadcastNotification notification = BroadcastNotification.builder()
                .template(template)
                .audienceType(audience.getType())
                .targetMinUserId(audience.getType() == AudienceType.USER_ID_RANGE ? audience.getMinUserId() : null)
                .targetMaxUserId(audience.getType() == AudienceType.USER_ID_RANGE ? audience.getMaxUserId() : null)
                .contextData(finalContextData)
                .build();

        notification = broadcastNotificationRepository.save(notification);
        log.info("템플릿 단체 알림 생성 완료: id={}, audienceType={}", notification.getId(), notification.getAudienceType());

        return BroadcastNotificationResponse.from(notification);
    }

    /**
     * 커스텀 단체 알림 생성 (Section 3.3, 6)
     */
    @Transactional
    public BroadcastNotificationResponse sendCustomBroadcast(
            BroadcastAudienceRequest audience,
            String title,
            String content,
            String redirectUrl,
            Map<String, Object> contextData) {

        validateAudience(audience);

        if (title == null || title.isBlank() || content == null || content.isBlank() || redirectUrl == null || redirectUrl.isBlank()) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }

        Map<String, Object> finalContextData = contextData != null ? new HashMap<>(contextData) : new HashMap<>();
        finalContextData.put("title", title);
        finalContextData.put("content", content);
        finalContextData.put("redirectUrl", redirectUrl);

        BroadcastNotification notification = BroadcastNotification.builder()
                .template(null) // 커스텀 알림은 템플릿 NULL
                .audienceType(audience.getType())
                .targetMinUserId(audience.getType() == AudienceType.USER_ID_RANGE ? audience.getMinUserId() : null)
                .targetMaxUserId(audience.getType() == AudienceType.USER_ID_RANGE ? audience.getMaxUserId() : null)
                .contextData(finalContextData)
                .build();

        notification = broadcastNotificationRepository.save(notification);
        log.info("커스텀 단체 알림 생성 완료: id={}, audienceType={}", notification.getId(), notification.getAudienceType());

        return BroadcastNotificationResponse.from(notification);
    }

    /**
     * 단체 알림 단건 조회
     */
    @Transactional(readOnly = true)
    public BroadcastNotificationResponse getBroadcastNotification(Long broadcastNotificationId) {
        BroadcastNotification notification = broadcastNotificationRepository.findById(broadcastNotificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.BROADCAST_NOTIFICATION_NOT_FOUND));

        return BroadcastNotificationResponse.from(notification);
    }

    /**
     * 단체 알림 읽음 처리 (Section 8)
     */
    @Transactional
    public void markBroadcastAsRead(Long userId, LocalDateTime userCreatedAt, Long broadcastNotificationId) {
        BroadcastNotification notification = broadcastNotificationRepository.findById(broadcastNotificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.BROADCAST_NOTIFICATION_NOT_FOUND));

        // 대상 판정 검증
        if (!notification.isTarget(userId, userCreatedAt)) {
            throw new NotificationException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        // 멱등한 상태 반영 (JPA 또는 DB Upsert)
        UserBroadcastState state = userBroadcastStateRepository.findByIdUserIdAndIdBroadcastNotificationId(userId, broadcastNotificationId)
                .orElseGet(() -> UserBroadcastState.of(userId, broadcastNotificationId, false, false));

        state.markAsRead();
        userBroadcastStateRepository.save(state);
    }

    /**
     * 단체 알림 삭제 처리 (Section 8)
     */
    @Transactional
    public void markBroadcastAsDeleted(Long userId, LocalDateTime userCreatedAt, Long broadcastNotificationId) {
        BroadcastNotification notification = broadcastNotificationRepository.findById(broadcastNotificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.BROADCAST_NOTIFICATION_NOT_FOUND));

        // 대상 판정 검증
        if (!notification.isTarget(userId, userCreatedAt)) {
            throw new NotificationException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        UserBroadcastState state = userBroadcastStateRepository.findByIdUserIdAndIdBroadcastNotificationId(userId, broadcastNotificationId)
                .orElseGet(() -> UserBroadcastState.of(userId, broadcastNotificationId, false, false));

        state.markAsDeleted();
        userBroadcastStateRepository.save(state);
    }

    /**
     * Audience 요청 검증 (Section 6.1)
     */
    private void validateAudience(BroadcastAudienceRequest audience) {
        if (audience == null || audience.getType() == null) {
            throw new NotificationException(ErrorCode.DATA_MISSING);
        }

        if (audience.getType() == AudienceType.ALL) {
            if (audience.getMinUserId() != null || audience.getMaxUserId() != null) {
                throw new NotificationException(ErrorCode.INVALID_DATA);
            }
        } else if (audience.getType() == AudienceType.USER_ID_RANGE) {
            Long min = audience.getMinUserId();
            Long max = audience.getMaxUserId();
            if (min == null || max == null || min < 1 || min > max) {
                throw new NotificationException(ErrorCode.INVALID_AUDIENCE_RANGE);
            }
        }
    }
}
