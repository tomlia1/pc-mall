# M3.5 商城主页与商品详情实施计划

> 状态：执行中。范围仅为 M3.5；保留现有 M1/M2/M3 聊天代码，M3.6 再改造为右下角组件。

## 目标

让访客无需打开聊天功能即可浏览在售商品、按条件筛选、搜索关键词并查看商品详情。商品事实仍以 MySQL `products` 表为唯一来源。

## 任务 1：商品数据与后端 API

1. 新增 `V4__update_seed_cover_images.sql`，仅更新既有种子商品的 `cover_image`，不改动 V1/V2/V3。
2. 先为 `ProductService` 的在售商品详情和过滤查询编写失败测试。
3. 扩展 `ProductQuery`、`ProductController` 和 `ProductServiceImpl`：支持 `minPrice`、`maxPrice`、`category`、`brand`、`keyword`，默认 `page=1`、`pageSize=12`；关键词匹配名称、CPU、GPU。
4. 增加 `GET /api/v1/products/{id}`，仅返回 `status=1` 的商品；不存在或下架返回统一的 404 错误。
5. 运行相关单元测试，再运行全部 Maven 测试。

## 任务 2：Vue 商城页面

1. 创建本地 SVG 商品图和 `/assets/images/default-laptop.svg`，避免外部图片依赖。
2. 保留现有聊天 `App.vue`，新增 `MarketplaceApp.vue` 作为当前入口；不实现 M3.6 的 ChatWidget。
3. 增加商品 API 封装、图片回退组件、商品卡片和无障碍详情弹窗。
4. 实现响应式首页：顶部导航与搜索、价格/品牌/分类筛选、商品网格、分页及商品详情。
5. 按 UI/UX Pro Max 的设计令牌实现可访问、移动端自适应的界面，不增加第三方 UI 依赖。
6. 运行 `npm run build`。

## 任务 3：本机联调与验收

1. 通过 Flyway 在本机 MySQL 应用 V4，并检查图片路径。
2. 使用 Spring Boot 实例验证筛选、商品详情和 404 响应。
3. 使用浏览器验证独立商城浏览、筛选、商品详情和图片回退。
4. 汇报修改文件、实际命令及结果；停止于 M3.5，不开始 M3.6/M4。
