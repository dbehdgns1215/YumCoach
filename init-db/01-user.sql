-- YumCoach Database Schema

CREATE DATABASE IF NOT EXISTS yumcoach_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE yumcoach_db;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주의: 다른 테이블들은 별도 SQL 파일에서 생성됩니다
-- 03-meal_tables.sql: meal_history, meal
-- 04-user_health.sql: user_health
-- 05-jwt_refresh.sql: jwt_refresh
-- 06-food_type.sql: food_type
-- 07-food_items.sql: food_items
-- 08-nutrition_facts_primary.sql: nutrition_facts_primary
-- 09-nutrition_facts.sql: nutrition_facts

