CREATE TABLE meal_todos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,

    meal_type VARCHAR(20) NOT NULL,
    food_code VARCHAR(50) NOT NULL,
    food_name VARCHAR(255) NOT NULL,
    default_grams INT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    INDEX idx_meal_todos_user_id (user_id),
    INDEX idx_meal_todos_user_meal (user_id, meal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
