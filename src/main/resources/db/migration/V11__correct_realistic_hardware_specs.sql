-- V10 已执行后，根据厂商公开规格做两项更正；不改写历史迁移。

UPDATE product_hardware_specs specs
JOIN products product ON product.id = specs.product_id
SET specs.gpu_length_mm = 280
WHERE product.name = 'SAPPHIRE PULSE Radeon RX 7800 XT 16GB 显卡';

UPDATE products
SET tags = '16GB GDDR6，长度280mm，演示参考价'
WHERE name = 'SAPPHIRE PULSE Radeon RX 7800 XT 16GB 显卡';

UPDATE product_hardware_specs specs
JOIN products product ON product.id = specs.product_id
SET specs.case_gpu_max_length_mm = 378
WHERE product.name = 'DeepCool CH370 机箱';

UPDATE products
SET tags = 'mATX，显卡限长378mm（装前置冷排时为320mm），演示参考价'
WHERE name = 'DeepCool CH370 机箱';
