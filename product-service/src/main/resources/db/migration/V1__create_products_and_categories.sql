CREATE TABLE IF NOT EXISTS categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS sub_categories (
    sub_category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    child_category VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS products (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    brand VARCHAR(255),
    price DECIMAL(38,2) NOT NULL,
    mrp DECIMAL(38,2),
    stock_quantity INT NOT NULL,
    sku VARCHAR(255),
    main_imageurl VARCHAR(255),
    category_id BIGINT,
    sub_category_id BIGINT,
    seller_id BIGINT,
    created_at DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Available'
);
