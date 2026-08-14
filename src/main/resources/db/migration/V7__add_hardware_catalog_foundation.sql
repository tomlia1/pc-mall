ALTER TABLE products
    ADD COLUMN product_type VARCHAR(32) NOT NULL DEFAULT 'LAPTOP' AFTER category,
    MODIFY COLUMN cpu VARCHAR(128) NULL,
    MODIFY COLUMN memory_gb INT NULL,
    MODIFY COLUMN storage_gb INT NULL,
    ADD KEY idx_products_type_status_price (product_type, status, price),
    ADD CONSTRAINT chk_products_product_type CHECK (product_type IN (
        'LAPTOP', 'CPU', 'MOTHERBOARD', 'GPU', 'RAM', 'SSD', 'PSU', 'CASE'
    ));

CREATE TABLE product_hardware_specs (
    product_id BIGINT NOT NULL,
    cpu_socket VARCHAR(32) DEFAULT NULL,
    memory_type VARCHAR(32) DEFAULT NULL,
    form_factor VARCHAR(32) DEFAULT NULL,
    supported_form_factors VARCHAR(128) DEFAULT NULL,
    gpu_length_mm INT DEFAULT NULL,
    case_gpu_max_length_mm INT DEFAULT NULL,
    storage_interface VARCHAR(64) DEFAULT NULL,
    supported_storage_interfaces VARCHAR(128) DEFAULT NULL,
    psu_watt INT DEFAULT NULL,
    estimated_power_w INT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    CONSTRAINT fk_product_hardware_specs_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DIY 装机商品规格';
