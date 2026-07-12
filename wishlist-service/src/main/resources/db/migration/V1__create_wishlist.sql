CREATE TABLE IF NOT EXISTS wishlist_items (
    wishlist_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    added_at DATETIME NOT NULL,
    UNIQUE KEY uq_customer_product (customer_id, product_id)
);
