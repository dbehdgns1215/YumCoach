-- 게시글 테이블
CREATE TABLE IF NOT EXISTS `post` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL COMMENT '게시글 제목',
    `content` TEXT NOT NULL COMMENT '게시글 내용',
    `category` VARCHAR(50) DEFAULT '경험' COMMENT '카테고리: 경험, 식단, 팁',
    `is_notice` TINYINT(1) DEFAULT 0 COMMENT '공지사항 여부',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '삭제 여부',
    `user_id` INT NOT NULL COMMENT '작성자 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일시',
    `updated_at` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '삭제일시',
    `deleted_by` INT NULL DEFAULT NULL COMMENT '삭제자 ID',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at` DESC),
    INDEX `idx_is_deleted` (`is_deleted`),
    INDEX `idx_category` (`category`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글 테이블';

-- 댓글 테이블
CREATE TABLE IF NOT EXISTS `comment` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `post_id` INT NOT NULL COMMENT '게시글 ID',
    `user_id` INT NOT NULL COMMENT '작성자 ID',
    `content` TEXT NOT NULL COMMENT '댓글 내용',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '작성일시',
    `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '삭제 여부',
    `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '삭제일시',
    PRIMARY KEY (`id`),
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at` ASC),
    INDEX `idx_is_deleted` (`is_deleted`),
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='댓글 테이블';