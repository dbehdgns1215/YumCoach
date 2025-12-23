-- Create tables for Report feature
-- report: daily/weekly report summary
-- report_meal: meals included in a report (summary)
-- report_insight: short insight rows (good/warn/keep)
-- report_generation_log: log of every generation attempt (user/system)

CREATE TABLE IF NOT EXISTS `report` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `type` ENUM('DAILY','WEEKLY') NOT NULL,
  `date` DATE DEFAULT NULL,
  `from_date` DATE DEFAULT NULL,
  `to_date` DATE DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PROGRESS',
  `source_hash` VARCHAR(128) DEFAULT NULL,
  `score` INT DEFAULT NULL,
  `total_calories` INT DEFAULT NULL,
  `protein_g` INT DEFAULT NULL,
  `carb_g` INT DEFAULT NULL,
  `fat_g` INT DEFAULT NULL,
  `meal_count` INT DEFAULT NULL,
  `ai_prompt` TEXT DEFAULT NULL,
  `ai_response` JSON DEFAULT NULL,
  `created_by` VARCHAR(16) NOT NULL DEFAULT 'USER',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_report_user_type_date` (`user_id`,`type`,`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- report_meal: link between report and individual meals (summary of meal nutrients)
CREATE TABLE IF NOT EXISTS `report_meal` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `report_id` INT NOT NULL,
  `meal_id` INT DEFAULT NULL,
  `meal_time` DATETIME DEFAULT NULL,
  `calories` INT DEFAULT NULL,
  `protein_g` INT DEFAULT NULL,
  `carb_g` INT DEFAULT NULL,
  `fat_g` INT DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX (`report_id`),
  CONSTRAINT `fk_report_meal_report` FOREIGN KEY (`report_id`) REFERENCES `report`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- report_insight: structured short messages for UI
CREATE TABLE IF NOT EXISTS `report_insight` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `report_id` INT NOT NULL,
  `kind` ENUM('good','warn','keep') NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `body` TEXT,
  PRIMARY KEY (`id`),
  INDEX (`report_id`),
  CONSTRAINT `fk_report_insight_report` FOREIGN KEY (`report_id`) REFERENCES `report`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- report_generation_log: record every generation attempt for analytics and limit checks
CREATE TABLE IF NOT EXISTS `report_generation_log` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `type` ENUM('DAILY','WEEKLY') NOT NULL,
  `date` DATE DEFAULT NULL,
  `from_date` DATE DEFAULT NULL,
  `to_date` DATE DEFAULT NULL,
  `triggered_by` ENUM('USER','SYSTEM') NOT NULL,
  `trigger_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 결과 코드: 서비스에서 사용되는 값들을 모두 포함
  -- 예: CREATED_WITH_AI, CREATED_NO_AI, NO_DATA 등
  `result` ENUM('CREATED','FAILED','LIMIT_EXCEEDED','CREATED_WITH_AI','CREATED_NO_AI','NO_DATA') NOT NULL,
  `report_id` INT DEFAULT NULL,
  `details` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_genlog_user_time` (`user_id`,`trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- report_insight 테이블 수정: kind에 'coach', 'action' 추가
ALTER TABLE `report_insight` 
MODIFY COLUMN `kind` ENUM('good','warn','keep','coach','action') NOT NULL;

-- Optional: foreign key from report.user_id to user table if exists
-- Uncomment if `user` table exists and you want FK enforcement
-- ALTER TABLE `report` ADD CONSTRAINT `fk_report_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;

-- Notes:
-- - Reports created by users should use created_by='USER' and status='IN_PROGRESS'
-- - System (batch) created reports should use created_by='SYSTEM' and status='COMPLETED'
-- - The system will always run; IN_PROGRESS records are preserved and not overwritten by the batch.
