package com.homesweet.notification.service.impl;

import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.service.UserService;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.NotificationTemplateType;
import com.homesweet.notification.domain.event.CustomNotificationEvent;
import com.homesweet.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.notification.domain.notification.CustomNotification;
import com.homesweet.notification.domain.notification.TemplateNotification;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.entity.NotificationCategory;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.service.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProcessorTest {

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationProcessor notificationProcessor;

    private User testUser;
    private NotificationTemplate testTemplate;
    private NotificationCategory testCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder().build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testCategory = NotificationCategory.builder()
                .categoryType(NotificationCategoryType.SYSTEM)
                .build();
        ReflectionTestUtils.setField(testCategory, "id", 1L);

        testTemplate = NotificationTemplate.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .redirectUrl("/test")
                .category(testCategory)
                .build();
    }

    @Nested
    @DisplayName("템플릿 알림 처리 테스트")
    class TemplateNotificationTest {

        @Test
        @DisplayName("단일 사용자에 대한 템플릿 알림을 처리한다")
        void processTemplateNotification_SingleUser() {
            // given
            TemplateNotification notification = mock(TemplateNotification.class);
            given(notification.getEventType()).willReturn(NotificationTemplateType.SYSTEM_UPDATE);
            given(notification.toMap()).willReturn(Map.of("key", "value"));

            TemplateNotificationEvent event = new TemplateNotificationEvent(1L, notification);

            given(userNotificationService.getNotificationTemplate(NotificationTemplateType.SYSTEM_UPDATE))
                    .willReturn(testTemplate);
            given(userService.getUserById(1L)).willReturn(testUser);

            UserNotification userNotification = UserNotification.builder()
                    .user(testUser)
                    .template(testTemplate)
                    .contextData(Map.of("key", "value"))
                    .build();
            userNotification.setId(100L);

            given(userNotificationService.createUserNotification(any(), any(), any()))
                    .willReturn(userNotification);

            // when
            notificationProcessor.processTemplateNotification(event);

            // then
            verify(userNotificationService).saveUserNotification(any(UserNotification.class));
            verify(notificationPublisher).publish(eq(1L), any(PushNotificationDTO.class));
        }

        @Test
        @DisplayName("다수 사용자에 대한 템플릿 알림을 배치 처리한다")
        void processTemplateNotification_Batch() {
            // given
            List<Long> userIds = List.of(1L, 2L, 3L);
            TemplateNotification notification = mock(TemplateNotification.class);
            given(notification.getEventType()).willReturn(NotificationTemplateType.SYSTEM_UPDATE);
            given(notification.toMap()).willReturn(Map.of("key", "value"));

            TemplateNotificationEvent event = new TemplateNotificationEvent(userIds, notification);

            given(userNotificationService.getNotificationTemplate(NotificationTemplateType.SYSTEM_UPDATE))
                    .willReturn(testTemplate);

            User user2 = User.builder().build();
            ReflectionTestUtils.setField(user2, "id", 2L);
            User user3 = User.builder().build();
            ReflectionTestUtils.setField(user3, "id", 3L);

            given(userService.getManyUsersById(userIds)).willReturn(List.of(testUser, user2, user3));

            UserNotification un1 = UserNotification.builder().user(testUser).contextData(Map.of()).build();
            un1.setId(101L);
            UserNotification un2 = UserNotification.builder().user(user2).contextData(Map.of()).build();
            un2.setId(102L);
            UserNotification un3 = UserNotification.builder().user(user3).contextData(Map.of()).build();
            un3.setId(103L);

            given(userNotificationService.createUserNotification(any(), any(), any()))
                    .willReturn(un1, un2, un3);

            // when
            notificationProcessor.processTemplateNotification(event);

            // then
            verify(userNotificationService).bulkInsertUserNotifications(anyList());
            verify(notificationPublisher).publishBulk(anyMap());
        }
    }

    @Nested
    @DisplayName("커스텀 알림 처리 테스트")
    class CustomNotificationTest {

        @Test
        @DisplayName("단일 사용자에 대한 커스텀 알림을 처리한다")
        void processCustomNotification_SingleUser() {
            // given
            CustomNotification notification = CustomNotification.builder()
                    .title("커스텀 제목")
                    .content("커스텀 내용")
                    .redirectUrl("/custom")
                    .contextData(Map.of("extra", "data"))
                    .build();

            CustomNotificationEvent event = new CustomNotificationEvent(1L, notification);

            given(userService.getUserById(1L)).willReturn(testUser);

            UserNotification userNotification = UserNotification.builder()
                    .user(testUser)
                    .contextData(Map.of("title", "커스텀 제목"))
                    .build();
            userNotification.setId(200L);

            given(userNotificationService.createUserNotification(eq(testUser), isNull(), anyMap()))
                    .willReturn(userNotification);

            // when
            notificationProcessor.processCustomNotification(event);

            // then
            verify(userNotificationService).saveUserNotification(any(UserNotification.class));
            verify(notificationPublisher).publish(eq(1L), any(PushNotificationDTO.class));
        }

        @Test
        @DisplayName("다수 사용자에 대한 커스텀 알림을 배치 처리한다")
        void processCustomNotification_Batch() {
            // given
            List<Long> userIds = List.of(1L, 2L);
            CustomNotification notification = CustomNotification.builder()
                    .title("커스텀 제목")
                    .content("커스텀 내용")
                    .redirectUrl("/custom")
                    .contextData(Map.of())
                    .build();

            CustomNotificationEvent event = new CustomNotificationEvent(userIds, notification);

            User user2 = User.builder().build();
            ReflectionTestUtils.setField(user2, "id", 2L);
            given(userService.getManyUsersById(userIds)).willReturn(List.of(testUser, user2));

            UserNotification un1 = UserNotification.builder().user(testUser).contextData(Map.of()).build();
            un1.setId(201L);
            UserNotification un2 = UserNotification.builder().user(user2).contextData(Map.of()).build();
            un2.setId(202L);

            given(userNotificationService.createUserNotification(any(), isNull(), anyMap()))
                    .willReturn(un1, un2);

            // when
            notificationProcessor.processCustomNotification(event);

            // then
            verify(userNotificationService).bulkInsertUserNotifications(anyList());
            verify(notificationPublisher).publishBulk(anyMap());
        }
    }
}
