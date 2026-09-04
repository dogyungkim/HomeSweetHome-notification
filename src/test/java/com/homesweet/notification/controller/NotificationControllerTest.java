package com.homesweet.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.notification.auth.entity.OAuth2Provider;
import com.homesweet.notification.auth.entity.OAuth2UserPrincipal;
import com.homesweet.notification.auth.entity.User;
import com.homesweet.notification.auth.entity.UserRole;
import com.homesweet.notification.config.security.jwt.JwtAuthenticationEntryPoint;
import com.homesweet.notification.config.security.jwt.JwtTokenProvider;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.broadcast.domain.NotificationType;
import com.homesweet.notification.domain.broadcast.service.BroadcastNotificationService;
import com.homesweet.notification.dto.NotificationFeedResponse;
import com.homesweet.notification.dto.NotificationItemDTO;
import com.homesweet.notification.service.impl.NotificationAPIService;
import com.homesweet.notification.service.impl.NotificationProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationAPIService notificationAPIService;

    @MockBean
    private BroadcastNotificationService broadcastNotificationService;

    @MockBean
    private KafkaTemplate kafkaTemplate;

    @MockBean
    private NotificationProcessor notificationProcessor;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private User testUser;
    private OAuth2UserPrincipal principal;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .provider(OAuth2Provider.GOOGLE)
                .role(UserRole.USER)
                .build();

        principal = new OAuth2UserPrincipal(testUser, Map.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("통합 알림 목록 조회 API 성공")
    void getNotifications_Success() throws Exception {
        NotificationFeedResponse response = NotificationFeedResponse.builder()
                .items(List.of(NotificationItemDTO.builder()
                        .notificationType(NotificationType.BROADCAST)
                        .notificationId(100L)
                        .title("공지")
                        .content("내용")
                        .categoryType(NotificationCategoryType.SYSTEM)
                        .createdAt(LocalDateTime.now())
                        .build()))
                .build();

        given(notificationAPIService.getIntegratedFeed(eq(1L), any())).willReturn(response);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].notificationId").value(100L))
                .andExpect(jsonPath("$.items[0].notificationType").value("BROADCAST"));
    }

    @Test
    @DisplayName("단체 알림 읽음 처리 API 성공")
    void markBroadcastAsRead_Success() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/broadcast/{id}/read", 100L))
                .andExpect(status().isOk());

        verify(broadcastNotificationService).markBroadcastAsRead(eq(1L), any(), eq(100L));
    }

    @Test
    @DisplayName("단체 알림 삭제 처리 API 성공")
    void deleteBroadcastNotification_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/broadcast/{id}", 100L))
                .andExpect(status().isOk());

        verify(broadcastNotificationService).markBroadcastAsDeleted(eq(1L), any(), eq(100L));
    }
}
