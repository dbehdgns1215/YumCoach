-- 카카오 로그인 지원을 위한 user 테이블 확장
-- 실행 전 주의: 기존 데이터가 있는 경우 백업 필수

USE yumcoach_db;

-- kakao_id 컬럼 추가 (NULL 허용, UNIQUE 제약)
ALTER TABLE user
    ADD COLUMN kakao_id BIGINT UNIQUE AFTER id;

-- password 컬럼을 NULL 허용으로 변경 (소셜 로그인 사용자용)
ALTER TABLE user
    MODIFY password VARCHAR(255) NULL;

-- nickname 컬럼 추가 (이미 존재하면 이 라인만 건너뛰어도 됨)
-- ALTER TABLE user
--     ADD COLUMN nickname VARCHAR(100) AFTER name;

-- 인덱스 추가 (카카오 ID 조회 성능 향상)
CREATE INDEX idx_kakao_id ON user(kakao_id);

-- 확인용 쿼리
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_KEY,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'yumcoach_db' 
  AND TABLE_NAME = 'user'
ORDER BY ORDINAL_POSITION;
