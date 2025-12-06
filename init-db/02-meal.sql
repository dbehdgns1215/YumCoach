use yumcoach_db;

CREATE TABLE IF NOT EXISTS `meal_history` (
	id INT AUTO_INCREMENT PRIMARY KEY,
	user_id INT NOT NULL,
    created_date DATETIME NOT NULL,
    date DATETIME NOT NULL,
    type VARCHAR(30) NOT NULL,
	FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS `meal` (
	id INT AUTO_INCREMENT PRIMARY KEY,
    history_id INT NOT NULL,
	meal_code VARCHAR(255) NOT NULL,
	meal_name VARCHAR(255) NOT NULL,
    amount INT,
	FOREIGN KEY (history_id) REFERENCES meal_history(id) ON DELETE CASCADE
);

