package com.homesweet.notification.domain.broadcast.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBroadcastNotificationRequest {

    @NotNull(message = "대상 정보(audience)는 필수입니다.")
    @Valid
    private BroadcastAudienceRequest audience;

    /**
     * 템플릿 알림인 경우 템플릿 ID (커스텀 알림인 경우 null)
     */
    private Long templateId;

    /**
     * 커스텀 알림용 제목 (templateId가 없을 때 필수)
     */
    private String title;

    /**
     * 커스텀 알림용 본문 (templateId가 없을 때 필수)
     */
    private String content;

    /**
     * 커스텀 알림용 이동 URL (templateId가 없을 때 필수)
     */
    private String redirectUrl;

    /**
     * 추가 컨텍스트 데이터
     */
    private Map<String, Object> contextData;
}
