-- 基于公开厂商规格的真实型号目录；价格仅用于本项目 Demo 的预算和兼容性演示，不是实时售价或购买报价。
-- 覆盖七个装机槽位的入门、中端和高端选择，并为每个散件提供基础兼容性检查所需事实数据。

INSERT INTO products (name, brand, category, product_type, price, cpu, gpu, memory_gb, storage_gb, tags, cover_image, status) VALUES
('AMD Ryzen 5 7500F 处理器', 'AMD', 'DIY 装机', 'CPU', 899.00, NULL, NULL, NULL, NULL, '6核12线程，AM5，65W，演示参考价', '/assets/images/hardware-cpu.svg', 1),
('AMD Ryzen 7 7700 处理器', 'AMD', 'DIY 装机', 'CPU', 1699.00, NULL, NULL, NULL, NULL, '8核16线程，AM5，65W，演示参考价', '/assets/images/hardware-cpu.svg', 1),
('AMD Ryzen 7 7800X3D 处理器', 'AMD', 'DIY 装机', 'CPU', 2899.00, NULL, NULL, NULL, NULL, '8核16线程，AM5，120W，演示参考价', '/assets/images/hardware-cpu.svg', 1),
('AMD Ryzen 5 9600X 处理器', 'AMD', 'DIY 装机', 'CPU', 1999.00, NULL, NULL, NULL, NULL, '6核12线程，AM5，65W，演示参考价', '/assets/images/hardware-cpu.svg', 1),
('Intel Core i5-13400F 处理器', 'Intel', 'DIY 装机', 'CPU', 1149.00, NULL, NULL, NULL, NULL, '10核16线程，LGA1700，148W，演示参考价', '/assets/images/hardware-cpu.svg', 1),
('Intel Core i5-14600KF 处理器', 'Intel', 'DIY 装机', 'CPU', 1849.00, NULL, NULL, NULL, NULL, '14核20线程，LGA1700，181W，演示参考价', '/assets/images/hardware-cpu.svg', 1),
('Intel Core i7-14700KF 处理器', 'Intel', 'DIY 装机', 'CPU', 2699.00, NULL, NULL, NULL, NULL, '20核28线程，LGA1700，253W，演示参考价', '/assets/images/hardware-cpu.svg', 1),

('GIGABYTE B650M DS3H 主板', 'GIGABYTE', 'DIY 装机', 'MOTHERBOARD', 899.00, NULL, NULL, NULL, NULL, 'AM5，mATX，DDR5，M.2 NVMe，SATA，演示参考价', '/assets/images/hardware-motherboard.svg', 1),
('MSI MAG B650 TOMAHAWK WIFI 主板', 'MSI', 'DIY 装机', 'MOTHERBOARD', 1499.00, NULL, NULL, NULL, NULL, 'AM5，ATX，DDR5，M.2 NVMe，SATA，演示参考价', '/assets/images/hardware-motherboard.svg', 1),
('ASUS TUF GAMING B650M-PLUS WIFI II 主板', 'ASUS', 'DIY 装机', 'MOTHERBOARD', 1299.00, NULL, NULL, NULL, NULL, 'AM5，mATX，DDR5，M.2 NVMe，SATA，演示参考价', '/assets/images/hardware-motherboard.svg', 1),
('ASRock B760M Pro RS/D4 主板', 'ASRock', 'DIY 装机', 'MOTHERBOARD', 799.00, NULL, NULL, NULL, NULL, 'LGA1700，mATX，DDR4，M.2 NVMe，SATA，演示参考价', '/assets/images/hardware-motherboard.svg', 1),
('ASUS TUF GAMING B760M-PLUS WIFI II 主板', 'ASUS', 'DIY 装机', 'MOTHERBOARD', 1399.00, NULL, NULL, NULL, NULL, 'LGA1700，mATX，DDR5，M.2 NVMe，SATA，演示参考价', '/assets/images/hardware-motherboard.svg', 1),
('GIGABYTE Z790 UD AX 主板', 'GIGABYTE', 'DIY 装机', 'MOTHERBOARD', 1699.00, NULL, NULL, NULL, NULL, 'LGA1700，ATX，DDR5，M.2 NVMe，SATA，演示参考价', '/assets/images/hardware-motherboard.svg', 1),
('MSI PRO B760M-A WIFI DDR4 主板', 'MSI', 'DIY 装机', 'MOTHERBOARD', 949.00, NULL, NULL, NULL, NULL, 'LGA1700，mATX，DDR4，M.2 NVMe，SATA，演示参考价', '/assets/images/hardware-motherboard.svg', 1),

