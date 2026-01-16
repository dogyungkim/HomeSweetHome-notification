package com.homesweet.notification.service.impl;

import com.homesweet.notification.auth.entity.OAuth2Provider;
import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.entity.UserRole;
import com.homesweet.notification.auth.repository.UserRepository;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.NotificationTemplateType;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.entity.NotificationCategory;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.repository.NotificationCategoryRepository;
import com.homesweet.notification.repository.NotificationTemplateRepository;
import com.homesweet.notification.repository.UserNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(NotificationAPIService.class)
@Transactional
class NotificationAPIServiceIT {

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.data.jpa.repository.config.EnableJpaAuditing
    static class TestConfig {
    }

    @Autowired
    private NotificationAPIService notificationAPIService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationCategoryRepository categoryRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    private User testUser;
    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        // 1. 테스트 사용자 생성
        testUser = User.builder()
                .email("it_repo_test@example.com")
                .name("IT Repo User")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("it_google_456")
                .role(UserRole.USER)
                .build();
        userRepository.save(testUser);

        // 2. 카테고리 생성
        NotificationCategory category = NotificationCategory.builder()
                .categoryType(NotificationCategoryType.ORDER)
                .build();
        categoryRepository.save(category);

        // 3. 템플릿 생성
        testTemplate = NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.ORDER_COMPLETED)
                .title("주문 완료")
                .content("주문이 완료되었습니다.")
                .redirectUrl("/orders/1")
                .build();
        templateRepository.save(testTemplate);
    }

    @Test
    @DisplayName("사용자의 최신 알림 목록 20개를 Repository를 통해 조회한다")
    void getAllNotifications_WithRepository() {
        // given: 25개의 알림 생성 및 저장
        for (int i = 1; i <= 25; i++) {
            UserNotification notification = UserNotification.builder()
                    .user(testUser)
                    .template(testTemplate)
                    .contextData(Map.of("orderId", i))
                    .build();
            userNotificationRepository.save(notification);
        }

        // when
        List<PushNotificationDTO> result = notificationAPIService.getAllNotifications(testUser.getId());

        // then
        assertThat(result).hasSize(20);
        assertThat(result.get(0).getTitle()).isEqualTo("주문 완료");
    }

    @Test
    @DisplayName("Repository를 사용하여 알림들을 읽음 처리한다")
    void markAsRead_WithRepository() {
        // given: 2개의 읽지 않은 알림 생성 및 저장
        UserNotification n1 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(Map.of())
                .isRead(false)
                .build();
        UserNotification n2 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(Map.of())
                .isRead(false)
                .build();
        userNotificationRepository.saveAll(List.of(n1, n2));

        List<Long> ids = List.of(n1.getId(), n2.getId());

        // when
        notificationAPIService.markAsRead(testUser.getId(), ids);

        // then: Repository를 통해 상태 확인
        UserNotification updatedN1 = userNotificationRepository.findById(n1.getId()).orElseThrow();
        UserNotification updatedN2 = userNotificationRepository.findById(n2.getId()).orElseThrow();
        assertThat(updatedN1.getIsRead()).isTrue();
        assertThat(updatedN2.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("Repository를 사용하여 알림들을 삭제 처리한다")
    void markAsDeleted_WithRepository() {
        // given: 1개의 알림 생성 및 저장
        UserNotification n1 = UserNotification.builder()
                .user(testUser)
                .template(testTemplate)
                .contextData(Map.of())
                .isDeleted(false)
                .build();
        userNotificationRepository.save(n1);

        // when
        notificationAPIService.markAsDeleted(testUser.getId(), List.of(n1.getId()));

        // then: Repository를 통해 상태 확인
        UserNotification updated = userNotificationRepository.findById(n1.getId()).orElseThrow();
        assertThat(updated.getIsDeleted()).isTrue();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 알림 ID로 조회 시 Repository 결과가 비어있어 예외가 발생한다")
    void markAsRead_NotFound_WithRepository() {
        // when & then
        assertThatThrownBy(() -> notificationAPIService.markAsRead(testUser.getId(), List.of(9999L)))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
