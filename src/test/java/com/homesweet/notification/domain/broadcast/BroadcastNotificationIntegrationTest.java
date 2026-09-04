package com.homesweet.notification.domain.broadcast;

import com.homesweet.notification.auth.entity.OAuth2Provider;
import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.entity.UserRole;
import com.homesweet.notification.auth.repository.UserRepository;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.NotificationTemplateType;
import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.domain.broadcast.domain.NotificationType;
import com.homesweet.notification.domain.broadcast.dto.BroadcastAudienceRequest;
import com.homesweet.notification.domain.broadcast.dto.BroadcastNotificationResponse;
import com.homesweet.notification.domain.broadcast.dto.CreateBroadcastNotificationRequest;
import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import com.homesweet.notification.domain.broadcast.repository.BroadcastNotificationRepository;
import com.homesweet.notification.domain.broadcast.repository.UserBroadcastStateRepository;
import com.homesweet.notification.domain.broadcast.service.BroadcastNotificationService;
import com.homesweet.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.notification.domain.notification.OrderNotification;
import com.homesweet.notification.dto.NotificationFeedResponse;
import com.homesweet.notification.entity.NotificationCategory;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.repository.NotificationCategoryRepository;
import com.homesweet.notification.repository.NotificationTemplateRepository;
import com.homesweet.notification.repository.UserNotificationRepository;
import com.homesweet.notification.service.NotificationPublisher;
import com.homesweet.notification.service.impl.NotificationAPIService;
import com.homesweet.notification.service.impl.NotificationProcessor;
import com.homesweet.notification.service.impl.UserNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Notification_Design.md Section 11.1 필수 통합 테스트 10종
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        BroadcastNotificationService.class,
        NotificationAPIService.class,
        NotificationProcessor.class,
        UserNotificationService.class
})
@Transactional
class BroadcastNotificationIntegrationTest {

    @Autowired
    private BroadcastNotificationService broadcastNotificationService;

    @Autowired
    private NotificationAPIService notificationAPIService;

    @Autowired
    private NotificationProcessor notificationProcessor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationCategoryRepository categoryRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private BroadcastNotificationRepository broadcastNotificationRepository;

    @Autowired
    private UserBroadcastStateRepository userBroadcastStateRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @MockBean
    private NotificationPublisher notificationPublisher;

    private NotificationCategory category;
    private NotificationTemplate template;

    private User user1;
    private User user2;
    private User user3;

    private final LocalDateTime t0 = LocalDateTime.of(2026, 8, 25, 10, 0, 0); // Early user signup
    private final LocalDateTime t1 = LocalDateTime.of(2026, 8, 25, 11, 0, 0); // Broadcast created
    private final LocalDateTime t2 = LocalDateTime.of(2026, 8, 25, 12, 0, 0); // Late user signup
    private final LocalDateTime t3 = LocalDateTime.of(2026, 8, 25, 13, 0, 0); // Personal notification

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(NotificationCategory.builder()
                .categoryType(NotificationCategoryType.SYSTEM)
                .build());

