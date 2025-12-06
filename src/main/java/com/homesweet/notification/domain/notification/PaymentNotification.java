package com.homesweet.notification.domain.notification;

import com.homesweet.notification.domain.NotificationTemplateType;

import lombok.Builder;
import lombok.Getter;


/**
 * 결제 관련 알림 클래스
 * 
 * 결제 관련 알림들을 내부 클래스로 그룹화합니다.
 * 
 * @author dogyungkim
 */
public class PaymentNotification {
    
    /**
     * 결제 성공 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - amount: String - 결제 금액
     */
    @Getter
    public static class PaymentSuccess implements TemplateNotification {
        private final String userName;
        private final String amount;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.PAYMENT_SUCCESS;
        
        @Builder
        public PaymentSuccess(String userName, String amount) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for PAYMENT_SUCCESS notification");
            }
            if (amount == null || amount.isBlank()) {
                throw new IllegalArgumentException("amount is required for PAYMENT_SUCCESS notification");
            }
            this.userName = userName;
            this.amount = amount;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 결제 실패 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     */
    @Getter
    public static class PaymentFailed implements TemplateNotification {
        private final String userName;
        private final Long orderId;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.PAYMENT_FAILED;
        
        @Builder
        public PaymentFailed(String userName, Long orderId) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for PAYMENT_FAILED notification");
            }
            if (orderId == null) {
                throw new IllegalArgumentException("orderId is required for PAYMENT_FAILED notification");
            }
            this.userName = userName;
            this.orderId = orderId;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
    
    /**
     * 환불 완료 알림
     * 
     * 📋 필요한 필드:
     * - userName: String - 사용자 이름
     * - amount: String - 환불 금액
     */
    @Getter
    public static class PaymentRefunded implements TemplateNotification {
        private final String userName;
        private final Long amount;
        
        private final NotificationTemplateType eventType = NotificationTemplateType.PAYMENT_REFUNDED;
        
        @Builder
        public PaymentRefunded(String userName, Long amount) {
            if (userName == null || userName.isBlank()) {
                throw new IllegalArgumentException("userName is required for PAYMENT_REFUNDED notification");
            }
            if (amount == null) {
                throw new IllegalArgumentException("amount is required for PAYMENT_REFUNDED notification");
            }
            this.userName = userName;
            this.amount = amount;
        }
        
        @Override
        public NotificationTemplateType getEventType() {
            return eventType;
        }
    }
}
