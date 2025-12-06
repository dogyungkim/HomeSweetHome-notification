package com.homesweet.notification.service;

import com.homesweet.notification.dto.NotificationMessage;
import com.homesweet.notification.dto.PushNotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String TOPIC = "notification:push";

    public void publish(Long userId, PushNotificationDTO notification) {
        NotificationMessage message = new NotificationMessage(userId, notification);
        redisTemplate.convertAndSend(TOPIC, message);
        log.debug("Notification published to Redis: userId={}", userId);
    }
}
