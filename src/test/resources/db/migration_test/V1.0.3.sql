-- ====================================
-- 알림 시스템 테이블 수정 (H2 테스트 호환용)
-- ====================================

-- 1. notification_category 컬럼 추가
ALTER TABLE `notification_category`
ADD COLUMN `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. notification -> notification_template 이름 변경
ALTER TABLE `notification` RENAME TO `notification_template`;

-- 3. notification_template 컬럼명 및 타입 수정
ALTER TABLE `notification_template`
ALTER COLUMN `notification_id`
RENAME TO `notification_template_id`;

ALTER TABLE `notification_template`
ADD COLUMN `template_type` VARCHAR(50) NOT NULL;

ALTER TABLE `notification_template`
ADD COLUMN `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 4. user_notification 외래키 컬럼명 변경
ALTER TABLE `user_notification`
ALTER COLUMN `notification_id`
RENAME TO `notification_template_id`;

-- 5. 기초 데이터 (SYSTEM 카테고리 등)
INSERT INTO notification_category (category_name) VALUES ('SYSTEM');