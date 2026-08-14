-- Use the locally supplied, type-specific product photos for hardware cards.
-- These are neutral category images and do not claim to be an exact SKU photo.
UPDATE products SET cover_image = '/assets/images/hardware-cpu.png' WHERE product_type = 'CPU';
UPDATE products SET cover_image = '/assets/images/hardware-motherboard.png' WHERE product_type = 'MOTHERBOARD';
UPDATE products SET cover_image = '/assets/images/hardware-gpu.png' WHERE product_type = 'GPU';
UPDATE products SET cover_image = '/assets/images/hardware-ram.png' WHERE product_type = 'RAM';
UPDATE products SET cover_image = '/assets/images/hardware-ssd.png' WHERE product_type = 'SSD';
UPDATE products SET cover_image = '/assets/images/hardware-psu.png' WHERE product_type = 'PSU';
UPDATE products SET cover_image = '/assets/images/hardware-case.png' WHERE product_type = 'CASE';
