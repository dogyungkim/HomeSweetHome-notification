package com.homesweet.notification.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.repository.UserNotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.broadcast.domain.NotificationType;
import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import com.homesweet.notification.domain.broadcast.entity.UserBroadcastState;
import com.homesweet.notification.domain.broadcast.repository.BroadcastNotificationRepository;
import com.homesweet.notification.domain.broadcast.repository.UserBroadcastStateRepository;
import com.homesweet.notification.dto.NotificationFeedResponse;
import com.homesweet.notification.dto.NotificationItemDTO;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAPIService {
    private final UserNotificationRepository userNotificationRepository;
    private final BroadcastNotificationRepository broadcastNotificationRepository;
    private final UserBroadcastStateRepository userBroadcastStateRepository;

    /**
     * 개인 알림과 단체 알림 통합 알림함 조회 (최신 20건, Section 7)
     */
    @Transactional(readOnly = true)
    public NotificationFeedResponse getIntegratedFeed(Long userId, LocalDateTime userCreatedAt) {
        List<NotificationItemDTO> mergedItems = new ArrayList<>();

        // 1. 개인 알림 최신 20건 조회
        List<UserNotification> personalNotifications = userNotificationRepository
                .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        for (UserNotification un : personalNotifications) {
            var template = un.getTemplate();
            String title = template != null ? template.getTitle() : (String) un.getContextData().getOrDefault("title", "");
            String content = template != null ? template.getContent() : (String) un.getContextData().getOrDefault("content", "");
            String redirectUrl = template != null ? template.getRedirectUrl() : (String) un.getContextData().getOrDefault("redirectUrl", "");
            NotificationCategoryType categoryType = (template != null && template.getCategory() != null)
                    ? template.getCategory().getCategoryType()
                    : NotificationCategoryType.CUSTOM;

            mergedItems.add(NotificationItemDTO.builder()
                    .notificationType(NotificationType.PERSONAL)
                    .notificationId(un.getId())
                    .title(title)
                    .content(content)
                    .redirectUrl(redirectUrl)
                    .contextData(un.getContextData())
                    .isRead(un.getIsRead())
                    .categoryType(categoryType)
                    .createdAt(un.getCreatedAt())
                    .build());
        }

        // 2. 단체 알림 최신 20건 조회 (DB 쿼리로 대상 및 미삭제 조건 평가)
        List<BroadcastNotification> broadcastNotifications = broadcastNotificationRepository
                .findCandidateBroadcastNotifications(userId, userCreatedAt, PageRequest.of(0, 20));

        if (!broadcastNotifications.isEmpty()) {
            List<Long> broadcastIds = broadcastNotifications.stream()
                    .map(BroadcastNotification::getId)
                    .toList();

            Map<Long, UserBroadcastState> stateMap = userBroadcastStateRepository
                    .findByIdUserIdAndIdBroadcastNotificationIdIn(userId, broadcastIds)
                    .stream()
                    .collect(Collectors.toMap(UserBroadcastState::getBroadcastNotificationId, s -> s));

            for (BroadcastNotification bn : broadcastNotifications) {
                var template = bn.getTemplate();
                String title = template != null ? template.getTitle() : (String) bn.getContextData().getOrDefault("title", "");
                String content = template != null ? template.getContent() : (String) bn.getContextData().getOrDefault("content", "");
                String redirectUrl = template != null ? template.getRedirectUrl() : (String) bn.getContextData().getOrDefault("redirectUrl", "");
                NotificationCategoryType categoryType = (template != null && template.getCategory() != null)
                        ? template.getCategory().getCategoryType()
                        : NotificationCategoryType.CUSTOM;

                UserBroadcastState state = stateMap.get(bn.getId());
                boolean isRead = state != null && state.isRead();

                mergedItems.add(NotificationItemDTO.builder()
                        .notificationType(NotificationType.BROADCAST)
                        .notificationId(bn.getId())
                        .title(title)
                        .content(content)
                        .redirectUrl(redirectUrl)
                        .contextData(bn.getContextData())
                        .isRead(isRead)
                        .categoryType(categoryType)
                        .createdAt(bn.getCreatedAt())
                        .build());
            }
        }

        // 3. 공통 정렬: createdAt DESC, notificationTypeRank DESC (PERSONAL=1, BROADCAST=0), notificationId DESC
        Comparator<NotificationItemDTO> comparator = Comparator
                .comparing(NotificationItemDTO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> item.getNotificationType().getRank(), Comparator.reverseOrder())
                .thenComparing(NotificationItemDTO::getNotificationId, Comparator.nullsLast(Comparator.reverseOrder()));

        mergedItems.sort(comparator);

        // 4. 상위 20건 슬라이스
        List<NotificationItemDTO> top20 = mergedItems.stream().limit(20).toList();

        return NotificationFeedResponse.builder()
                .items(top20)
                .build();
    }

    /**
     * 사용자의 알림 목록 조회 (최대 20개)
     * 
     * @param userId 사용자 ID
     * @return 알림 목록 (최대 20개)
     */
    @Transactional(readOnly = true)
    public List<PushNotificationDTO> getAllNotifications(Long userId) {
        List<UserNotification> userNotifications = userNotificationRepository
                .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        // List<UserNotification>을 List<PushNotificationDTO>로 변환하고 최대 20개로 제한
        return userNotifications.stream()
                .limit(20)
                .map(userNotification -> {
                    var template = userNotification.getTemplate();
                    return PushNotificationDTO.builder()
                            .notificationId(userNotification.getId())
                            .title(template.getTitle())
                            .content(template.getContent())
                            .redirectUrl(template.getRedirectUrl())
                            .contextData(userNotification.getContextData())
                            .isRead(userNotification.getIsRead())
                            .categoryType(template.getCategory().getCategoryType())
                            .createdAt(userNotification.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 알림 읽음 처리 (단일 및 여러 개 모두 처리)
     * 
     * @param userId          사용자 ID
     * @param notificationIds 알림 ID 리스트
     * @throws IllegalArgumentException 알림 ID 리스트가 null이거나 비어있는 경우
     * @throws NotificationException    알림을 찾을 수 없는 경우
     */
    @Transactional
    public void markAsRead(Long userId, List<Long> notificationIds) {
        List<UserNotification> userNotifications = userNotificationRepository
                .findByIdInAndUserIdAndNotDeleted(notificationIds, userId);

        if (userNotifications.isEmpty()) {
            throw new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        userNotifications.forEach(UserNotification::markAsRead);
        userNotificationRepository.saveAll(userNotifications);
    }

    /**
     * 알림 삭제 처리 (단일 및 여러 개 모두 처리)
     * 
     * @param userId          사용자 ID
     * @param notificationIds 알림 ID 리스트
     * @throws IllegalArgumentException 알림 ID 리스트가 null이거나 비어있는 경우
     * @throws NotificationException    알림을 찾을 수 없는 경우
     */
    @Transactional
    public void markAsDeleted(Long userId, List<Long> notificationIds) {
        List<UserNotification> userNotifications = userNotificationRepository
                .findByIdInAndUserIdAndNotDeleted(notificationIds, userId);

        if (userNotifications.isEmpty()) {
            throw new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        userNotifications.forEach(UserNotification::markAsDeleted);
        userNotifications.forEach(UserNotification::markAsRead);

        userNotificationRepository.saveAll(userNotifications);
    }
}
