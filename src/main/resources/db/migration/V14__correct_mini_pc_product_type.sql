ALTER TABLE products DROP CHECK chk_products_product_type;

ALTER TABLE products
    ADD CONSTRAINT chk_products_product_type CHECK (product_type IN (
        'LAPTOP', 'MINI_PC', 'CPU', 'MOTHERBOARD', 'GPU', 'RAM', 'SSD', 'PSU', 'CASE'
    ));

UPDATE products
SET product_type = 'MINI_PC'
WHERE name = '零刻 SER8 迷你电脑主机';
