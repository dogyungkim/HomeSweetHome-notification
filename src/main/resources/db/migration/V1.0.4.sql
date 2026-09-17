-- ====================================
-- 단체 인앱 알림 (Broadcast Notification) 최소 설계 테이블 생성 및 기존 테이블 수정
-- ====================================

-- 1. users 테이블 created_at 정밀도 변경
ALTER TABLE `users`
    MODIFY `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- 2. user_notification 피드 조회용 복합 인덱스 추가
ALTER TABLE `user_notification`
    ADD KEY `idx_user_notification_feed`
        (`user_id`, `is_deleted`, `created_at` DESC, `user_notification_id` DESC);

-- 3. broadcast_notification 테이블 생성
CREATE TABLE `broadcast_notification` (
    `broadcast_notification_id` BIGINT NOT NULL AUTO_INCREMENT,
    `notification_template_id` BIGINT NULL,
    `audience_type` VARCHAR(20) NOT NULL,
    `target_min_user_id` BIGINT NULL,
    `target_max_user_id` BIGINT NULL,
    `context_data` JSON NOT NULL,
    `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`broadcast_notification_id`),
    KEY `idx_broadcast_notification_feed`
        (`created_at` DESC, `broadcast_notification_id` DESC),
    CONSTRAINT `fk_broadcast_notification_template`
        FOREIGN KEY (`notification_template_id`)
        REFERENCES `notification_template` (`notification_template_id`)
        ON DELETE RESTRICT,
    CONSTRAINT `chk_broadcast_notification_audience`
        CHECK (
            (
                `audience_type` = 'ALL'
                AND `target_min_user_id` IS NULL
                AND `target_max_user_id` IS NULL
            )
            OR
            (
                `audience_type` = 'USER_ID_RANGE'
                AND `target_min_user_id` >= 1
                AND `target_max_user_id` >= `target_min_user_id`
            )
        )
);

-- 4. user_broadcast_state 테이블 생성
CREATE TABLE `user_broadcast_state` (
    `user_id` BIGINT NOT NULL,
    `broadcast_notification_id` BIGINT NOT NULL,
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (`user_id`, `broadcast_notification_id`),
    KEY `idx_user_broadcast_state_notification`
        (`broadcast_notification_id`, `user_id`),
    CONSTRAINT `fk_user_broadcast_state_user`
        FOREIGN KEY (`user_id`)
        REFERENCES `users` (`user_id`)
        ON DELETE CASCADE,
    CONSTRAINT `fk_user_broadcast_state_notification`
        FOREIGN KEY (`broadcast_notification_id`)
        REFERENCES `broadcast_notification` (`broadcast_notification_id`)
        ON DELETE CASCADE
);