('SAPPHIRE PULSE Radeon RX 7600 8GB 显卡', 'SAPPHIRE', 'DIY 装机', 'GPU', 1999.00, NULL, NULL, NULL, NULL, '8GB GDDR6，长度240mm，演示参考价', '/assets/images/hardware-gpu.svg', 1),
('SAPPHIRE PULSE Radeon RX 7700 XT 12GB 显卡', 'SAPPHIRE', 'DIY 装机', 'GPU', 3199.00, NULL, NULL, NULL, NULL, '12GB GDDR6，长度280mm，演示参考价', '/assets/images/hardware-gpu.svg', 1),
('SAPPHIRE PULSE Radeon RX 7800 XT 16GB 显卡', 'SAPPHIRE', 'DIY 装机', 'GPU', 3899.00, NULL, NULL, NULL, NULL, '16GB GDDR6，长度320mm，演示参考价', '/assets/images/hardware-gpu.svg', 1),
('ASUS Dual GeForce RTX 4060 Ti 8GB 显卡', 'ASUS', 'DIY 装机', 'GPU', 2999.00, NULL, NULL, NULL, NULL, '8GB GDDR6，长度227mm，演示参考价', '/assets/images/hardware-gpu.svg', 1),
('MSI GeForce RTX 4070 SUPER VENTUS 2X OC 显卡', 'MSI', 'DIY 装机', 'GPU', 4799.00, NULL, NULL, NULL, NULL, '12GB GDDR6X，长度242mm，演示参考价', '/assets/images/hardware-gpu.svg', 1),
('GIGABYTE RTX 4070 Ti SUPER WINDFORCE OC 16G 显卡', 'GIGABYTE', 'DIY 装机', 'GPU', 6599.00, NULL, NULL, NULL, NULL, '16GB GDDR6X，长度261mm，演示参考价', '/assets/images/hardware-gpu.svg', 1),
('ASUS Dual GeForce RTX 3050 6GB 显卡', 'ASUS', 'DIY 装机', 'GPU', 1399.00, NULL, NULL, NULL, NULL, '6GB GDDR6，长度200mm，演示参考价', '/assets/images/hardware-gpu.svg', 1),

('Kingston FURY Beast DDR4 16GB 内存套装', 'Kingston', 'DIY 装机', 'RAM', 279.00, NULL, NULL, NULL, NULL, '8GB×2，DDR4-3200，演示参考价', '/assets/images/hardware-ram.svg', 1),
('Corsair Vengeance LPX DDR4 16GB 内存套装', 'Corsair', 'DIY 装机', 'RAM', 299.00, NULL, NULL, NULL, NULL, '8GB×2，DDR4-3200，演示参考价', '/assets/images/hardware-ram.svg', 1),
('G.SKILL Ripjaws V DDR4 32GB 内存套装', 'G.SKILL', 'DIY 装机', 'RAM', 519.00, NULL, NULL, NULL, NULL, '16GB×2，DDR4-3600，演示参考价', '/assets/images/hardware-ram.svg', 1),
('Kingston FURY Beast DDR5 32GB 内存套装', 'Kingston', 'DIY 装机', 'RAM', 699.00, NULL, NULL, NULL, NULL, '16GB×2，DDR5-6000，演示参考价', '/assets/images/hardware-ram.svg', 1),
('Corsair Vengeance DDR5 32GB 内存套装', 'Corsair', 'DIY 装机', 'RAM', 759.00, NULL, NULL, NULL, NULL, '16GB×2，DDR5-6000，演示参考价', '/assets/images/hardware-ram.svg', 1),
('ADATA XPG Lancer Blade DDR5 32GB 内存套装', 'ADATA', 'DIY 装机', 'RAM', 699.00, NULL, NULL, NULL, NULL, '16GB×2，DDR5-6000，演示参考价', '/assets/images/hardware-ram.svg', 1),
('TEAMGROUP T-CREATE CLASSIC DDR5 64GB 内存套装', 'TEAMGROUP', 'DIY 装机', 'RAM', 1299.00, NULL, NULL, NULL, NULL, '32GB×2，DDR5-6000，演示参考价', '/assets/images/hardware-ram.svg', 1),

