package com.homesweet.notification.domain.broadcast;

import com.homesweet.notification.auth.entity.OAuth2Provider;
import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.entity.UserRole;
import com.homesweet.notification.auth.repository.UserRepository;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.NotificationTemplateType;
import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.domain.broadcast.domain.NotificationType;
import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import com.homesweet.notification.domain.broadcast.entity.UserBroadcastState;
import com.homesweet.notification.domain.broadcast.repository.BroadcastNotificationRepository;
import com.homesweet.notification.domain.broadcast.repository.UserBroadcastStateRepository;
import com.homesweet.notification.domain.broadcast.service.BroadcastNotificationService;
import com.homesweet.notification.domain.event.CustomNotificationEvent;
import com.homesweet.notification.domain.event.BroadcastNotificationEvent;
import com.homesweet.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.notification.domain.notification.CustomNotification;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.entity.NotificationCategory;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.repository.NotificationCategoryRepository;
import com.homesweet.notification.repository.NotificationTemplateRepository;
import com.homesweet.notification.repository.UserNotificationRepository;
import com.homesweet.notification.service.impl.NotificationAPIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BroadcastNotificationService.class, NotificationAPIService.class})
class BroadcastNotificationIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationCategoryRepository categoryRepository;
    @Autowired
    private NotificationTemplateRepository templateRepository;
    @Autowired
    private UserNotificationRepository userNotificationRepository;
    @Autowired
    private BroadcastNotificationRepository broadcastNotificationRepository;
    @Autowired
    private UserBroadcastStateRepository userBroadcastStateRepository;
    @Autowired
    private BroadcastNotificationService broadcastNotificationService;
    @Autowired
    private NotificationAPIService notificationAPIService;

    private User user;
    private NotificationTemplate template;

    @BeforeEach
    void setUp() {
        user = createUser("broadcast user", LocalDateTime.of(2026, 1, 10, 0, 0));

        NotificationCategory category = categoryRepository.save(NotificationCategory.builder()
                .categoryType(NotificationCategoryType.SYSTEM)
                .build());
        template = templateRepository.save(NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.SYSTEM_UPDATE)
                .title("시스템 업데이트")
                .content("업데이트 내용")
                .redirectUrl("/system/update")
                .build());
    }

    private User createUser(String name, LocalDateTime createdAt) {
        String unique = name.replace(' ', '-') + "-" + System.nanoTime();
        return userRepository.save(User.builder()
                .email(unique + "@example.com")
                .name(name)
                .provider(OAuth2Provider.GOOGLE)
                .providerId(unique)
                .role(UserRole.USER)
                .createdAt(createdAt)
                .build());
    }

    @Test
    @DisplayName("브로드캐스트는 가입 시각 이후 사용자에게만 노출된다")
    void futureJoinerIsExcluded() {
        BroadcastNotification notification = broadcastNotificationRepository.save(BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.ALL)
                .contextData(Map.of("version", "1"))
                .createdAt(LocalDateTime.of(2026, 1, 10, 0, 1))
                .build());

        assertThat(broadcastNotificationRepository.findCandidates(
                user.getId(), user.getCreatedAt(), AudienceType.ALL,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .containsExactly(notification);
        assertThat(broadcastNotificationRepository.findCandidates(
                user.getId(), LocalDateTime.of(2026, 1, 10, 0, 2), AudienceType.ALL,
                org.springframework.data.domain.PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    @DisplayName("USER_ID_RANGE는 양 끝 경계를 포함한다")
    void rangeIncludesBothBoundaries() {
        User upperBoundary = createUser("upper boundary", user.getCreatedAt());
        User outside = createUser("outside range", user.getCreatedAt());
        BroadcastNotification notification = broadcastNotificationRepository.save(BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.USER_ID_RANGE)
                .targetMinUserId(user.getId())
                .targetMaxUserId(upperBoundary.getId())
                .contextData(Map.of())
                .createdAt(LocalDateTime.of(2026, 1, 10, 0, 1))
                .build());

        assertThat(broadcastNotificationRepository.findCandidates(
                user.getId(), user.getCreatedAt(), AudienceType.ALL,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .containsExactly(notification);
        assertThat(broadcastNotificationRepository.findCandidates(
                upperBoundary.getId(), upperBoundary.getCreatedAt(), AudienceType.ALL,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .containsExactly(notification);
        assertThat(broadcastNotificationRepository.findCandidates(
                outside.getId(), outside.getCreatedAt(), AudienceType.ALL,
                org.springframework.data.domain.PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    @DisplayName("통합 목록은 개인·단체 혼합 결과에서 정확히 최신 20개를 반환한다")
    void integratedFeedKeepsArrayItemsAndOrdering() {
        for (int i = 1; i <= 15; i++) {
            broadcastNotificationRepository.save(BroadcastNotification.builder()
                    .template(template)
                    .audienceType(AudienceType.ALL)
                    .contextData(Map.of("source", "broadcast"))
                    .createdAt(LocalDateTime.of(2026, 1, 10, 1, i))
                    .build());
        }
        for (int i = 1; i <= 10; i++) {
            userNotificationRepository.save(UserNotification.builder()
                    .user(user)
                    .template(template)
                    .contextData(Map.of("source", "personal"))
                    .createdAt(LocalDateTime.of(2026, 1, 10, 2, i))
                    .build());
        }

        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(
                user.getId(), user.getCreatedAt());

        assertThat(result).hasSize(20);
        assertThat(result).extracting(PushNotificationDTO::getNotificationType)
                .containsExactlyElementsOf(List.of(
                        NotificationType.PERSONAL, NotificationType.PERSONAL,
                        NotificationType.PERSONAL, NotificationType.PERSONAL,
                        NotificationType.PERSONAL, NotificationType.PERSONAL,
                        NotificationType.PERSONAL, NotificationType.PERSONAL,
                        NotificationType.PERSONAL, NotificationType.PERSONAL,
                        NotificationType.BROADCAST, NotificationType.BROADCAST,
                        NotificationType.BROADCAST, NotificationType.BROADCAST,
                        NotificationType.BROADCAST, NotificationType.BROADCAST,
                        NotificationType.BROADCAST, NotificationType.BROADCAST,
                        NotificationType.BROADCAST, NotificationType.BROADCAST));
        assertThat(result.get(10).getCreatedAt())
                .isEqualTo(LocalDateTime.of(2026, 1, 10, 1, 15));
        assertThat(result.get(19).getCreatedAt())
                .isEqualTo(LocalDateTime.of(2026, 1, 10, 1, 6));
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getCreatedAt())
                    .isAfterOrEqualTo(result.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("브로드캐스트 생성은 broadcast row 하나만 추가한다")
    void createDoesNotFanOutRows() {
        long broadcastCount = broadcastNotificationRepository.count();
        long personalCount = userNotificationRepository.count();
        long stateCount = userBroadcastStateRepository.count();

        broadcastNotificationService.create(new BroadcastNotificationEvent(
                AudienceType.ALL, null, null, template.getId(),
                null, null, null, Map.of("source", "kafka")));

        assertThat(broadcastNotificationRepository.count()).isEqualTo(broadcastCount + 1);
        assertThat(userNotificationRepository.count()).isEqualTo(personalCount);
        assertThat(userBroadcastStateRepository.count()).isEqualTo(stateCount);
    }

    @Test
    @DisplayName("한 사용자의 읽음·삭제 상태는 다른 사용자의 피드에 영향을 주지 않는다")
    void stateIsolatedBetweenUsers() {
        User otherUser = createUser("other user", user.getCreatedAt());
        BroadcastNotification notification = broadcastNotificationRepository.save(BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.ALL)
                .contextData(Map.of())
                .createdAt(LocalDateTime.of(2026, 1, 10, 0, 1))
                .build());

        broadcastNotificationService.markAsRead(user.getId(), user.getCreatedAt(), notification.getId());
        broadcastNotificationService.markAsDeleted(user.getId(), user.getCreatedAt(), notification.getId());

        assertThat(notificationAPIService.getAllNotifications(user.getId(), user.getCreatedAt())).isEmpty();
        List<PushNotificationDTO> otherFeed = notificationAPIService.getAllNotifications(
                otherUser.getId(), otherUser.getCreatedAt());
        assertThat(otherFeed).hasSize(1);
        assertThat(otherFeed.get(0).getNotificationType()).isEqualTo(NotificationType.BROADCAST);
        assertThat(otherFeed.get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("기존 임의 사용자 목록 이벤트는 1000명 초과도 수용한다")
    void legacyEventsDoNotImposeNewUserLimit() {
        List<Long> userIds = LongStream.rangeClosed(1, 1001).boxed().toList();
        var templateNotification = com.homesweet.notification.domain.notification.OrderNotification.OrderCompleted
                .builder().userName("user").orderId(1L).build();
        var customNotification = CustomNotification.builder()
                .title("title")
                .content("content")
                .redirectUrl("/url")
                .contextData(Map.of())
                .build();

        assertThat(new TemplateNotificationEvent(userIds, templateNotification).userIds()).hasSize(1001);
        assertThat(new CustomNotificationEvent(userIds, customNotification).userIds()).hasSize(1001);
    }

    @Test
    @DisplayName("읽음과 삭제는 원자적 upsert로 멱등 처리된다")
    void stateUpsertIsIdempotent() {
        BroadcastNotification notification = broadcastNotificationService.create(
                new BroadcastNotificationEvent(
                        AudienceType.ALL, null, null, template.getId(),
                        null, null, null, Map.of()));

        broadcastNotificationService.markAsRead(user.getId(), user.getCreatedAt(), notification.getId());
        broadcastNotificationService.markAsRead(user.getId(), user.getCreatedAt(), notification.getId());
        broadcastNotificationService.markAsDeleted(user.getId(), user.getCreatedAt(), notification.getId());
        broadcastNotificationService.markAsDeleted(user.getId(), user.getCreatedAt(), notification.getId());

        UserBroadcastState state = userBroadcastStateRepository.findById(
                new com.homesweet.notification.domain.broadcast.entity.UserBroadcastStateId(
                        user.getId(), notification.getId())).orElseThrow();
        assertThat(state.isRead()).isTrue();
        assertThat(state.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("지원하지 않는 사용자 생성 시각은 인증 실패로 처리된다")
    void unsupportedPrincipalDataIsRejected() {
        assertThatThrownBy(() -> broadcastNotificationService.markAsRead(
                user.getId(), null, 1L))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_MISSING);
    }
}
