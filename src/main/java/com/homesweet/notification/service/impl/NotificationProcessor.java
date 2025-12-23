package com.homesweet.notification.service.impl;

import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.service.UserService;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.event.CustomNotificationEvent;
import com.homesweet.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.notification.domain.notification.CustomNotification;
import com.homesweet.notification.domain.notification.TemplateNotification;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.service.NotificationPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 알림 이벤트 리스너
 * 
 * 이벤트 기반으로 알림을 비동기 처리합니다.
 * 
 * @author dogyungkim
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProcessor {

  private final UserNotificationService userNotificationService;
  private final NotificationPublisher notificationPublisher;
  private final UserService userService;

  private static final int BATCH_SIZE = 500;

  /**
   * 템플릿 알림 이벤트 처리
   * 
   * 단일 사용자 또는 다수 사용자 모두 처리합니다.
   * TemplateNotification을 통해 DB에서 템플릿을 조회하고, Payload와 함께 알림을 전송합니다.
   * 
   * Kafka에서 호출될 때는 동기적으로 처리되어 commit 보장을 위해 @Async가 없습니다.
   * 다른 곳에서 이벤트로 호출될 때는 비동기 처리를 위해 @EventListener와 함께 사용됩니다.
   */
  @EventListener
  @Async("notificationTaskExecutor")
  public void handleTemplateNotificationEvent(TemplateNotificationEvent event) {
    processTemplateNotification(event);
  }

  /**
   * 템플릿 알림 처리 로직 (동기 처리용)
   * 
   * Kafka Listener에서 직접 호출하여 동기적으로 처리합니다.
   */
  public void processTemplateNotification(TemplateNotificationEvent event) {
    log.info("템플릿 알림 이벤트 처리 시작: userIds={}, eventType={}", event.userIds(),
        event.notification().getEventType());

    // 1. 이벤트에서 알림 템플릿 타입 추출
    TemplateNotification notification = event.notification();

    // 2. 알림 템플릿 조회
    NotificationTemplate template = userNotificationService.getNotificationTemplate(notification.getEventType());

    log.info("템플릿 조회 완료: template={}", template);

    List<Long> userIds = event.userIds();

    // 단일 사용자 처리 최적화
    if (userIds.size() == 1) {
      processSingleNotification(userIds, notification, template);
      return;
    }

    // 다수 사용자 배치 처리
    processBatchNotifications(userIds, notification, template);
  }

  /**
   * 커스텀 알림 이벤트 처리
   * 
   * Kafka에서 호출될 때는 동기적으로 처리되어 commit 보장을 위해 @Async가 없습니다.
   * 다른 곳에서 이벤트로 호출될 때는 비동기 처리를 위해 @EventListener와 함께 사용됩니다.
   */
  @EventListener
  @Async("notificationTaskExecutor")
  public void handleCustomNotificationEvent(CustomNotificationEvent event) {
    processCustomNotification(event);
  }

  /**
   * 커스텀 알림 처리 로직 (동기 처리용)
   * 
   * Kafka Listener에서 직접 호출하여 동기적으로 처리합니다.
   */
  public void processCustomNotification(CustomNotificationEvent event) {
    log.info("커스텀 알림 이벤트 처리 시작: userIds={}, categoryType={}, title={}", event.userIds(),
        event.notification().getTitle(), event.notification().getContent());

    // 1. 이벤트에서 알림 정보 추출
    CustomNotification notification = event.notification();

    // TODO: 커스텀 알림은 따로 템플릿 저장하지 않기
    // 2. 커스텀 알림 템플릿 생성
    NotificationTemplate template = userNotificationService.createAndSaveCustomNotificationTemplate(
        notification.getTitle(),
        notification.getContent(),
        notification.getRedirectUrl());

    List<Long> userIds = event.userIds();

    // 단일 사용자 처리 최적화
    if (userIds.size() == 1) {
      processSingleNotification(userIds, notification, template);
      return;
    }

    // 다수 사용자 배치 처리
    processBatchNotifications(userIds, notification, template);
  }

  // 내부 메서드

  private void processSingleNotification(List<Long> userIds, TemplateNotification notification,
      NotificationTemplate template) {
    // 3. 템플릿을 사용하여 사용자화 된 알림 생성
    UserNotification userNotification = createSingleUserNotification(userIds.get(0), notification, template);

    // 4. 단건 저장
    if (userNotification != null) {
      userNotificationService.saveUserNotification(userNotification);

      // 5. DTO 변환
      // convertToPushNotificationDTO expects a list, so we wrap the single item
      List<UserNotification> userNotifications = List.of(userNotification);
      Map<Long, PushNotificationDTO> pushNotificationDTOMap = convertToPushNotificationDTO(template, userNotifications);
      // 6. 푸시 알림 전송
      notificationPublisher.publishBulk(pushNotificationDTOMap);
    }
  }

  private void processBatchNotifications(List<Long> userIds, TemplateNotification notification,
      NotificationTemplate template) {
    int totalUsers = userIds.size();
    for (int i = 0; i < totalUsers; i += BATCH_SIZE) {
      int end = Math.min(i + BATCH_SIZE, totalUsers);
      List<Long> batchUserIds = userIds.subList(i, end);

      try {
        // 3. 배치 단위 알림 생성
        List<UserNotification> batchUserNotifications = createBatchUserNotifications(batchUserIds, notification,
            template);

        if (batchUserNotifications.isEmpty()) {
          continue;
        }

        // 4. 배치 저장
        userNotificationService.bulkInsertUserNotifications(batchUserNotifications);

        // 5. 배치 DTO 변환 및 전송
        Map<Long, PushNotificationDTO> pushNotificationDTOMap = convertToPushNotificationDTO(template,
            batchUserNotifications);
        notificationPublisher.publishBulk(pushNotificationDTOMap);

      } catch (Exception e) {
        log.error("배치 처리 중 오류 발생: range={}-{}, error={}", i, end, e.getMessage(), e);
        // 배치 처리 중 오류가 발생하더라도 다음 배치는 계속 처리해야 함
      }
    }
  }

  /**
   * 알림 정보를 활용해 사용자 알림 생성
   */
  /**
   * 알림 정보를 활용해 단일 사용자 알림 생성
   */
  private UserNotification createSingleUserNotification(
      Long userId,
      TemplateNotification notification,
      NotificationTemplate template) {
    try {
      User user = userService.getUserById(userId);
      Map<String, Object> notificationContextData = notification.toMap();
      return userNotificationService.createUserNotification(
          user,
          template,
          notificationContextData);
    } catch (Exception e) {
      log.error("사용자 알림 객체 생성 실패: userId={}, error={}", userId, e.getMessage(), e);
      return null;
    }
  }

  /**
   * 알림 정보를 활용해 다수 사용자 알림 생성 (배치)
   */
  private List<UserNotification> createBatchUserNotifications(
      List<Long> userIds,
      TemplateNotification notification,
      NotificationTemplate template) {
    // 2. 알림 객체 생성 (메모리)
    List<UserNotification> userNotifications = new ArrayList<>();
    Map<String, Object> notificationContextData = notification.toMap();
    List<User> users = userService.getManyUsersById(userIds);

    for (User user : users) {
      try {
        // 3. 사용자 알림 생성
        UserNotification userNotification = userNotificationService.createUserNotification(
            user,
            template,
            notificationContextData);
        userNotifications.add(userNotification);
      } catch (Exception e) {
        log.error("사용자 알림 객체 생성 실패: userId={}, error={}", user.getId(), e.getMessage(), e);
      }
    }
    return userNotifications;
  }

  /**
   * 알림 객체를 푸시 알림 DTO로 변환
   */
  private Map<Long, PushNotificationDTO> convertToPushNotificationDTO(
      NotificationTemplate template,
      List<UserNotification> userNotifications) {
    Map<Long, PushNotificationDTO> pushNotificationDTOMap = new HashMap<>(userNotifications.size());

    for (UserNotification userNotification : userNotifications) {
      try {
        PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(
            userNotification.getContextData(),
            template,
            userNotification.getId() // JDBC Insert로 할당된 ID 사용
        );
        pushNotificationDTOMap.put(userNotification.getUser().getId(), pushNotificationDTO);
      } catch (Exception e) {
        log.error("알림 DTO 생성 실패: userId={}, error={}", userNotification.getUser().getId(),
            e.getMessage(), e);
      }
    }
    return pushNotificationDTOMap;
  }

  private PushNotificationDTO buildPushNotificationDTO(
      Map<String, Object> contextData,
      NotificationTemplate template,
      Long notificationId) {
    return PushNotificationDTO.builder()
        .notificationId(notificationId)
        .title(template.getTitle())
        .content(template.getContent())
        .redirectUrl(template.getRedirectUrl())
        .contextData(contextData)
        .categoryType(NotificationCategoryType.fromCategoryId(template.getCategory().getId()))
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
