package com.homesweet.notification.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.kafka.annotation.KafkaListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import com.homesweet.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.notification.service.impl.NotificationProcessor;

/**
 * Kafka 알림 컨슈머
 * 
 * Kafka에서 알림 메시지를 받아 동기적으로 처리합니다.
 * 동기 처리로 Kafka commit이 실제 알림 처리 완료 후에 이루어지도록 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

  private final NotificationProcessor notificationProcessor;

  @KafkaListener(topics = "notification", groupId = "notification-group", batch = "true")
  public void listenBulk(List<TemplateNotificationEvent> messages) {
    messages.forEach(msg -> {
      notificationProcessor.processTemplateNotification(msg);
    });
  }
}
