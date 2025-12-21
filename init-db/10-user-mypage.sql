-- Add gender and age to user
ALTER TABLE `user`
ADD COLUMN `gender` VARCHAR(16) DEFAULT NULL,
ADD COLUMN `age` INT DEFAULT NULL;

-- Add nickname to user
ALTER TABLE `user`
ADD COLUMN `nickname` VARCHAR(64) DEFAULT NULL;

-- Add activity_level to user_health (INT enum: 0..3)
ALTER TABLE `user_health`
ADD COLUMN `activity_level` INT DEFAULT 0;

-- Create user_diet_restriction table
CREATE TABLE IF NOT EXISTS `user_diet_restriction` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `restriction_type` VARCHAR(64) NOT NULL,
  `restriction_value` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX (`user_id`),
  CONSTRAINT `fk_udr_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
