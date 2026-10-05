-- Migration V2: Module Produit OptiVision Enterprise Schema

CREATE TABLE IF NOT EXISTS brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    logo VARCHAR(1000),
    website VARCHAR(255),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(1000),
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    active BOOLEAN DEFAULT TRUE,
    image VARCHAR(1000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(255) UNIQUE,
    reference VARCHAR(255),
    barcode VARCHAR(255) UNIQUE,
    name VARCHAR(255) NOT NULL,
    short_description VARCHAR(255),
    description TEXT,
    product_type VARCHAR(50),
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    sub_category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    brand_id BIGINT REFERENCES brands(id) ON DELETE SET NULL,
    legacy_category VARCHAR(255),
    legacy_brand VARCHAR(255),
    model VARCHAR(255),
    gender VARCHAR(50),
    target_age VARCHAR(50),
    color VARCHAR(255),
    material VARCHAR(255),
    collection VARCHAR(255),
    supplier VARCHAR(255),
    purchase_price NUMERIC(10, 2),
    selling_price NUMERIC(10, 2),
    tax_rate NUMERIC(5, 2) DEFAULT 20.00,
    active BOOLEAN DEFAULT TRUE,
    featured BOOLEAN DEFAULT FALSE,
    searchable BOOLEAN DEFAULT TRUE,
    frame_shape VARCHAR(50),
    frame_width INTEGER,
    lens_width INTEGER,
    bridge_width INTEGER,
    temple_length INTEGER,
    frame_height INTEGER,
    frame_material VARCHAR(255),
    frame_color VARCHAR(255),
    lens_type VARCHAR(255),
    lens_color VARCHAR(255),
    polarized BOOLEAN DEFAULT FALSE,
    photochromic BOOLEAN DEFAULT FALSE,
    uv_protection VARCHAR(255),
    virtual_try_on_enabled BOOLEAN DEFAULT TRUE,
    legacy_stock INTEGER,
    legacy_face_shape VARCHAR(255),
    legacy_frame_type VARCHAR(255),
    image_url VARCHAR(1000),
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS product_variants (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(255) NOT NULL UNIQUE,
    barcode VARCHAR(255) UNIQUE,
    color VARCHAR(255),
    size VARCHAR(255),
    purchase_price NUMERIC(10, 2),
    selling_price NUMERIC(10, 2),
    active BOOLEAN DEFAULT TRUE,
    stock INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants(id) ON DELETE CASCADE,
    url VARCHAR(1000) NOT NULL,
    alt_text VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    primary_image BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    performed_by VARCHAR(255),
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    old_value TEXT,
    new_value TEXT
);

CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_variants_sku ON product_variants(sku);
