package com.homesweet.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.notification.auth.entity.OAuth2Provider;
import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.entity.UserRole;
import com.homesweet.notification.auth.repository.UserRepository;
import com.homesweet.notification.auth.service.UserService;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.NotificationTemplateType;
import com.homesweet.notification.domain.event.CustomNotificationEvent;
import com.homesweet.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.notification.domain.notification.CustomNotification;
import com.homesweet.notification.domain.notification.SystemNotification;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.entity.NotificationCategory;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.repository.NotificationCategoryRepository;
import com.homesweet.notification.repository.NotificationTemplateRepository;
import com.homesweet.notification.repository.UserNotificationRepository;
import com.homesweet.notification.repository.impl.H2UserNotificationJdbcRepository;
import com.homesweet.notification.service.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
                NotificationProcessor.class,
                UserNotificationService.class,
                UserService.class,
                H2UserNotificationJdbcRepository.class,
                ObjectMapper.class
})
@Transactional
class NotificationProcessorIT {

        @org.springframework.boot.test.context.TestConfiguration
        @org.springframework.data.jpa.repository.config.EnableJpaAuditing
        static class TestConfig {
        }

        @Autowired
        private NotificationProcessor notificationProcessor;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private NotificationCategoryRepository categoryRepository;

        @Autowired
        private NotificationTemplateRepository templateRepository;

        @Autowired
        private UserNotificationRepository userNotificationRepository;

        @MockitoBean
        private NotificationPublisher notificationPublisher;

        private User testUser;
        private NotificationTemplate testTemplate;

        @BeforeEach
        void setUp() {
                // 1. 테스트 사용자 생성
                testUser = User.builder()
                                .email("proc_it@example.com")
                                .name("Proc IT User")
                                .provider(OAuth2Provider.GOOGLE)
                                .providerId("proc_google_123")
                                .role(UserRole.USER)
                                .build();
                userRepository.save(testUser);

                // 2. 카테고리 생성
                NotificationCategory category = NotificationCategory.builder()
                                .categoryType(NotificationCategoryType.SYSTEM)
                                .build();
                categoryRepository.save(category);

                // 3. 템플릿 생성
                testTemplate = NotificationTemplate.builder()
                                .category(category)
                                .templateType(NotificationTemplateType.SYSTEM_UPDATE)
                                .title("시스템 점검")
                                .content("시스템 점검 안내입니다.")
                                .redirectUrl("/notice/1")
                                .build();
                templateRepository.save(testTemplate);
        }

        @Test
        @DisplayName("템플릿 알림 이벤트를 받아 DB 저장 및 전송까지의 전체 과정을 수행한다")
        void processTemplateNotification_Integration() {
                // given
                SystemNotification.SystemUpdate notification = new SystemNotification.SystemUpdate("1.0.1", "성능 개선");
                TemplateNotificationEvent event = new TemplateNotificationEvent(testUser.getId(), notification);

                // when
                notificationProcessor.processTemplateNotification(event);

                // then: DB 저장 확인
                List<UserNotification> saved = userNotificationRepository
                                .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(testUser.getId());
                assertThat(saved).hasSize(1);
                assertThat(saved.get(0).getTemplate().getTemplateType())
                                .isEqualTo(NotificationTemplateType.SYSTEM_UPDATE);
                assertThat(saved.get(0).getContextData().get("version")).isEqualTo("1.0.1");

                // then: Publisher 호출 확인
                verify(notificationPublisher, atLeastOnce()).publish(eq(testUser.getId()),
                                any(PushNotificationDTO.class));
        }

        @Test
        @DisplayName("커스텀 알림 이벤트를 받아 DB 저장 및 전송까지의 전체 과정을 수행한다")
        void processCustomNotification_Integration() {
                // given
                CustomNotification notification = CustomNotification.builder()
                                .title("긴급 공지")
                                .content("내용입니다")
                                .redirectUrl("/emergency")
                                .contextData(Map.of("critical", true))
                                .build();
                CustomNotificationEvent event = new CustomNotificationEvent(testUser.getId(), notification);

                // when
                notificationProcessor.processCustomNotification(event);

                // then: DB 저장 확인
                List<UserNotification> saved = userNotificationRepository
                                .findTop20ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(testUser.getId());
                assertThat(saved).hasSize(1);
                assertThat(saved.get(0).getTemplate()).isNull(); // 커스텀은 템플릿 없음
                assertThat(saved.get(0).getContextData().get("title")).isEqualTo("긴급 공지");

                // then: Publisher 호출 확인
                verify(notificationPublisher, atLeastOnce()).publish(eq(testUser.getId()),
                                any(PushNotificationDTO.class));
        }

        @Test
        @DisplayName("다수 사용자에게 대량의 알림을 배치로 처리한다")
        void processBatchNotification_Integration() {
                // given: 추가 사용자 생성
                User secondUser = User.builder()
                                .email("proc_it2@example.com")
                                .name("User 2")
                                .provider(OAuth2Provider.GOOGLE)
                                .providerId("proc_google_456")
                                .role(UserRole.USER)
                                .build();
                userRepository.save(secondUser);

                SystemNotification.SystemUpdate notification = new SystemNotification.SystemUpdate("2.0.0", "대규모 패치");
                TemplateNotificationEvent event = new TemplateNotificationEvent(
                                List.of(testUser.getId(), secondUser.getId()),
                                notification);

                // when
                notificationProcessor.processTemplateNotification(event);

                // then: DB 저장 확인 (두 명 모두 저장되었는지)
                assertThat(userNotificationRepository.count()).isEqualTo(2);

                // then: Bulk Publisher 호출 확인
                verify(notificationPublisher, atLeastOnce()).publishBulk(anyMap());
        }
}
