package com.homesweet.notification.service.impl;

import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.entity.NotificationCategory;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.repository.UserNotificationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationAPIServiceTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @InjectMocks
    private NotificationAPIService notificationAPIService;

    private final Long userId = 1L;
    private User testUser;
    private Map<String, Object> contextData;

    @BeforeEach
    void setUp() {
        testUser = User.builder().build();
        ReflectionTestUtils.setField(testUser, "id", userId);
        contextData = Map.of("key", "value");
    }

    @Nested
    @DisplayName("알림 목록 조회 테스트")
    class GetAllNotifications {

        @Test
        @DisplayName("사용자의 최신 알림 목록을 최대 20개 조회한다")
        void getAllNotifications_Success() {
            // given
            NotificationCategory category = NotificationCategory.builder()
                    .categoryType(NotificationCategoryType.SYSTEM)
                    .build();

            NotificationTemplate template = NotificationTemplate.builder()
                    .title("제목")
                    .content("내용")
                    .redirectUrl("/url")
                    .category(category)
                    .build();

            UserNotification notification = UserNotification.builder()
                    .user(testUser)
                    .template(template)
                    .contextData(contextData)
                    .isRead(false)
                    .isDeleted(false)
                    .build();
            notification.setId(100L);

            given(userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId))
                    .willReturn(List.of(notification));

            // when
            List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(userId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNotificationId()).isEqualTo(100L);
            assertThat(result.get(0).getTitle()).isEqualTo("제목");
            assertThat(result.get(0).getCategoryType()).isEqualTo(NotificationCategoryType.SYSTEM);
            verify(userNotificationRepository, times(1))
                    .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        }
    }

    @Nested
    @DisplayName("알림 읽음 처리 테스트")
    class MarkAsRead {

        @Test
        @DisplayName("알림 아이디 리스트를 전달받아 읽음 처리한다")
        void markAsRead_Success() {
            // given
            List<Long> notificationIds = List.of(1L, 2L);
            UserNotification n1 = spy(
                    UserNotification.builder().user(testUser).contextData(contextData).isRead(false).build());
            UserNotification n2 = spy(
                    UserNotification.builder().user(testUser).contextData(contextData).isRead(false).build());

            given(userNotificationRepository.findByIdInAndUserIdAndNotDeleted(notificationIds, userId))
                    .willReturn(List.of(n1, n2));

            // when
            notificationAPIService.markAsRead(userId, notificationIds);

            // then
            verify(n1, times(1)).markAsRead();
            verify(n2, times(1)).markAsRead();
            assertThat(n1.getIsRead()).isTrue();
            assertThat(n2.getIsRead()).isTrue();
            verify(userNotificationRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("조회된 알림이 없으면 NOTIFICATION_NOT_FOUND 예외가 발생한다")
        void markAsRead_NotFound() {
            // given
            List<Long> notificationIds = List.of(99L);
            given(userNotificationRepository.findByIdInAndUserIdAndNotDeleted(notificationIds, userId))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> notificationAPIService.markAsRead(userId, notificationIds))
                    .isInstanceOf(NotificationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("알림 삭제 처리 테스트")
    class MarkAsDeleted {

        @Test
        @DisplayName("알림 아이디 리스트를 전달받아 삭제 및 읽음 처리한다")
        void markAsDeleted_Success() {
            // given
            List<Long> notificationIds = List.of(1L);
            UserNotification n1 = spy(UserNotification.builder().user(testUser).contextData(contextData)
                    .isDeleted(false).isRead(false).build());

            given(userNotificationRepository.findByIdInAndUserIdAndNotDeleted(notificationIds, userId))
                    .willReturn(List.of(n1));

            // when
            notificationAPIService.markAsDeleted(userId, notificationIds);

            // then
            verify(n1, times(1)).markAsDeleted();
            verify(n1, times(1)).markAsRead();
            assertThat(n1.getIsDeleted()).isTrue();
            assertThat(n1.getIsRead()).isTrue();
            verify(userNotificationRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("조회된 알림이 없으면 NOTIFICATION_NOT_FOUND 예외가 발생한다")
        void markAsDeleted_NotFound() {
            // given
            List<Long> notificationIds = List.of(99L);
            given(userNotificationRepository.findByIdInAndUserIdAndNotDeleted(notificationIds, userId))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> notificationAPIService.markAsDeleted(userId, notificationIds))
                    .isInstanceOf(NotificationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }
}
