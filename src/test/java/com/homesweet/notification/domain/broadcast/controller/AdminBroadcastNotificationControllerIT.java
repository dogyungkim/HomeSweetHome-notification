package com.homesweet.notification.domain.broadcast.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import com.homesweet.notification.domain.broadcast.dto.BroadcastAudienceRequest;
import com.homesweet.notification.domain.broadcast.dto.CreateBroadcastNotificationRequest;
import com.homesweet.notification.domain.broadcast.entity.BroadcastNotification;
import com.homesweet.notification.domain.broadcast.repository.BroadcastNotificationRepository;
import com.homesweet.notification.entity.NotificationCategory;
import com.homesweet.notification.entity.NotificationTemplate;
import com.homesweet.notification.domain.NotificationCategoryType;
import com.homesweet.notification.domain.NotificationTemplateType;
import com.homesweet.notification.repository.NotificationCategoryRepository;
import com.homesweet.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class AdminBroadcastNotificationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BroadcastNotificationRepository broadcastNotificationRepository;

    @Autowired
    private NotificationCategoryRepository categoryRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    private NotificationTemplate template;

    @BeforeEach
    void setUp() {
        NotificationCategory category = categoryRepository.save(NotificationCategory.builder()
                .categoryType(NotificationCategoryType.SYSTEM)
                .build());

        template = templateRepository.save(NotificationTemplate.builder()
                .category(category)
                .templateType(NotificationTemplateType.SYSTEM_UPDATE)
                .title("관리자 공지")
                .content("공지 내용")
                .redirectUrl("/admin/notice")
                .build());
    }

    @Test
    @DisplayName("관리자 템플릿 단체 알림 생성 API 성공")
    @WithMockUser(roles = "ADMIN")
    void createTemplateBroadcast_Success() throws Exception {
        CreateBroadcastNotificationRequest request = CreateBroadcastNotificationRequest.builder()
                .audience(BroadcastAudienceRequest.builder()
                        .type(AudienceType.ALL)
                        .build())
                .templateId(template.getId())
                .contextData(Map.of("key", "value"))
                .build();

        mockMvc.perform(post("/api/v1/admin/broadcast-notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.broadcastNotificationId").exists())
                .andExpect(jsonPath("$.audienceType").value("ALL"));
    }

    @Test
    @DisplayName("단체 알림 단건 조회 API 성공")
    @WithMockUser(roles = "ADMIN")
    void getBroadcastNotification_Success() throws Exception {
        BroadcastNotification broadcast = BroadcastNotification.builder()
                .template(template)
                .audienceType(AudienceType.ALL)
                .build();
        broadcast = broadcastNotificationRepository.save(broadcast);

        mockMvc.perform(get("/api/v1/admin/broadcast-notifications/{id}", broadcast.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broadcastNotificationId").value(broadcast.getId()))
                .andExpect(jsonPath("$.audienceType").value("ALL"));
    }
}
