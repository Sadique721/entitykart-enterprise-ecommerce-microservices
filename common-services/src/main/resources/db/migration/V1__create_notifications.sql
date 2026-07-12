CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(5000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(255),
    sent_at DATETIME,
    created_at DATETIME
);
