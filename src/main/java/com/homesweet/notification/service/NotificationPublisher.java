package com.homesweet.notification.service;

import com.homesweet.notification.dto.NotificationMessage;
import com.homesweet.notification.dto.PushNotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

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
    
    public void publishBulk(Map<Long, PushNotificationDTO> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();
            @SuppressWarnings("unchecked")
            RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

            byte[] channel = stringSerializer.serialize(TOPIC);

            notifications.forEach((userId, notification) -> {
                NotificationMessage message = new NotificationMessage(userId, notification);
                byte[] payload = valueSerializer.serialize(message);
                connection.publish(channel, payload);
            });
            return null;
        });

        log.debug("Bulk notification published to Redis: count={}", notifications.size());
    }
}
