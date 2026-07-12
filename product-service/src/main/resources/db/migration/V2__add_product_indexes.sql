CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_sub_category_id ON products(sub_category_id);
CREATE INDEX idx_products_seller_id ON products(seller_id);
CREATE INDEX idx_sub_categories_category_id ON sub_categories(category_id);
