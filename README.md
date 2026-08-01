# PC Mall M1 Backend

Java 17 / Spring Boot 3 的 AI 智能电脑导购 MVP 后端。当前只包含管理员认证、商品查询、留资、数据库迁移、Swagger 和 Docker Compose；不包含聊天页面、DeepSeek 或 SSE。

## API

- `POST /api/v1/admin/login` — `{ "username": "admin", "password": "..." }`
- `GET /api/v1/products?category=游戏&maxPrice=6000&tag=LOL&page=1&pageSize=20`
- `POST /api/v1/leads` — 需提交 `privacyAgreed: true`
- Swagger UI: `/swagger-ui/index.html`
- Health: `/actuator/health`

所有业务响应使用 `{ "code": 200, "message": "success", "data": ... }`；参数或业务错误会返回相应 HTTP 状态和同一响应结构。

## 本地构建

Maven 必须以 JDK 17 运行：

```powershell
$env:JAVA_HOME='D:\dev\JAVA\JDK-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn test
```

## Docker Desktop 安装后

1. 从 `.env.example` 复制为 `.env`，填入所有占位符；`JWT_SECRET_BASE64` 解码后至少为 32 字节。
2. 执行 `docker compose up --build`。
3. 首次启动时，若 `ADMIN_INITIAL_USERNAME` 不存在，应用会以 `ADMIN_INITIAL_PASSWORD` 的 BCrypt 散列创建该管理员。不要在仓库或日志中保存真实密码。

MySQL 没有发布宿主机端口，只在 Compose 内部网络中可用。Flyway 会自动执行 DDL 和商品种子数据。
