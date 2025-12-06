package com.homesweet.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 템플릿 타입
 * 
 * 📚 사용 가이드:
 * 1. 각 알림 타입별로 필요한 contextData가 명시되어 있습니다.
 * 2. 📋 표시된 필드들은 필수 데이터입니다.
 * 3. 💡 표시된 예시를 참고하여 사용하세요.
 * 4. 모든 데이터는 String 타입으로 전달해주세요.
 * 
 * 🔧 사용 방법:
 * ```java
 * Map<String, Object> context = Map.of(
 * "userName", "홍길동",
 * "orderId", "12345",
 * "productName", "아이폰",
 * "totalAmount", "1000000"
 * );
 * 
 * NotificationEvent event = new NotificationEvent(
 * NotificationTemplateType.ORDER_COMPLETED,
 * userId,
 * context,
 * null, null, null // 커스텀 알림이 아닌 경우 null
 * );
 * ```
 * 
 * @author dogyungkim
 */
@Getter
@RequiredArgsConstructor
public enum NotificationTemplateType {
    // ==================== 주문 관련 ====================
    /**
     * 주문 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 주문이 완료되었습니다. (주문번호: {orderId})"
     * 
     * 💡 사용 예시:
     * Map.of("userName", "홍길동", "orderId", "12345")
     */
    ORDER_COMPLETED("주문 완료", NotificationCategoryType.ORDER),

    /**
     * 주문 취소 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 주문이 취소되었습니다. (주문번호: {orderId})"
     */
    ORDER_CANCELLED("주문 취소", NotificationCategoryType.ORDER),

    /**
     * 배송 시작 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 주문이 배송을 시작했습니다. (주문번호: {orderId})"
     */
    ORDER_SHIPPED("배송 시작", NotificationCategoryType.ORDER),

    /**
     * 배송 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 주문이 배송 완료되었습니다. (주문번호: {orderId})"
     */
    ORDER_DELIVERED("배송 완료", NotificationCategoryType.ORDER),

    // ==================== 결제 관련 ====================
    /**
     * 결제 성공 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - amount: String - 결제 금액
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 결제가 성공적으로 완료되었습니다. (금액: {amount}원)"
     */
    PAYMENT_SUCCESS("결제 성공", NotificationCategoryType.PAYMENT),

    /**
     * 결제 실패 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - orderId: String - 주문 ID
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 결제가 실패했습니다. (주문번호: {orderId})"
     */
    PAYMENT_FAILED("결제 실패", NotificationCategoryType.PAYMENT),

    /**
     * 환불 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - amount: String - 환불 금액
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 환불이 완료되었습니다. (금액: {amount}원)"
     */
    PAYMENT_REFUNDED("환불 완료", NotificationCategoryType.PAYMENT),

    // ==================== 커뮤니티 관련 ====================
    /**
     * 새 댓글 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 댓글 작성자 이름
     * - postId: String - 게시글 ID
     * - postTitle: String - 게시글 제목
     * 
     * 📝 Content 템플릿:
     * "{userName}님이 {postTitle}에 댓글을 남겼습니다."
     */
    NEW_COMMENT("새 댓글", NotificationCategoryType.COMMUNITY),

    /**
     * 새 좋아요 알림 (게시글)
     * 
     * 📋 필요한 contextData:
     * - userName: String - 좋아요 누른 사용자 이름
     * - postId: String - 게시글 ID
     * - postTitle: String - 게시글 제목
     * 
     * 📝 Content 템플릿:
     * "{userName}님이 {postTitle}에 좋아요를 눌렀습니다."
     */
    NEW_LIKE("새 좋아요", NotificationCategoryType.COMMUNITY),

    /**
     * 새 댓글 좋아요 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 좋아요 누른 사용자 이름
     * - postId: String - 게시글 ID
     * - postTitle: String - 게시글 제목
     * - commentId: String - 댓글 ID
     * 
     * 📝 Content 템플릿:
     * "{userName}님이 댓글에 좋아요를 눌렀습니다."
     */
    NEW_COMMENT_LIKE("새 댓글 좋아요", NotificationCategoryType.COMMUNITY),

    // ==================== 정산 관련 ====================
    /**
     * 정산 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - settlementId: String - 정산 ID
     * - amount: String - 정산 금액
     * - settlementName: String - 정산 이름
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 {settlementName} 정산이 완료되었습니다. (금액: {amount}원)"
     */
    SETTLEMENT_COMPLETED("정산 완료", NotificationCategoryType.SETTLEMENT),

