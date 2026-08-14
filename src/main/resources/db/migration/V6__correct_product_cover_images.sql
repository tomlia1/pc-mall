-- 中性本地设备展示图：按真实设备类型覆盖历史种子封面，不修改 V1～V5。
UPDATE products SET cover_image = '/assets/images/device-ultrabook.svg' WHERE category = '办公/编程';
UPDATE products SET cover_image = '/assets/images/device-gaming-laptop.svg' WHERE category = '游戏';
UPDATE products SET cover_image = '/assets/images/device-workstation.svg' WHERE category = '设计/创作';
UPDATE products SET cover_image = '/assets/images/device-mini-pc.svg' WHERE name LIKE '%迷你%' OR tags LIKE '%迷你主机%';
