package com.homesweet.notification.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.notification.auth.entity.OAuth2Provider;
import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.entity.UserRole;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.NotificationTemplateType;
import com.homesweet.notification.entity.NotificationCategory;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.entity.UserNotification;
import com.homesweet.notification.repository.impl.H2UserNotificationJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 설정 파일의 DataSource를 사용하도록 설정
@Import({ H2UserNotificationJdbcRepository.class, ObjectMapper.class })
class UserNotificationJdbcRepositoryTest {

    @Autowired
    private UserNotificationJdbcRepository userNotificationJdbcRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private NotificationTemplate testTemplate;

    @BeforeEach
    void setUp() {
        // 1. 테스트용 사용자 생성
        String uniqueId = String.valueOf(System.nanoTime());
        testUser = User.builder()
                .email("test" + uniqueId + "@example.com")
                .name("Test User")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("google_" + uniqueId)
                .role(UserRole.USER)
                .build();
        entityManager.persist(testUser);

        // 2. 카테고리 생성
        NotificationCategory category = NotificationCategory.builder()
                .categoryType(NotificationCategoryType.SYSTEM)
                .build();
        entityManager.persist(category);

        // 3. 테스트용 템플릿 생성
        testTemplate = NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.SYSTEM_UPDATE)
                .title("테스트 제목")
                .content("테스트 내용")
                .redirectUrl("/test")
                .build();
        entityManager.persist(testTemplate);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("DataJpaTest 환경에서 Bulk Insert가 정상 동작하고 ID가 할당된다")
    void saveAll_WithDataJpaTest() {
        // given
        int count = 10;
        List<UserNotification> notifications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            notifications.add(UserNotification.builder()
                    .user(testUser)
                    .template(testTemplate)
                    .contextData(Map.of("key", "value " + i))
                    .build());
        }

        // when
        userNotificationJdbcRepository.saveAll(notifications);

        // then
        // 1. 모든 엔티티에 ID가 할당되었는지 확인
        long lastId = -1;
        for (UserNotification n : notifications) {
            assertThat(n.getId()).isNotNull();
            assertThat(n.getId()).isGreaterThan(lastId);
            lastId = n.getId();
        }

        // 2. 실제 DB 저장 확인
        List<UserNotification> saved = entityManager.getEntityManager()
                .createQuery("select un from UserNotification un where un.user.id = :userId", UserNotification.class)
                .setParameter("userId", testUser.getId())
                .getResultList();

        assertThat(saved).hasSize(count);
    }
}
