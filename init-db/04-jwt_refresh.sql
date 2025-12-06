use yumcoach_db;

CREATE TABLE IF NOT EXISTS refresh_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  token VARCHAR(512) NOT NULL,
  revoked TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_used_at DATETIME NULL,
  expires_at DATETIME NOT NULL,
  UNIQUE KEY uk_token (token),
  INDEX ix_user (user_id),
  CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);