('Kingston NV2 1TB 固态硬盘', 'Kingston', 'DIY 装机', 'SSD', 399.00, NULL, NULL, NULL, NULL, '1TB，M.2 NVMe PCIe 4.0，演示参考价', '/assets/images/hardware-ssd.svg', 1),
('Lexar NM790 1TB 固态硬盘', 'Lexar', 'DIY 装机', 'SSD', 499.00, NULL, NULL, NULL, NULL, '1TB，M.2 NVMe PCIe 4.0，演示参考价', '/assets/images/hardware-ssd.svg', 1),
('Crucial P3 Plus 1TB 固态硬盘', 'Crucial', 'DIY 装机', 'SSD', 479.00, NULL, NULL, NULL, NULL, '1TB，M.2 NVMe PCIe 4.0，演示参考价', '/assets/images/hardware-ssd.svg', 1),
('WD_BLACK SN770 1TB 固态硬盘', 'WD', 'DIY 装机', 'SSD', 549.00, NULL, NULL, NULL, NULL, '1TB，M.2 NVMe PCIe 4.0，演示参考价', '/assets/images/hardware-ssd.svg', 1),
('Samsung 990 PRO 2TB 固态硬盘', 'Samsung', 'DIY 装机', 'SSD', 1099.00, NULL, NULL, NULL, NULL, '2TB，M.2 NVMe PCIe 4.0，演示参考价', '/assets/images/hardware-ssd.svg', 1),
('Crucial BX500 1TB SATA 固态硬盘', 'Crucial', 'DIY 装机', 'SSD', 429.00, NULL, NULL, NULL, NULL, '1TB，SATA 2.5英寸，演示参考价', '/assets/images/hardware-ssd.svg', 1),
('Samsung 870 EVO 1TB SATA 固态硬盘', 'Samsung', 'DIY 装机', 'SSD', 699.00, NULL, NULL, NULL, NULL, '1TB，SATA 2.5英寸，演示参考价', '/assets/images/hardware-ssd.svg', 1),

('MSI MAG A550BN 550W 电源', 'MSI', 'DIY 装机', 'PSU', 349.00, NULL, NULL, NULL, NULL, '550W，80 PLUS Bronze，演示参考价', '/assets/images/hardware-psu.svg', 1),
('Corsair CX550 550W 电源', 'Corsair', 'DIY 装机', 'PSU', 449.00, NULL, NULL, NULL, NULL, '550W，80 PLUS Bronze，演示参考价', '/assets/images/hardware-psu.svg', 1),
('Thermaltake Smart BX1 650W 电源', 'Thermaltake', 'DIY 装机', 'PSU', 499.00, NULL, NULL, NULL, NULL, '650W，80 PLUS Bronze，演示参考价', '/assets/images/hardware-psu.svg', 1),
('Cooler Master MWE Gold 650 V2 电源', 'Cooler Master', 'DIY 装机', 'PSU', 649.00, NULL, NULL, NULL, NULL, '650W，80 PLUS Gold，演示参考价', '/assets/images/hardware-psu.svg', 1),
('MSI MAG A750GL PCIE5 750W 电源', 'MSI', 'DIY 装机', 'PSU', 799.00, NULL, NULL, NULL, NULL, '750W，80 PLUS Gold，ATX 3.0，演示参考价', '/assets/images/hardware-psu.svg', 1),
('Corsair RM750e 750W 电源', 'Corsair', 'DIY 装机', 'PSU', 899.00, NULL, NULL, NULL, NULL, '750W，80 PLUS Gold，ATX 3.0，演示参考价', '/assets/images/hardware-psu.svg', 1),
('Seasonic FOCUS GX-850 850W 电源', 'Seasonic', 'DIY 装机', 'PSU', 1099.00, NULL, NULL, NULL, NULL, '850W，80 PLUS Gold，全模组，演示参考价', '/assets/images/hardware-psu.svg', 1),

('DeepCool CH370 机箱', 'DeepCool', 'DIY 装机', 'CASE', 329.00, NULL, NULL, NULL, NULL, 'mATX，显卡限长320mm，演示参考价', '/assets/images/hardware-case.svg', 1),
('Cooler Master TD300 Mesh 机箱', 'Cooler Master', 'DIY 装机', 'CASE', 499.00, NULL, NULL, NULL, NULL, 'mATX，显卡限长344mm，演示参考价', '/assets/images/hardware-case.svg', 1),
('Montech AIR 903 MAX 机箱', 'Montech', 'DIY 装机', 'CASE', 499.00, NULL, NULL, NULL, NULL, 'ATX，显卡限长400mm，演示参考价', '/assets/images/hardware-case.svg', 1),
('Corsair 3000D AIRFLOW 机箱', 'Corsair', 'DIY 装机', 'CASE', 599.00, NULL, NULL, NULL, NULL, 'ATX，显卡限长360mm，演示参考价', '/assets/images/hardware-case.svg', 1),
('LIAN LI LANCOOL 216 机箱', 'LIAN LI', 'DIY 装机', 'CASE', 799.00, NULL, NULL, NULL, NULL, 'ATX，显卡限长392mm，演示参考价', '/assets/images/hardware-case.svg', 1),
('JONSBO D31 Mesh 机箱', 'JONSBO', 'DIY 装机', 'CASE', 449.00, NULL, NULL, NULL, NULL, 'mATX，显卡限长400mm，演示参考价', '/assets/images/hardware-case.svg', 1),
('NZXT H6 Flow 机箱', 'NZXT', 'DIY 装机', 'CASE', 899.00, NULL, NULL, NULL, NULL, 'ATX，显卡限长365mm，演示参考价', '/assets/images/hardware-case.svg', 1);

