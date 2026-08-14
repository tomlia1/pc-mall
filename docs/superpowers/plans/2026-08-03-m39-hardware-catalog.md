# M3.9 Hardware Catalog and Build List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the hardware catalog and seven-slot build-list prototype without AI build advice or compatibility rules.

**Architecture:** V7 adds catalog type and nullable laptop-only fields; V8 adds complete, local demo hardware records. The existing products endpoint gains a `productType` filter, and the Vue catalog keeps the draft build only in current-page memory. The UI uses product IDs and product facts returned by the API; it does not send or fabricate technical specs.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis-Plus, Flyway, MySQL 8, Vue 3, TypeScript, Vite.

## Global Constraints

- Do not modify V1 through V6; add only V7 and V8 migrations.
- Keep `products.category` as a use-case category; add `product_type` for catalog type.
- `cpu`, `memory_gb`, and `storage_gb` become nullable; no component receives invented laptop fields.
- Do not add dependencies, AI build prompts, compatibility-check APIs, Docker work, or persistence for the draft build.
- Preserve the existing Paper-Tech Catalog visual language and current chat/SSE behavior.

---

### Task 1: Hardware catalog database foundation

**Files:**
- Create: `src/main/resources/db/migration/V7__add_hardware_catalog_foundation.sql`
- Create: `src/main/resources/db/migration/V8__seed_hardware_catalog.sql`

**Interfaces:**
- Produces: `products.product_type` with `LAPTOP`, `CPU`, `MOTHERBOARD`, `GPU`, `RAM`, `SSD`, `PSU`, and `CASE` values.
- Produces: nullable `cpu`, `memory_gb`, `storage_gb` and typed component fields in `product_hardware_specs`.

- [ ] **Step 1: Add a migration assertion test**

Write a Spring integration test that asserts active product records can contain a `CPU` product with null laptop-only fields and a non-null type.

- [ ] **Step 2: Run the assertion before migrations exist**

Run: `mvn test -Dtest=ProductHardwareCatalogIntegrationTest`

Expected: failure because `product_type` and the component catalog do not exist.

- [ ] **Step 3: Add V7 and V8**

V7 adds `product_type`, changes the three laptop-only columns to nullable, and creates `product_hardware_specs`. V8 inserts complete, active component records for the seven build slots and their machine-readable facts.

- [ ] **Step 4: Run the integration test after migration**

Run: `mvn test -Dtest=ProductHardwareCatalogIntegrationTest`

Expected: pass; CPU records have null laptop-only fields and all seven types are represented.

### Task 2: Product API type filter

**Files:**
- Modify: `src/main/java/com/pcmall/entity/Product.java`
- Modify: `src/main/java/com/pcmall/dto/ProductQuery.java`
- Modify: `src/main/java/com/pcmall/controller/ProductController.java`
- Modify: `src/main/java/com/pcmall/service/impl/ProductServiceImpl.java`
- Modify: `src/test/java/com/pcmall/service/impl/ProductServiceImplTest.java`

**Interfaces:**
- Consumes: `GET /api/v1/products?productType=<type>`.
- Produces: active `Product` records including `productType`.

- [ ] **Step 1: Write failing service tests**

Add tests that verify a type filter returns only matching active products and rejects a value outside the eight allowed product types.

- [ ] **Step 2: Run the focused test class**

Run: `mvn test -Dtest=ProductServiceImplTest`

Expected: failure because `ProductQuery` has no product type and the service cannot filter it.

- [ ] **Step 3: Implement minimal API support**

Add an enum-backed query parameter, map it to `Product.productType`, and add one MyBatis-Plus equality predicate.

- [ ] **Step 4: Re-run focused tests**

Run: `mvn test -Dtest=ProductServiceImplTest`

Expected: pass.

### Task 3: In-memory seven-slot build state

**Files:**
- Create: `frontend/src/build-list.ts`
- Create: `frontend/src/build-list.test.ts` only if a no-dependency executable TypeScript test command is available; otherwise verify through browser acceptance.
- Modify: `frontend/src/types.ts`

**Interfaces:**
- Consumes: `Product` records with `productType`.
- Produces: `BuildSlot`, `BUILD_SLOTS`, `addToBuild(product)`, `removeFromBuild(type)`, `selectedProducts`, `totalPrice`, and `missingSlots`.

- [ ] **Step 1: Define expected slot behavior**

Specify one current-page build item per type, replacement for the same type, total price from selected products, and missing required slots.

- [ ] **Step 2: Verify replacement through the page**

Use browser automation to add two products of the same type and assert that only the latest selection remains in that slot.

- [ ] **Step 3: Implement the minimal reactive state helper**

Use Vue reactive primitives only. Do not use localStorage, sessionStorage, database persistence, or a new state library.

- [ ] **Step 4: Re-run replacement and total-price verification**

Use browser automation to assert seven-slot totals and missing-item counts.

### Task 4: Hardware catalog and responsive build-list UI

**Files:**
- Modify: `frontend/src/MarketplaceApp.vue`
- Modify: `frontend/src/components/ProductCard.vue`
- Modify: `frontend/src/components/ProductDetailModal.vue`
- Create: `frontend/src/components/BuildListPanel.vue`
- Modify: `frontend/src/marketplace.css`

**Interfaces:**
- Consumes: product list data and in-memory build state.
- Produces: desktop right-side build panel and mobile fixed summary plus bottom drawer.

- [ ] **Step 1: Capture the current marketplace baseline**

Use browser automation to capture the existing desktop page before UI changes.

- [ ] **Step 2: Implement card and detail actions**

Keep whole-card detail opening. Add a separate `加入装机单` action only for the seven component types; laptop products stay browseable and cannot occupy a build slot.

- [ ] **Step 3: Implement desktop and mobile build surfaces**

Show all seven slots, selected product names, total price, and missing slots. Show only a non-verdict “兼容检查将在下一阶段接入” note; do not render fabricated ERROR, WARNING, or pass results.

- [ ] **Step 4: Verify desktop and 375px layouts**

Use browser automation to open product detail, add and replace an item, and capture desktop plus 375px screenshots.

### Task 5: Full verification

**Files:**
- Verify: `src/main/resources/db/migration/V7__add_hardware_catalog_foundation.sql`
- Verify: `src/main/resources/db/migration/V8__seed_hardware_catalog.sql`
- Verify: `frontend/`
- Verify: backend test suite

- [ ] **Step 1: Verify Flyway against the local MySQL database**

Run the current Spring Boot application and query `flyway_schema_history`, product counts by `product_type`, and null laptop-only fields for component products.

- [ ] **Step 2: Run backend tests**

Run: `mvn test`

Expected: all existing and new tests pass.

- [ ] **Step 3: Run frontend build**

Run: `npm run build` from `frontend`.

Expected: Vue type checking and Vite production build pass.

- [ ] **Step 4: Run browser acceptance**

Verify product-type filtering, full-card detail opening, add/replace behavior, seven-slot empty state, total price, missing slots, desktop right rail, and 375px bottom drawer.
