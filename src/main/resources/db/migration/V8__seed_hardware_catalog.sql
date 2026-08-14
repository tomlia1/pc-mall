-- 演示硬件目录：价格仅用于商城原型与装机单总价计算，技术规格为后续兼容性检查的数据库事实来源。
INSERT INTO products (name, brand, category, product_type, price, cpu, gpu, memory_gb, storage_gb, tags, cover_image, status) VALUES
('Ryzen 5 7600 处理器', 'AMD', 'DIY 装机', 'CPU', 1299.00, NULL, NULL, NULL, NULL, '6 核 12 线程，AM5 平台', '/assets/images/hardware-cpu.svg', 1),
('Core i5-14400F 处理器', 'Intel', 'DIY 装机', 'CPU', 1399.00, NULL, NULL, NULL, NULL, '10 核 16 线程，LGA1700 平台', '/assets/images/hardware-cpu.svg', 1),
('B650M-A WIFI II 主板', 'ASUS', 'DIY 装机', 'MOTHERBOARD', 1099.00, NULL, NULL, NULL, NULL, 'AM5，mATX，DDR5，M.2 NVMe', '/assets/images/hardware-motherboard.svg', 1),
('PRO B760M-P DDR4 主板', 'MSI', 'DIY 装机', 'MOTHERBOARD', 799.00, NULL, NULL, NULL, NULL, 'LGA1700，mATX，DDR4，M.2 NVMe', '/assets/images/hardware-motherboard.svg', 1),
('Dual RTX 4060 8GB 显卡', 'ASUS', 'DIY 装机', 'GPU', 2399.00, NULL, NULL, NULL, NULL, '8GB GDDR6，双风扇', '/assets/images/hardware-gpu.svg', 1),
('RTX 4060 WINDFORCE OC 8G 显卡', 'GIGABYTE', 'DIY 装机', 'GPU', 2499.00, NULL, NULL, NULL, NULL, '8GB GDDR6，三风扇', '/assets/images/hardware-gpu.svg', 1),
('FURY Beast DDR5 32GB 内存套装', 'Kingston', 'DIY 装机', 'RAM', 699.00, NULL, NULL, NULL, NULL, '16GB × 2，DDR5-6000', '/assets/images/hardware-ram.svg', 1),
('Vengeance LPX DDR4 32GB 内存套装', 'Corsair', 'DIY 装机', 'RAM', 499.00, NULL, NULL, NULL, NULL, '16GB × 2，DDR4-3200', '/assets/images/hardware-ram.svg', 1),
('990 EVO 1TB 固态硬盘', 'Samsung', 'DIY 装机', 'SSD', 599.00, NULL, NULL, NULL, NULL, '1TB，M.2 NVMe PCIe 4.0', '/assets/images/hardware-ssd.svg', 1),
('Blue SN580 1TB 固态硬盘', 'WD', 'DIY 装机', 'SSD', 499.00, NULL, NULL, NULL, NULL, '1TB，M.2 NVMe PCIe 4.0', '/assets/images/hardware-ssd.svg', 1),
('CX650 650W 电源', 'Corsair', 'DIY 装机', 'PSU', 549.00, NULL, NULL, NULL, NULL, '650W，80 PLUS Bronze', '/assets/images/hardware-psu.svg', 1),
('MWE Gold 750 V2 电源', 'Cooler Master', 'DIY 装机', 'PSU', 699.00, NULL, NULL, NULL, NULL, '750W，80 PLUS Gold', '/assets/images/hardware-psu.svg', 1),
('AIR 100 ARGB 机箱', 'Montech', 'DIY 装机', 'CASE', 399.00, NULL, NULL, NULL, NULL, 'mATX，显卡限长 330mm', '/assets/images/hardware-case.svg', 1),
('H5 Flow 机箱', 'NZXT', 'DIY 装机', 'CASE', 599.00, NULL, NULL, NULL, NULL, 'ATX，显卡限长 365mm', '/assets/images/hardware-case.svg', 1);

INSERT INTO product_hardware_specs (product_id, cpu_socket, memory_type, form_factor, supported_form_factors, gpu_length_mm, case_gpu_max_length_mm, storage_interface, supported_storage_interfaces, psu_watt, estimated_power_w)
SELECT id, 'AM5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 88 FROM products WHERE name = 'Ryzen 5 7600 处理器'
UNION ALL SELECT id, 'LGA1700', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 148 FROM products WHERE name = 'Core i5-14400F 处理器'
UNION ALL SELECT id, 'AM5', 'DDR5', 'mATX', NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL FROM products WHERE name = 'B650M-A WIFI II 主板'
UNION ALL SELECT id, 'LGA1700', 'DDR4', 'mATX', NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL FROM products WHERE name = 'PRO B760M-P DDR4 主板'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 227, NULL, NULL, NULL, NULL, 115 FROM products WHERE name = 'Dual RTX 4060 8GB 显卡'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 261, NULL, NULL, NULL, NULL, 115 FROM products WHERE name = 'RTX 4060 WINDFORCE OC 8G 显卡'
UNION ALL SELECT id, NULL, 'DDR5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'FURY Beast DDR5 32GB 内存套装'
UNION ALL SELECT id, NULL, 'DDR4', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'Vengeance LPX DDR4 32GB 内存套装'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL, NULL FROM products WHERE name = '990 EVO 1TB 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL, NULL FROM products WHERE name = 'Blue SN580 1TB 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 650, NULL FROM products WHERE name = 'CX650 650W 电源'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 750, NULL FROM products WHERE name = 'MWE Gold 750 V2 电源'
UNION ALL SELECT id, NULL, NULL, NULL, 'mATX,Mini-ITX', NULL, 330, NULL, NULL, NULL, NULL FROM products WHERE name = 'AIR 100 ARGB 机箱'
UNION ALL SELECT id, NULL, NULL, NULL, 'ATX,mATX,Mini-ITX', NULL, 365, NULL, NULL, NULL, NULL FROM products WHERE name = 'H5 Flow 机箱';
