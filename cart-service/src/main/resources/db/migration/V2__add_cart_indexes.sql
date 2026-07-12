-- V2__add_cart_indexes.sql (cart_service)
CREATE INDEX idx_cart_customer_id ON cart_items (customer_id);
CREATE INDEX idx_cart_product_id  ON cart_items (product_id);
