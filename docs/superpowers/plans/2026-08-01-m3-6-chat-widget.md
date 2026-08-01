# M3.6 ChatWidget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a responsive AI shopping-guide widget to the marketplace and pass an optional, server-validated product context to the existing SSE chat API.

**Architecture:** `ChatRequest` receives only an optional product ID. `ChatServiceImpl` resolves that ID through `ProductService`, then adds facts from MySQL to the server-owned system prompt. Vue moves the existing stream state into `ChatWidget`; the marketplace opens it globally or from a product detail consultation event.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Vue 3 Composition API, TypeScript, Vite, native Fetch/ReadableStream.

## Global Constraints

- Do not add dependencies, Redis, queues, vector storage, payment, accounts, or another chat endpoint.
- Keep `POST /api/v1/chat/stream` and the `Result` convention for non-SSE endpoints.
- Product facts come only from MySQL; the client sends only `contextProductId`.
- Keep DeepSeek keys out of source, logs, tests, and commits.
- Use semantic controls, dynamic ARIA state, 150–250ms motion, and reduced-motion fallback.

---

### Task 1: Secure product context for the existing chat stream

**Files:**
- Modify: `src/main/java/com/pcmall/dto/ChatRequest.java`
- Modify: `src/main/java/com/pcmall/service/impl/ChatServiceImpl.java`
- Modify: `src/test/java/com/pcmall/service/impl/ChatServiceImplTest.java`

**Interfaces:**
- Consumes: `ChatRequest.sessionId()`, `message()`, optional `contextProductId()`.
- Consumes: `ProductService.getActiveById(long)`.
- Produces: a system prompt containing server-read product facts only when the ID resolves to an active product.

- [ ] **Step 1: Write failing tests for product-context resolution**

```java
@Test
void addsOnlyDatabaseBackedProductFactsToTheSystemPrompt() {
    when(productService.getActiveById(2L)).thenReturn(product(2L, "游戏先锋 G15"));
    service.chat(new ChatRequest(UUID, "这台能玩游戏吗", 2L));
    assertTrue(firstSystemPrompt().contains("游戏先锋 G15"));
}

@Test
void rejectsAnInactiveOrMissingContextProduct() {
    when(productService.getActiveById(404L)).thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或已下架"));
    assertThrows(BusinessException.class, () -> service.chat(new ChatRequest(UUID, "咨询", 404L)));
}
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run: `mvn -Dtest=ChatServiceImplTest test` with JDK 17.

Expected: compilation or assertion failure because `contextProductId` and server-side context handling do not exist.

- [ ] **Step 3: Implement minimal trusted-context resolution**

```java
private String systemPrompt(Long contextProductId) {
    if (contextProductId == null) return SYSTEM_PROMPT;
    Product product = productService.getActiveById(contextProductId);
    return SYSTEM_PROMPT + "\n[当前用户正在查看商品: " + product.getName()
            + ", 价格: " + product.getPrice() + "元, CPU: " + product.getCpu()
            + ", GPU: " + product.getGpu() + ", 内存: " + product.getMemoryGb()
            + "GB, 存储: " + product.getStorageGb() + "GB]";
}
```

Pass the prompt into `loadHistory`; do not accept product name, price or hardware fields from the request.

- [ ] **Step 4: Run focused test and full Maven suite**

Run: `mvn -Dtest=ChatServiceImplTest test`, then `mvn test` with JDK 17.

Expected: all focused and full tests pass.

### Task 2: Build the responsive ChatWidget and product-detail handoff

**Files:**
- Create: `frontend/src/components/ChatWidget.vue`
- Create: `frontend/src/chat-widget.css`
- Modify: `frontend/src/MarketplaceApp.vue`
- Modify: `frontend/src/components/ProductDetailModal.vue`
- Modify: `frontend/src/components/LeadModal.vue`

**Interfaces:**
- `ChatWidget` props: `open: boolean`, `contextProductId: number | null`.
- `ChatWidget` emits: `close`.
- `ProductDetailModal` emits: `close`, `consult(productId: number)`.
- `MarketplaceApp` stores `chatOpen` and `chatContextProductId` and passes them to `ChatWidget`.

- [ ] **Step 1: Extract existing stream behavior into ChatWidget**

```ts
const body = { sessionId: sessionId(), message }
if (props.contextProductId !== null) body.contextProductId = props.contextProductId
await fetch('/api/v1/chat/stream', { method: 'POST', body: JSON.stringify(body), ... })
```

Retain message parsing, typewriter queue, abort controller, product-card rendering and `LeadModal` submission.

- [ ] **Step 2: Add semantic widget controls and responsive layout**

```html
<button :aria-expanded="open" aria-controls="ai-shopping-guide" @click="open = true">AI 导购咨询</button>
<aside id="ai-shopping-guide" role="dialog" aria-modal="false">...</aside>
```

Use fixed right-bottom panel above 720px and bottom-sheet layout at or below 720px. Include visible focus styles and `@media (prefers-reduced-motion: reduce)`.

- [ ] **Step 3: Wire detail consultation into the widget**

```ts
function consultProduct(productId: number) {
  selectedProduct.value = null
  chatContextProductId.value = productId
  chatOpen.value = true
}
```

The detail modal must emit only the product ID; it must not put product details in the chat payload.

- [ ] **Step 4: Build frontend**

Run: `npm.cmd run build` in `frontend`.

Expected: Vue type-check and Vite production build pass.

### Task 3: Local integration and acceptance

**Files:**
- Test: `src/test/java/com/pcmall/service/impl/ChatServiceImplTest.java`
- Test manually: `http://127.0.0.1:5173`

- [ ] **Step 1: Package and start the backend with JDK 17 and process-only local secrets**

Run: `mvn package -DskipTests`; start on port 8081 with environment variables only.

- [ ] **Step 2: Verify trusted context through the live SSE endpoint**

Send a UUID, a message and `contextProductId: 2`; verify message/product/done SSE events. Send a nonexistent ID and verify an SSE error event without contacting DeepSeek.

- [ ] **Step 3: Verify browser behavior**

Use browser automation to verify: normal marketplace browsing; widget opening and closing; desktop panel; product detail “咨询 AI” opens widget; a streaming response renders; stop generation works; lead dialog keeps the mandatory privacy checkbox.

- [ ] **Step 4: Report evidence and stop**

Report modified files, test commands and actual results. Do not begin M4.
