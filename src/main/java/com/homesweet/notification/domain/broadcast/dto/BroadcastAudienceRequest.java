package com.homesweet.notification.domain.broadcast.dto;

import com.homesweet.notification.domain.broadcast.domain.AudienceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastAudienceRequest {

    @NotNull(message = "대상 유형(type)은 필수입니다.")
    private AudienceType type;

    private Long minUserId;

    private Long maxUserId;
}