    /**
     * 정산 실패 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - settlementId: String - 정산 ID
     * 
     * 📝 Content 템플릿:
     * (템플릿이 아직 정의되지 않았습니다)
     */
    SETTLEMENT_FAILED("정산 실패", NotificationCategoryType.SETTLEMENT),

    // ==================== 상품 관련 ====================
    /**
     * 상품 승인 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 상품이 승인되었습니다. (상품명: {productName})"
     */
    PRODUCT_APPROVED("상품 승인", NotificationCategoryType.PRODUCT),

    /**
     * 상품 거부 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 상품이 거부되었습니다. (상품명: {productName})"
     */
    PRODUCT_REJECTED("상품 거부", NotificationCategoryType.PRODUCT),

    /**
     * 재고 부족 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     * - currentStock: String - 현재 재고 수량
     * 
     * 📝 Content 템플릿:
     * "{userName}님의 {productName} 상품 재고가 부족합니다. (현재 재고: {currentStock})"
     */
    PRODUCT_LOW_STOCK("재고 부족", NotificationCategoryType.PRODUCT),

    /**
     * 새 리뷰 등록 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * - productId: String - 상품 ID
     * - productName: String - 상품명
     * 
     * 📝 Content 템플릿:
     * "{userName}님이 {productName} 상품에 리뷰를 등록했습니다."
     */
    NEW_REVIEW("새 리뷰 등록", NotificationCategoryType.PRODUCT),

    // ==================== 채팅 관련 ====================
    /**
     * 새 메시지 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 메시지 발신자 이름
     * - roomId: String - 채팅방 ID
     * - roomName: String - 채팅방 이름
     * - message: String - 메시지 내용
     * 
     * 📝 Content 템플릿:
     * "{userName}님이 {roomName} 채팅방에서 메시지를 보냈습니다: {message}"
     */
    NEW_MESSAGE("새 메시지", NotificationCategoryType.CHAT),

    // ==================== 시스템 관련 ====================
    /**
     * 시스템 점검 알림
     * 
     * 📋 필요한 contextData:
     * - maintenanceTime: String - 점검 시간
     * 
     * 📝 Content 템플릿:
     * "시스템 점검 안내: {maintenanceTime}"
     */
    SYSTEM_MAINTENANCE("시스템 점검", NotificationCategoryType.SYSTEM),

    /**
     * 시스템 업데이트 알림
     * 
     * 📋 필요한 contextData:
     * - version: String - 업데이트 버전
     * - updateFeatures: String - 업데이트 기능 목록
     * 
     * 📝 Content 템플릿:
     * "시스템이 업데이트되었습니다. (버전: {version})"
     */
    SYSTEM_UPDATE("시스템 업데이트", NotificationCategoryType.SYSTEM),

    // ==================== 프로모션 관련 ====================
    /**
     * 프로모션 시작 알림
     * 
     * 📋 필요한 contextData:
     * - promotionName: String - 프로모션 이름
     * 
     * 📝 Content 템플릿:
     * "{promotionName} 프로모션이 시작되었습니다!"
     */
    PROMOTION_START("프로모션 시작", NotificationCategoryType.PROMOTION),

    /**
     * 프로모션 종료 알림
     * 
     * 📋 필요한 contextData:
     * - promotionName: String - 프로모션 이름
     * 
     * 📝 Content 템플릿:
     * "{promotionName} 프로모션이 종료되었습니다."
     */
    PROMOTION_END("프로모션 종료", NotificationCategoryType.PROMOTION),

    // ==================== 판매자 등록 완료 관련 ====================
    /**
     * 판매자 등록 완료 알림
     * 
     * 📋 필요한 contextData:
     * - userName: String - 사용자 이름
     * 
     * 📝 Content 템플릿:
     * "판매자 등록이 완료되었습니다."
     */
    SELLER_REGISTRATION_COMPLETE("판매자 등록 완료", NotificationCategoryType.SYSTEM),

    // ==================== 커스텀 알림 ====================
    /**
     * 커스텀 알림
     * 
     * 📋 필요한 contextData:
     * - 사용자 정의 (title, content, redirectUrl 직접 지정)
     * 
     * 💡 사용 예시:
     * NotificationEvent event = new NotificationEvent(
     * NotificationTemplateType.CUSTOM,
     * userId,
     * context,
     * "긴급 공지",
     * "시스템 점검 안내",
     * "app://maintenance"
     * );
     */
    CUSTOM("커스텀 알림", NotificationCategoryType.CUSTOM);

    private final String description;
    private final NotificationCategoryType categoryType;

    public static NotificationTemplateType fromCode(String code) {
        try {
            return NotificationTemplateType.valueOf(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown notification template type: " + code);
        }
    }
}