-- V2__add_wishlist_indexes.sql (wishlist_service)
CREATE INDEX idx_wishlist_customer ON wishlist_items (customer_id);
CREATE INDEX idx_wishlist_product  ON wishlist_items (product_id);
