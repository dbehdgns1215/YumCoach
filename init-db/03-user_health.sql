-- schema.sql
use yumcoach_db;

CREATE TABLE user_health (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    height INT DEFAULT 0,
    weight INT DEFAULT 0,
    diabetes BOOLEAN DEFAULT FALSE,
    high_blood_pressure BOOLEAN DEFAULT FALSE,
    hyperlipidemia BOOLEAN DEFAULT FALSE,
    kidney_disease BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

