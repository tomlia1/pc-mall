# M3.6 ChatWidget 设计

## 目标

在商城主页中提供全局常驻的 AI 导购入口，并让商品详情可将可信的当前商品上下文传入既有 SSE 聊天流程。

## 范围与边界

- 保留 `POST /api/v1/chat/stream`、原生 `fetch + ReadableStream`、打字机文本、停止生成、结构化商品卡及留资表单。
- 页面默认只显示右下角“AI 导购咨询”按钮；桌面端展开为右侧浮层，移动端展开为底部抽屉。
- 商品详情新增“咨询 AI”按钮，打开窗口并设置 `contextProductId`。
- 不增加 Redis、消息队列、向量数据库、支付、账号体系或新的聊天 REST 端点。

## 后端设计

`ChatRequest` 新增可选 `Long contextProductId`。`ChatServiceImpl` 接收后以 `ProductService.getActiveById` 查询 MySQL；商品不存在或下架时返回既有统一 404 业务错误。服务端将查出的名称、价格、CPU、GPU、内存和存储以只读上下文附加到 system prompt，绝不使用前端传入的任何商品事实。

## 前端设计

将旧的全屏聊天状态和 SSE 逻辑移入 `ChatWidget.vue`。商城页面只负责控制窗口打开状态与可选的 `contextProductId`。`ProductDetailModal` 发出 `consult` 事件；`MarketplaceApp` 接收后关闭详情弹窗、打开 ChatWidget，并将商品 ID 传入。

窗口具有可访问的开关按钮、关闭按钮和清晰的生成/停止状态。桌面端固定在右下角；在 720px 以下改为屏幕底部抽屉。动画仅使用 150–250ms 的透明度和位移过渡，并遵从 `prefers-reduced-motion`。

## 验收

1. 未打开聊天时商城浏览与筛选正常。
2. 右下角按钮能打开和关闭聊天窗口；移动布局为底部抽屉。
3. 商品详情的“咨询 AI”可带入该商品 ID。
4. 后端仅在商品存在且在售时构造上下文，不能信任客户端商品参数。
5. SSE 文本、商品卡、停止生成、留资和隐私强制校验继续工作。