        template = templateRepository.save(NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.SYSTEM_UPDATE)
                .title("시스템 공지")
                .content("공지사항 내용입니다.")
                .redirectUrl("/notice")
                .build());

        // 사용자 생성
        user1 = User.builder()
                .email("user1@test.com")
                .name("User 1")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        userRepository.save(user1);
        ReflectionTestUtils.setField(user1, "createdAt", t0);

        user2 = User.builder()
                .email("user2@test.com")
                .name("User 2")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        userRepository.save(user2);
        ReflectionTestUtils.setField(user2, "createdAt", t0);

        user3 = User.builder()
                .email("user3@test.com")
                .name("User 3")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();
        userRepository.save(user3);
        ReflectionTestUtils.setField(user3, "createdAt", t2);
    }

    @Test
    @DisplayName("1. ALL 단체 알림 생성 시 broadcast_notification 한 행만 생성된다")
    void test1_broadcastNotificationSingleRowCreated() {
        long beforeCount = broadcastNotificationRepository.count();

        BroadcastAudienceRequest audience = BroadcastAudienceRequest.builder()
                .type(AudienceType.ALL)
                .build();

        broadcastNotificationService.sendTemplateBroadcast(audience, template.getId(), Map.of());

        long afterCount = broadcastNotificationRepository.count();
        assertThat(afterCount - beforeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("2. 단체 알림 생성 시 user_notification과 user_broadcast_state가 생성되지 않는다")
    void test2_noUserNotificationOrStateCreatedOnBroadcast() {
        long beforeUserNotificationCount = userNotificationRepository.count();
        long beforeUserBroadcastStateCount = userBroadcastStateRepository.count();

        BroadcastAudienceRequest audience = BroadcastAudienceRequest.builder()
                .type(AudienceType.ALL)
                .build();

        broadcastNotificationService.sendCustomBroadcast(audience, "전체 알림", "내용", "/url", Map.of());

        assertThat(userNotificationRepository.count()).isEqualTo(beforeUserNotificationCount);
        assertThat(userBroadcastStateRepository.count()).isEqualTo(beforeUserBroadcastStateCount);
    }

    @Test
    @DisplayName("3. 단체 알림 생성 이후 가입자는 해당 알림을 조회하지 못한다")
    void test3_lateUserCannotSeePastBroadcast() {
        // given: t1 시점에 단체 알림 생성
        BroadcastNotification broadcast = BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.ALL)
                .createdAt(t1)
                .build();
        broadcastNotificationRepository.save(broadcast);

        // when: t0 가입자(user1)와 t2 가입자(user3)의 피드 조회
        NotificationFeedResponse feedUser1 = notificationAPIService.getIntegratedFeed(user1.getId(), t0);
        NotificationFeedResponse feedUser3 = notificationAPIService.getIntegratedFeed(user3.getId(), t2);

        // then: user1은 단체 알림을 보고, user3은 보지 못함
        assertThat(feedUser1.getItems()).hasSize(1);
        assertThat(feedUser1.getItems().get(0).getNotificationId()).isEqualTo(broadcast.getId());

        assertThat(feedUser3.getItems()).isEmpty();
    }

    @Test
    @DisplayName("4. USER_ID_RANGE의 최소·최대 경계 사용자는 대상에 포함된다")
    void test4_boundaryUsersIncludedInRange() {
        Long minId = user1.getId();
        Long maxId = user2.getId();
        if (minId > maxId) {
            Long temp = minId;
            minId = maxId;
            maxId = temp;
        }

        BroadcastNotification broadcast = BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.USER_ID_RANGE)
                .targetMinUserId(minId)
                .targetMaxUserId(maxId)
                .createdAt(t1)
                .build();
        broadcastNotificationRepository.save(broadcast);

        NotificationFeedResponse feedMin = notificationAPIService.getIntegratedFeed(minId, t0);
        NotificationFeedResponse feedMax = notificationAPIService.getIntegratedFeed(maxId, t0);

        assertThat(feedMin.getItems()).hasSize(1);
        assertThat(feedMax.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("5. USER_ID_RANGE 밖 사용자는 대상에 포함되지 않는다")
    void test5_outOfRangeUserExcluded() {
        Long minId = 1000L;
        Long maxId = 2000L;

        BroadcastNotification broadcast = BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.USER_ID_RANGE)
                .targetMinUserId(minId)
                .targetMaxUserId(maxId)
                .createdAt(t1)
                .build();
        broadcastNotificationRepository.save(broadcast);

        // user1의 ID는 1000~2000 밖임
        NotificationFeedResponse feed = notificationAPIService.getIntegratedFeed(user1.getId(), t0);
        assertThat(feed.getItems()).isEmpty();
    }

    @Test
    @DisplayName("6. 개인·단체 알림이 createdAt 기준으로 병합되어 최신 20건만 반환된다")
    void test6_mergedOrderByCreatedAtDescLimit20() {
        // given: 15개의 단체 알림 (t1)
        for (int i = 1; i <= 15; i++) {
            BroadcastNotification b = BroadcastNotification.builder()
                    .template(template)
                    .audienceType(AudienceType.ALL)
                    .createdAt(t1.plusMinutes(i))
                    .build();
            broadcastNotificationRepository.save(b);
        }

        // given: 10개의 개인 알림 (t1 이전 및 이후 섞임)
        for (int i = 1; i <= 10; i++) {
            UserNotification un = UserNotification.builder()
                    .user(user1)
                    .template(template)
                    .contextData(Map.of())
                    .isRead(false)
                    .isDeleted(false)
                    .build();
            userNotificationRepository.save(un);
            ReflectionTestUtils.setField(un, "createdAt", t1.plusMinutes(i * 2));
        }

        // when: user1 피드 조회
        NotificationFeedResponse feed = notificationAPIService.getIntegratedFeed(user1.getId(), t0);

        // then: 정확히 20건 반환 및 내림차순 정렬 확인
        assertThat(feed.getItems()).hasSize(20);
        for (int i = 0; i < feed.getItems().size() - 1; i++) {
            LocalDateTime current = feed.getItems().get(i).getCreatedAt();
            LocalDateTime next = feed.getItems().get(i + 1).getCreatedAt();
            assertThat(current).isAfterOrEqualTo(next);
        }
    }

    @Test
    @DisplayName("7. 한 사용자의 읽음·삭제가 다른 사용자에게 영향을 주지 않는다")
    void test7_stateIsolationBetweenUsers() {
        BroadcastNotification broadcast = BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.ALL)
                .createdAt(t1)
                .build();
        broadcastNotificationRepository.save(broadcast);

        // user1이 읽음 및 삭제 처리
        broadcastNotificationService.markBroadcastAsRead(user1.getId(), t0, broadcast.getId());
        broadcastNotificationService.markBroadcastAsDeleted(user1.getId(), t0, broadcast.getId());

        // user1에게는 삭제되어 보이지 않아야 함
        NotificationFeedResponse feedUser1 = notificationAPIService.getIntegratedFeed(user1.getId(), t0);
        assertThat(feedUser1.getItems()).isEmpty();

        // user2에게는 여전히 안 읽은 상태로 보여야 함
        NotificationFeedResponse feedUser2 = notificationAPIService.getIntegratedFeed(user2.getId(), t0);
        assertThat(feedUser2.getItems()).hasSize(1);
        assertThat(feedUser2.getItems().get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("8. 단체 알림 읽음·삭제 반복 요청의 결과가 같다 (멱등성)")
    void test8_idempotentStateRequests() {
        BroadcastNotification broadcast = BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.ALL)
                .createdAt(t1)
                .build();
        broadcastNotificationRepository.save(broadcast);

        // 읽음 처리 2회 반복
        broadcastNotificationService.markBroadcastAsRead(user1.getId(), t0, broadcast.getId());
        broadcastNotificationService.markBroadcastAsRead(user1.getId(), t0, broadcast.getId());

        long stateCount = userBroadcastStateRepository.count();
        assertThat(stateCount).isEqualTo(1);

        // 삭제 처리 2회 반복
        broadcastNotificationService.markBroadcastAsDeleted(user1.getId(), t0, broadcast.getId());
        broadcastNotificationService.markBroadcastAsDeleted(user1.getId(), t0, broadcast.getId());

        assertThat(userBroadcastStateRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("9. 기존 단일 사용자 알림 경로가 그대로 동작한다")
    void test9_legacySingleUserNotificationPathWorks() {
        long beforeCount = userNotificationRepository.count();

        var notification = OrderNotification.OrderCompleted.builder()
                .userName("홍길동")
                .orderId(9999L)
                .build();

        notificationProcessor.processTemplateNotification(new TemplateNotificationEvent(user1.getId(), notification));

        assertThat(userNotificationRepository.count()).isEqualTo(beforeCount + 1);
    }

    @Test
    @DisplayName("10. 기존 소규모 임의 사용자 목록 알림이 목록에 포함된 사용자에게만 생성된다")
    void test10_legacyArbitraryUserIdsNotificationWorks() {
        long beforeUser1 = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user1.getId()).size();
        long beforeUser2 = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user2.getId()).size();
        long beforeUser3 = userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user3.getId()).size();

        var notification = OrderNotification.OrderCompleted.builder()
                .userName("다수사용자")
                .orderId(8888L)
                .build();

        // user1, user2에게만 발송
        notificationProcessor.processTemplateNotification(new TemplateNotificationEvent(List.of(user1.getId(), user2.getId()), notification));

        assertThat(userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user1.getId()).size()).isEqualTo(beforeUser1 + 1);
        assertThat(userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user2.getId()).size()).isEqualTo(beforeUser2 + 1);
        assertThat(userNotificationRepository.findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user3.getId()).size()).isEqualTo(beforeUser3);
    }
}
