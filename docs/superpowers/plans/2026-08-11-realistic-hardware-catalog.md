# Realistic Hardware Catalog Implementation Plan

> **For agentic workers:** Execute inline in the current workspace; do not create a worktree or modify existing migrations.

**Goal:** Expand the demo hardware catalog from 53 to 102 active products with seven additional real-world model choices for each DIY build slot.

**Architecture:** Flyway V10 inserts 49 `products` rows and their corresponding `product_hardware_specs` rows. Flyway V11 corrects two vendor-spec facts discovered after V10 had been applied. Existing APIs and compatibility services continue to query the same tables, so no application code or API contract changes are required.

**Tech Stack:** MySQL, Flyway, Spring Boot 3, existing Vue product API.

## Global Constraints

- Create `V10__expand_realistic_hardware_catalog.sql` and, only because V10 was already applied, `V11__correct_realistic_hardware_specs.sql`; never alter V1 through V10.
- Use real, publicly documented model families and technical attributes, but label all prices as demo reference prices rather than live offers.
- Use only existing local neutral component SVG paths; do not fetch or hotlink external brand images.
- Keep product facts compatible with the seven-slot compatibility checker.
- Add no dependencies, tables, endpoints, AI behavior, cart, payment, or purchase functionality.

---

### Task 1: Add the V10 catalog migration

**Files:**

- Create: `src/main/resources/db/migration/V10__expand_realistic_hardware_catalog.sql`

**Produces:** 49 active product rows: seven each of CPU, motherboard, GPU, RAM kit, SSD, PSU, and case; each row has a matching `product_hardware_specs` record.

- [ ] Insert CPU rows with `cpu_socket` and `estimated_power_w`.
- [ ] Insert motherboard rows with socket, memory generation, form factor, and M.2/SATA support.
- [ ] Insert GPU rows with card length and estimated board power.
- [ ] Insert RAM kits with the correct `memory_type`.
- [ ] Insert SSDs with `storage_interface`.
- [ ] Insert PSUs with `psu_watt`.
- [ ] Insert cases with form-factor and GPU-length support.
- [ ] Correct any post-application vendor-spec discrepancy with a new migration rather than editing V10.

### Task 2: Verify the migration and product browsing

**Files:**

- Verify: `src/main/resources/db/migration/V10__expand_realistic_hardware_catalog.sql`

**Checks:**

- [ ] Run `mvn test -q` with JDK 17.
- [ ] Restart Spring Boot so Flyway applies V10; verify migration history reports version 10.
- [ ] Request the products API and assert 102 active products, seven product types, and valid per-type filtering.
- [ ] Run `npm.cmd run build` in `frontend`.
- [ ] Open the marketplace in a browser and verify the larger catalog and a hardware-type filter render correctly.
