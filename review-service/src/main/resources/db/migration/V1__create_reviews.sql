CREATE TABLE IF NOT EXISTS reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(255),
    created_at DATETIME NOT NULL,
    UNIQUE KEY uq_customer_product (customer_id, product_id)
);
