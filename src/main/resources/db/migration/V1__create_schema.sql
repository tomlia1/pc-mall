CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    brand VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    cpu VARCHAR(128) NOT NULL,
    gpu VARCHAR(128) DEFAULT NULL,
    memory_gb INT NOT NULL,
    storage_gb INT NOT NULL,
    tags VARCHAR(500) DEFAULT NULL,
    cover_image VARCHAR(500) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_products_search (status, category, price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

CREATE TABLE admin_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_ip VARCHAR(45) DEFAULT NULL,
    last_login_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员';

CREATE TABLE leads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    contact_type VARCHAR(16) NOT NULL,
    contact_value VARCHAR(128) NOT NULL,
    privacy_agreed TINYINT NOT NULL DEFAULT 0,
    agreed_at DATETIME DEFAULT NULL,
    user_budget DECIMAL(10,2) DEFAULT NULL,
    user_remark VARCHAR(500) DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_leads_product (product_id),
    CONSTRAINT fk_leads_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留资';

CREATE TABLE sys_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    module VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    ip VARCHAR(45) NOT NULL,
    detail TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operation_admin_module (admin_id, module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作审计日志';
