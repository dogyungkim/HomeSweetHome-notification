-- ====================================
-- 사용자 및 등급 테이블
-- ====================================

CREATE TABLE `grade` (
                         `grade_id` INT NOT NULL AUTO_INCREMENT,
                         `grade` VARCHAR(10) NOT NULL,
                         `fee_rate` DECIMAL(5,2) NOT NULL,
                         PRIMARY KEY (`grade_id`)
);

CREATE TABLE `users` (
                         `user_id` BIGINT NOT NULL AUTO_INCREMENT,
                         `name` VARCHAR(20) NULL,
                         `email` VARCHAR(100) NOT NULL UNIQUE,
                         `address` VARCHAR(100) NULL,
                         `birth` DATE NULL,
                         `profile_img_url` VARCHAR(255) NULL,
                         `role` VARCHAR(100) NOT NULL,
                         `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         `grade_id` INT NULL,
                         PRIMARY KEY (`user_id`),
                         FOREIGN KEY (`grade_id`) REFERENCES `grade` (`grade_id`)
);


-- ====================================
-- 알림 관련 테이블
-- ====================================

CREATE TABLE `notification_category` (
                                         `notification_category_id` INT NOT NULL AUTO_INCREMENT,
                                         `category_name` VARCHAR(50) NOT NULL,
                                         PRIMARY KEY (`notification_category_id`)
);

CREATE TABLE `notification` (
                                `notification_id` BIGINT NOT NULL AUTO_INCREMENT,
                                `notification_category_id` INT NOT NULL,
                                `title` VARCHAR(30) NOT NULL,
                                `content` VARCHAR(50) NOT NULL,
                                `redirect_url` VARCHAR(255) NOT NULL,
                                `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`notification_id`),
                                FOREIGN KEY (`notification_category_id`) REFERENCES `notification_category` (`notification_category_id`)
);

CREATE TABLE `user_notification` (
                                     `user_notification_id` BIGINT NOT NULL AUTO_INCREMENT,
                                     `user_id` BIGINT NOT NULL,
                                     `notification_id` BIGINT NOT NULL,
                                     `context_data` JSON NOT NULL,
                                     `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
                                     `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE,
                                     `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`user_notification_id`),
                                     FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
                                     FOREIGN KEY (`notification_id`) REFERENCES `notification` (`notification_id`) ON DELETE CASCADE
);