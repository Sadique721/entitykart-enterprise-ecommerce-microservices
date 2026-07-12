CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    transaction_ref VARCHAR(255),
    payment_status VARCHAR(50) NOT NULL,
    payment_date DATETIME,
    created_at DATETIME NOT NULL,
    gateway_transaction_id VARCHAR(255),
    gateway_response_code VARCHAR(255),
    gateway_response_text VARCHAR(255)
);
