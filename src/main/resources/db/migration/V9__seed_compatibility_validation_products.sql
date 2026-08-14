-- 为 M3.10 基础兼容性检查补充最小验收商品；价格仅用于演示总价与规则验证。
INSERT INTO products (name, brand, category, product_type, price, cpu, gpu, memory_gb, storage_gb, tags, cover_image, status) VALUES
('RTX 4070 SUPER 三风扇显卡', 'Demo Hardware', 'DIY 装机', 'GPU', 4299.00, NULL, NULL, NULL, NULL, '12GB GDDR6X，长度 348mm', '/assets/images/hardware-gpu.svg', 1),
('Z790-P WIFI DDR5 主板', 'Demo Hardware', 'DIY 装机', 'MOTHERBOARD', 1599.00, NULL, NULL, NULL, NULL, 'LGA1700，ATX，DDR5，M.2 NVMe', '/assets/images/hardware-motherboard.svg', 1),
('SATA 1TB 固态硬盘', 'Demo Hardware', 'DIY 装机', 'SSD', 459.00, NULL, NULL, NULL, NULL, '1TB，SATA 2.5 英寸', '/assets/images/hardware-ssd.svg', 1),
('450W 铜牌电源', 'Demo Hardware', 'DIY 装机', 'PSU', 329.00, NULL, NULL, NULL, NULL, '450W，80 PLUS Bronze', '/assets/images/hardware-psu.svg', 1);

INSERT INTO product_hardware_specs (product_id, cpu_socket, memory_type, form_factor, supported_form_factors, gpu_length_mm, case_gpu_max_length_mm, storage_interface, supported_storage_interfaces, psu_watt, estimated_power_w)
SELECT id, NULL, NULL, NULL, NULL, 348, NULL, NULL, NULL, NULL, 220 FROM products WHERE name = 'RTX 4070 SUPER 三风扇显卡'
UNION ALL SELECT id, 'LGA1700', 'DDR5', 'ATX', NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL FROM products WHERE name = 'Z790-P WIFI DDR5 主板'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'SATA 2.5', NULL, NULL, NULL FROM products WHERE name = 'SATA 1TB 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 450, NULL FROM products WHERE name = '450W 铜牌电源';
