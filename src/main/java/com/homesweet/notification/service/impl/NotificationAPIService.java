package com.homesweet.notification.service.impl;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.domain.broadcast.domain.NotificationType;
import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import com.homesweet.notification.domain.broadcast.entity.UserBroadcastState;
import com.homesweet.notification.domain.broadcast.repository.BroadcastNotificationRepository;
import com.homesweet.notification.domain.broadcast.repository.UserBroadcastStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.repository.UserNotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationAPIService {
    private final UserNotificationRepository userNotificationRepository;
    private final BroadcastNotificationRepository broadcastNotificationRepository;
    private final UserBroadcastStateRepository userBroadcastStateRepository;

    /**
     * 개인 알림과 단체 알림을 하나의 기존 배열 응답으로 병합한다.
     */
    @Transactional(readOnly = true)
    public List<PushNotificationDTO> getAllNotifications(Long userId, java.time.LocalDateTime userCreatedAt) {
        List<PushNotificationDTO> result = new ArrayList<>();

        userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(notification -> toPushNotification(notification, NotificationType.PERSONAL))
                .forEach(result::add);

        if (userCreatedAt == null) {
            return result;
        }

        List<BroadcastNotification> broadcasts = broadcastNotificationRepository.findCandidates(
                userId, userCreatedAt, AudienceType.ALL, PageRequest.of(0, 20));
        Map<Long, UserBroadcastState> states = broadcasts.isEmpty()
                ? Map.of()
                : userBroadcastStateRepository
                        .findByIdUserIdAndIdBroadcastNotificationIdIn(
                                userId, broadcasts.stream().map(BroadcastNotification::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(UserBroadcastState::getBroadcastNotificationId, state -> state));

        broadcasts.stream()
                .map(notification -> toPushNotification(notification, states.get(notification.getId())))
                .forEach(result::add);

        result.sort(Comparator
                .comparing(PushNotificationDTO::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(dto -> dto.getNotificationType() == NotificationType.PERSONAL ? 1 : 0,
                        Comparator.reverseOrder())
                .thenComparing(PushNotificationDTO::getNotificationId,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return result.stream().limit(20).toList();
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
                .map(userNotification -> toPushNotification(userNotification, NotificationType.PERSONAL))
                .collect(Collectors.toList());
    }

    private PushNotificationDTO toPushNotification(UserNotification notification, NotificationType type) {
        var template = notification.getTemplate();
        String title = template != null
                ? template.getTitle()
                : String.valueOf(notification.getContextData().getOrDefault("title", ""));
        String content = template != null
                ? template.getContent()
                : String.valueOf(notification.getContextData().getOrDefault("content", ""));
        String redirectUrl = template != null
                ? template.getRedirectUrl()
                : String.valueOf(notification.getContextData().getOrDefault("redirectUrl", ""));
        NotificationCategoryType categoryType = template != null && template.getCategory() != null
                ? template.getCategory().getCategoryType()
                : NotificationCategoryType.CUSTOM;

        return PushNotificationDTO.builder()
                .notificationType(type)
                .notificationId(notification.getId())
                .title(title)
                .content(content)
                .redirectUrl(redirectUrl)
                .contextData(notification.getContextData())
                .isRead(notification.getIsRead())
                .categoryType(categoryType)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private PushNotificationDTO toPushNotification(BroadcastNotification notification,
            UserBroadcastState state) {
        var template = notification.getTemplate();
        String title = template != null
                ? template.getTitle()
                : String.valueOf(notification.getContextData().getOrDefault("title", ""));
        String content = template != null
                ? template.getContent()
                : String.valueOf(notification.getContextData().getOrDefault("content", ""));
        String redirectUrl = template != null
                ? template.getRedirectUrl()
                : String.valueOf(notification.getContextData().getOrDefault("redirectUrl", ""));
        NotificationCategoryType categoryType = template != null && template.getCategory() != null
                ? template.getCategory().getCategoryType()
                : NotificationCategoryType.CUSTOM;

        return PushNotificationDTO.builder()
                .notificationType(NotificationType.BROADCAST)
                .notificationId(notification.getId())
                .title(title)
                .content(content)
                .redirectUrl(redirectUrl)
                .contextData(notification.getContextData())
                .isRead(state != null && state.isRead())
                .categoryType(categoryType)
                .createdAt(notification.getCreatedAt())
                .build();
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