INSERT INTO product_hardware_specs (product_id, cpu_socket, memory_type, form_factor, supported_form_factors, gpu_length_mm, case_gpu_max_length_mm, storage_interface, supported_storage_interfaces, psu_watt, estimated_power_w)
SELECT id, 'AM5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 65 FROM products WHERE name = 'AMD Ryzen 5 7500F 处理器'
UNION ALL SELECT id, 'AM5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 65 FROM products WHERE name = 'AMD Ryzen 7 7700 处理器'
UNION ALL SELECT id, 'AM5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 120 FROM products WHERE name = 'AMD Ryzen 7 7800X3D 处理器'
UNION ALL SELECT id, 'AM5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 65 FROM products WHERE name = 'AMD Ryzen 5 9600X 处理器'
UNION ALL SELECT id, 'LGA1700', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 148 FROM products WHERE name = 'Intel Core i5-13400F 处理器'
UNION ALL SELECT id, 'LGA1700', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 181 FROM products WHERE name = 'Intel Core i5-14600KF 处理器'
UNION ALL SELECT id, 'LGA1700', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 253 FROM products WHERE name = 'Intel Core i7-14700KF 处理器'
UNION ALL SELECT id, 'AM5', 'DDR5', 'mATX', NULL, NULL, NULL, NULL, 'M.2 NVMe,SATA 2.5', NULL, NULL FROM products WHERE name = 'GIGABYTE B650M DS3H 主板'
UNION ALL SELECT id, 'AM5', 'DDR5', 'ATX', NULL, NULL, NULL, NULL, 'M.2 NVMe,SATA 2.5', NULL, NULL FROM products WHERE name = 'MSI MAG B650 TOMAHAWK WIFI 主板'
UNION ALL SELECT id, 'AM5', 'DDR5', 'mATX', NULL, NULL, NULL, NULL, 'M.2 NVMe,SATA 2.5', NULL, NULL FROM products WHERE name = 'ASUS TUF GAMING B650M-PLUS WIFI II 主板'
UNION ALL SELECT id, 'LGA1700', 'DDR4', 'mATX', NULL, NULL, NULL, NULL, 'M.2 NVMe,SATA 2.5', NULL, NULL FROM products WHERE name = 'ASRock B760M Pro RS/D4 主板'
UNION ALL SELECT id, 'LGA1700', 'DDR5', 'mATX', NULL, NULL, NULL, NULL, 'M.2 NVMe,SATA 2.5', NULL, NULL FROM products WHERE name = 'ASUS TUF GAMING B760M-PLUS WIFI II 主板'
UNION ALL SELECT id, 'LGA1700', 'DDR5', 'ATX', NULL, NULL, NULL, NULL, 'M.2 NVMe,SATA 2.5', NULL, NULL FROM products WHERE name = 'GIGABYTE Z790 UD AX 主板'
UNION ALL SELECT id, 'LGA1700', 'DDR4', 'mATX', NULL, NULL, NULL, NULL, 'M.2 NVMe,SATA 2.5', NULL, NULL FROM products WHERE name = 'MSI PRO B760M-A WIFI DDR4 主板'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 240, NULL, NULL, NULL, NULL, 165 FROM products WHERE name = 'SAPPHIRE PULSE Radeon RX 7600 8GB 显卡'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 280, NULL, NULL, NULL, NULL, 245 FROM products WHERE name = 'SAPPHIRE PULSE Radeon RX 7700 XT 12GB 显卡'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 320, NULL, NULL, NULL, NULL, 263 FROM products WHERE name = 'SAPPHIRE PULSE Radeon RX 7800 XT 16GB 显卡'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 227, NULL, NULL, NULL, NULL, 160 FROM products WHERE name = 'ASUS Dual GeForce RTX 4060 Ti 8GB 显卡'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 242, NULL, NULL, NULL, NULL, 220 FROM products WHERE name = 'MSI GeForce RTX 4070 SUPER VENTUS 2X OC 显卡'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 261, NULL, NULL, NULL, NULL, 285 FROM products WHERE name = 'GIGABYTE RTX 4070 Ti SUPER WINDFORCE OC 16G 显卡'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, 200, NULL, NULL, NULL, NULL, 70 FROM products WHERE name = 'ASUS Dual GeForce RTX 3050 6GB 显卡'
UNION ALL SELECT id, NULL, 'DDR4', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'Kingston FURY Beast DDR4 16GB 内存套装'
UNION ALL SELECT id, NULL, 'DDR4', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'Corsair Vengeance LPX DDR4 16GB 内存套装'
UNION ALL SELECT id, NULL, 'DDR4', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'G.SKILL Ripjaws V DDR4 32GB 内存套装'
UNION ALL SELECT id, NULL, 'DDR5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'Kingston FURY Beast DDR5 32GB 内存套装'
UNION ALL SELECT id, NULL, 'DDR5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'Corsair Vengeance DDR5 32GB 内存套装'
UNION ALL SELECT id, NULL, 'DDR5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'ADATA XPG Lancer Blade DDR5 32GB 内存套装'
UNION ALL SELECT id, NULL, 'DDR5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM products WHERE name = 'TEAMGROUP T-CREATE CLASSIC DDR5 64GB 内存套装'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL, NULL FROM products WHERE name = 'Kingston NV2 1TB 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL, NULL FROM products WHERE name = 'Lexar NM790 1TB 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL, NULL FROM products WHERE name = 'Crucial P3 Plus 1TB 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL, NULL FROM products WHERE name = 'WD_BLACK SN770 1TB 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'M.2 NVMe', NULL, NULL, NULL FROM products WHERE name = 'Samsung 990 PRO 2TB 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'SATA 2.5', NULL, NULL, NULL FROM products WHERE name = 'Crucial BX500 1TB SATA 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, 'SATA 2.5', NULL, NULL, NULL FROM products WHERE name = 'Samsung 870 EVO 1TB SATA 固态硬盘'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 550, NULL FROM products WHERE name = 'MSI MAG A550BN 550W 电源'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 550, NULL FROM products WHERE name = 'Corsair CX550 550W 电源'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 650, NULL FROM products WHERE name = 'Thermaltake Smart BX1 650W 电源'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 650, NULL FROM products WHERE name = 'Cooler Master MWE Gold 650 V2 电源'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 750, NULL FROM products WHERE name = 'MSI MAG A750GL PCIE5 750W 电源'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 750, NULL FROM products WHERE name = 'Corsair RM750e 750W 电源'
UNION ALL SELECT id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 850, NULL FROM products WHERE name = 'Seasonic FOCUS GX-850 850W 电源'
UNION ALL SELECT id, NULL, NULL, NULL, 'mATX,Mini-ITX', NULL, 320, NULL, NULL, NULL, NULL FROM products WHERE name = 'DeepCool CH370 机箱'
UNION ALL SELECT id, NULL, NULL, NULL, 'mATX,Mini-ITX', NULL, 344, NULL, NULL, NULL, NULL FROM products WHERE name = 'Cooler Master TD300 Mesh 机箱'
UNION ALL SELECT id, NULL, NULL, NULL, 'ATX,mATX,Mini-ITX', NULL, 400, NULL, NULL, NULL, NULL FROM products WHERE name = 'Montech AIR 903 MAX 机箱'
UNION ALL SELECT id, NULL, NULL, NULL, 'ATX,mATX,Mini-ITX', NULL, 360, NULL, NULL, NULL, NULL FROM products WHERE name = 'Corsair 3000D AIRFLOW 机箱'
UNION ALL SELECT id, NULL, NULL, NULL, 'ATX,mATX,Mini-ITX', NULL, 392, NULL, NULL, NULL, NULL FROM products WHERE name = 'LIAN LI LANCOOL 216 机箱'
UNION ALL SELECT id, NULL, NULL, NULL, 'mATX,Mini-ITX', NULL, 400, NULL, NULL, NULL, NULL FROM products WHERE name = 'JONSBO D31 Mesh 机箱'
UNION ALL SELECT id, NULL, NULL, NULL, 'ATX,mATX,Mini-ITX', NULL, 365, NULL, NULL, NULL, NULL FROM products WHERE name = 'NZXT H6 Flow 机箱';
