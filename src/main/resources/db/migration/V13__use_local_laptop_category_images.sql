-- Use locally supplied category images. These images represent laptop categories, not exact SKU photos.
UPDATE products SET cover_image = '/assets/images/device-gaming-laptop.png'
WHERE product_type = 'LAPTOP' AND category = '游戏';

UPDATE products SET cover_image = '/assets/images/device-thin-light-laptop.png'
WHERE product_type = 'LAPTOP' AND category = '办公/编程';
