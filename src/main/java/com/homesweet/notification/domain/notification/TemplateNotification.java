package com.homesweet.notification.domain.notification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.notification.domain.NotificationTemplateType;

import java.util.Map;

/**
 * 템플릿 기반 알림 인터페이스
 * 
 * EventType과 데이터를 하나로 묶어 타입 안전성을 제공합니다.
 * 
 * @author dogyungkim
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "eventType", visible = true)
@JsonSubTypes({
        // System
        @JsonSubTypes.Type(value = SystemNotification.SystemMaintenance.class, name = "SYSTEM_MAINTENANCE"),
        @JsonSubTypes.Type(value = SystemNotification.SystemUpdate.class, name = "SYSTEM_UPDATE"),
        @JsonSubTypes.Type(value = SystemNotification.SellerRegistrationComplete.class, name = "SELLER_REGISTRATION_COMPLETE"),
        // Order
        @JsonSubTypes.Type(value = OrderNotification.OrderCompleted.class, name = "ORDER_COMPLETED"),
        @JsonSubTypes.Type(value = OrderNotification.OrderCancelled.class, name = "ORDER_CANCELLED"),
        @JsonSubTypes.Type(value = OrderNotification.OrderShipped.class, name = "ORDER_SHIPPED"),
        @JsonSubTypes.Type(value = OrderNotification.OrderDelivered.class, name = "ORDER_DELIVERED")
})
public interface TemplateNotification {

    /**
     * 알림 이벤트 타입 반환
     * 
     * @return NotificationTemplateType
     */
    NotificationTemplateType getEventType();

    /**
     * 알림 데이터를 Map으로 변환
     * ObjectMapper를 사용하여 자동으로 변환합니다.
     * eventType 필드는 각 Notification 클래스에서 @JsonIgnore로 변환 전에 제외됩니다.
     * 
     * @return 변환된 Map 객체 (eventType 제외)
     */
    @SuppressWarnings("unchecked")
    default Map<String, Object> toMap() {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.convertValue(this, Map.class);
        map.remove("eventType");
        return map;
    }
}
