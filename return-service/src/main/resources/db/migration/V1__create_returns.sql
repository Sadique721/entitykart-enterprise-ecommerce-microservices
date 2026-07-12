CREATE TABLE IF NOT EXISTS returns (
    return_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(50) NOT NULL,
    refund_amount DOUBLE,
    admin_note VARCHAR(255),
    rejection_reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
