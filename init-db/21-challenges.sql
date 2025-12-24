-- 1. challenges 테이블 (핵심)
CREATE TABLE IF NOT EXISTS challenges (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  
  -- 기본 정보
  title VARCHAR(255) NOT NULL,
  description TEXT,
  
  -- 목표 타입 & 구체적 목표
  goal_type VARCHAR(50) NOT NULL, -- PROTEIN, CALORIE, WEIGHT, WATER, EXERCISE, HABIT, COMBINED
  goal_details JSON NOT NULL, 
  -- 예: {"protein": "200g", "frequency": "daily"}
  -- 예: {"weight": "-5kg", "duration": "30days"}
  -- 예: {"calories": "<1500", "protein": ">100g"}
  
  -- 기간
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  duration_days INT GENERATED ALWAYS AS (DATEDIFF(end_date, start_date) + 1) STORED,
  
  -- 진척도
  status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, FAILED, ABANDONED
  current_streak INT DEFAULT 0,       -- 연속 달성 일수
  max_streak INT DEFAULT 0,
  total_success_days INT DEFAULT 0,   -- 총 성공한 일수
  success_rate DECIMAL(5,2) DEFAULT 0.00, -- 성공률 (%)
  
  -- 생성 출처
  source VARCHAR(50), -- CHATBOT, REPORT, MANUAL
  source_id BIGINT,   -- chatbot_message_id or report_id
  source_data JSON,   -- 원본 AI 플랜이나 리포트 데이터
  
  -- AI 관련
  ai_generated BOOLEAN DEFAULT FALSE,
  ai_prompt TEXT,
  
  -- 메타
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  
  INDEX idx_user_status (user_id, status),
  INDEX idx_user_active (user_id, status, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. challenge_daily_logs (일일 추적)
CREATE TABLE IF NOT EXISTS challenge_daily_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  challenge_id BIGINT NOT NULL,
  log_date DATE NOT NULL,
  
  -- 목표 vs 실제
  target_value VARCHAR(100),  -- 목표: "200g"
  actual_value VARCHAR(100),  -- 실제: "185g"
  
  -- 달성 여부
  is_achieved BOOLEAN DEFAULT FALSE,
  achievement_rate DECIMAL(5,2), -- 92.5% (185/200)
  
  -- 자동 계산된 데이터 (리포트에서 가져옴)
  from_report_id BIGINT,
  report_data JSON, -- 해당 날짜 리포트의 요약 데이터
  
  -- AI 피드백
  ai_feedback TEXT, -- "목표까지 15g 부족! 저녁에 닭가슴살 추가해보세요"
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  UNIQUE KEY unique_challenge_date (challenge_id, log_date),
  INDEX idx_challenge (challenge_id),
  INDEX idx_date (log_date),
  FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. challenge_items (체크리스트, 기존 유지하되 확장)
CREATE TABLE IF NOT EXISTS challenge_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  challenge_id BIGINT NOT NULL,
  
  item_text TEXT NOT NULL,
  item_type VARCHAR(30) DEFAULT 'ACTION', -- ACTION, TIP, MILESTONE
  
  -- 날짜 연동 (선택)
  target_date DATE NULL, -- 특정 날짜 항목
  
  order_idx INT DEFAULT 0,
  done BOOLEAN DEFAULT FALSE,
  done_at DATETIME NULL,
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_challenge (challenge_id),
  INDEX idx_target_date (challenge_id, target_date),
  FOREIGN KEY (challenge_id) REFERENCES challenges(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;