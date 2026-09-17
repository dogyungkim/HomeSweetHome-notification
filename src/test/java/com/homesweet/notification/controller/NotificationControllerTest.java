package com.homesweet.notification.controller;

import com.homesweet.notification.auth.entity.OAuth2Provider;
import com.homesweet.notification.auth.entity.OAuth2UserPrincipal;
import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.entity.UserRole;
import com.homesweet.notification.domain.broadcast.service.BroadcastNotificationService;
import com.homesweet.notification.dto.PushNotificationDTO;
import com.homesweet.notification.exception.ErrorCode;
import com.homesweet.notification.exception.NotificationException;
import com.homesweet.notification.service.impl.NotificationAPIService;
import com.homesweet.notification.service.impl.NotificationProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    @SuppressWarnings("unchecked")
    private NotificationController controller(NotificationAPIService apiService) {
        return new NotificationController(
                apiService,
                mock(KafkaTemplate.class),
                mock(NotificationProcessor.class),
                mock(BroadcastNotificationService.class));
    }

    @Test
    void getNotificationsKeepsListResponseContract() {
        NotificationAPIService apiService = mock(NotificationAPIService.class);
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("user")
                .provider(OAuth2Provider.GOOGLE)
                .providerId("provider-id")
                .role(UserRole.USER)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, Map.of());
        List<PushNotificationDTO> expected = List.of(PushNotificationDTO.builder().notificationId(1L).build());
        when(apiService.getAllNotifications(1L, user.getCreatedAt())).thenReturn(expected);

        ResponseEntity<List<PushNotificationDTO>> response = controller(apiService).getNotifications(principal);

        assertThat(response.getBody()).isSameAs(expected);
        assertThat(response.getBody()).isInstanceOf(List.class);
    }

    @Test
    void missingOrUnsupportedPrincipalFailsAuthentication() {
        NotificationController controller = controller(mock(NotificationAPIService.class));

        assertThatThrownBy(() -> controller.getNotifications(null))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_MISSING);
        assertThatThrownBy(() -> controller.getNotifications("unsupported"))
                .isInstanceOf(NotificationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_MISSING);
    }
}
