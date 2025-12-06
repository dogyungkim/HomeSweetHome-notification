package com.homesweet.notification.domain.notification;

import com.homesweet.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 정산 관련 알림 클래스
 * 
 * 정산 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class SettlementNotification {
    
    /**
     * 정산 완료 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - settlementId: Long - 정산 ID
     * - amount: Long - 정산 금액
     * - settlementName: String - 정산 이름
     */
    @Getter
    public static class SettlementCompleted implements TemplateNotification {
        private final String userName;
        private final Long settlementId;
        private final Long amount;
        private final String settlementName;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.SETTLEMENT_COMPLETED;
        
        @Builder
        public SettlementCompleted(String userName, Long settlementId, Long amount, String settlementName) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for SETTLEMENT_COMPLETED notification");
            }
            if (settlementId == null) {
                throw new IllegalArgumentException("settlementId is required for SETTLEMENT_COMPLETED notification");
            }
            if (amount == null) {
                throw new IllegalArgumentException("amount is required for SETTLEMENT_COMPLETED notification");
            }
            if (settlementName == null || settlementName.isBlank()) {
                throw new IllegalArgumentException("settlementName is required for SETTLEMENT_COMPLETED notification");
            }
            this.userName = userName;
            this.settlementId = settlementId;
            this.amount = amount;
            this.settlementName = settlementName;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 정산 실패 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - settlementId: Long - 정산 ID
     */
    @Getter
    public static class SettlementFailed implements TemplateNotification {
        private final String userName;
        private final Long settlementId;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.SETTLEMENT_FAILED;
        
        @Builder
        public SettlementFailed(String userName, Long settlementId) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for SETTLEMENT_FAILED notification");
            }
            if (settlementId == null) {
                throw new IllegalArgumentException("settlementId is required for SETTLEMENT_FAILED notification");
            }
            this.userName = userName;
            this.settlementId = settlementId;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
}
