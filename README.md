# PC Mall

面向电脑选购与 DIY 装机的 AI 导购 MVP。用户可以筛选在售电脑和硬件、加入装机单进行基础兼容性检查，并在配置 DeepSeek 后获得基于商品库事实的导购建议。

## 功能

- 商品目录：按关键词、用途、品牌、价格和硬件类型筛选
- 商品详情与咨询留资：提交前必须同意隐私说明
- DIY 装机单：CPU、主板、显卡、内存、SSD、电源和机箱的基础兼容性检查
- AI 导购：根据预算、用途和当前商品库给出建议；AI 不能编造商品事实
- 管理员登录：BCrypt 密码散列、JWT、失败锁定
- 部署：Vue、Spring Boot、MySQL 和 Nginx 的 Docker Compose 栈

## 技术栈

- Java 17、Spring Boot 3、Spring Security、MyBatis-Plus、Flyway、MySQL 8
- Vue 3、TypeScript、Vite
- Docker Compose、Nginx

## 快速启动

1. 复制环境变量模板：

   ```powershell
   Copy-Item .env.example .env
   ```

2. 编辑 `.env`，替换数据库密码、JWT 密钥和初始管理员密码。`JWT_SECRET_BASE64` 解码后必须至少为 32 字节。AI 导购不是必填功能，未配置 `DEEPSEEK_API_KEY` 时会返回可理解的不可用提示。

3. 构建并启动：

   ```powershell
   docker compose up --build -d
   ```

4. 打开：

   - 商城：`http://localhost/`
   - Swagger：`http://localhost/swagger-ui/index.html`
   - 健康检查：`http://localhost/actuator/health`

MySQL 和 Spring Boot 服务只在 Compose 内部网络中暴露；宿主机只开放 Nginx 的 80 端口。

## 本地开发

后端需要 JDK 17：

```powershell
$env:JAVA_HOME='D:\dev\JAVA\JDK-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

前端开发服务会把 `/api` 代理到 `http://localhost:8081`：

```powershell
Set-Location frontend
npm ci
npm run dev
```

## 验证

```powershell
$env:JAVA_HOME='D:\dev\JAVA\JDK-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn clean test

Set-Location frontend
npm run build
node --test scripts/api-error-handling.test.mjs
Get-ChildItem scripts\verify-*.mjs | ForEach-Object { node $_.FullName }
```

## API 概览

- `GET /api/v1/products`：商品筛选与分页
- `GET /api/v1/products/{id}`：商品详情
- `POST /api/v1/builds/check`：装机兼容性检查
- `POST /api/v1/builds/analyze/stream`：AI 装机分析 SSE
- `POST /api/v1/chat/stream`：AI 导购 SSE
- `POST /api/v1/leads`：留资
- `POST /api/v1/admin/login`：管理员登录

详细请求与响应请使用 Swagger 查看。
