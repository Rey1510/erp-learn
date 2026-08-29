# 🚀 Enterprise ERP & POS System (Spring Boot + PostgreSQL + Nuxt 4)

A fullstack enterprise **Inventory, Order POS Management & Sales Analytics** platform built with modern **Spring Boot 3 (Java 17)**, **PostgreSQL 16**, and **Nuxt 4 (Vue 3 Composition API & Tailwind CSS)**.

---

## 🏗️ Architecture & Technology Stack

| Tier | Technology | Description |
| :--- | :--- | :--- |
| **Database** | PostgreSQL 16 (Docker) | Relational persistence with foreign keys, indexes, and audit logs |
| **Backend** | Spring Boot 3 + JPA Hibernate | REST APIs, JPA Criteria dynamic filtering, CSV parser, RBAC |
| **Frontend** | Nuxt 4 + Vue 3 (Composition API) | SSR-ready reactive SPA, Tailwind CSS, SVG BI charts, i18n |
| **Security** | Role-Based Access Control (RBAC) | Role-based navigation guards with SSR-friendly cookie persistence |

---

## ⚡ Quick Start Guide (Step-by-Step)

### 📋 Prerequisites
Make sure you have the following installed on your machine:
* **Docker & Docker Compose**
* **Java 17+** (OpenJDK or Oracle JDK)
* **Apache Maven 3.8+**
* **Node.js 18+** & `npm`

---

### Step 1: 🐳 Run Database via Docker
From the project root directory, spin up PostgreSQL 16:

```bash
# Start PostgreSQL container in background
docker compose up -d

# Verify container is healthy and running on port 5432
docker ps
```

> **Database Credentials**:
> * **Host**: `localhost:5432`
> * **Database**: `erp_db`
> * **Username**: `postgres`
> * **Password**: `password123`

---

### Step 2: ☕ Run Backend (Spring Boot 3)
Navigate to the `backend` directory and launch the Spring Boot application:

```bash
cd backend

# Run Spring Boot with Maven
mvn -s settings-public.xml spring-boot:run
```

* Backend server will start on **`http://localhost:8080`**.
* The database schema (`users`, `products`, `orders`, `order_items`, `stock_movements`) will be automatically created, updated, and seeded with demo accounts and sample products upon startup (`DataInitializer.java`).

---

### Step 3: 💚 Run Frontend (Nuxt 4 / Vue 3)
Open a new terminal window, navigate to the `frontend` directory, and start the development server:

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

* Open your browser at **`http://localhost:3000`**.

---

## 🔑 Demo Accounts & Pre-Seeded Roles

Use the quick autofill buttons on the login page (`/login`) or type the credentials manually:

| Role | Email | Password | Permissions & Capabilities |
| :--- | :--- | :--- | :--- |
| 👑 **Administrator** | `admin@mail.com` | `admin123` | Full access: Master catalog CRUD, restock ledger, CSV import/export, bulk multi-select delete, executive BI sales analytics. |
| 🛒 **Cashier** | `cashier@mail.com` | `cashier123` | POS checkout terminal, process transactions, generate thermal receipts, read-only product availability search. |

---

## 🌟 Implemented Enterprise Modules

### 1. 🧾 80mm Printable Thermal Receipt & PDF Invoices
* Isolated iframe printing engine formatted for standard 80mm point-of-sale thermal roll printers.
* Displays transaction metadata, customer details, itemized table, and barcode.

### 2. 📊 Executive BI Dashboard & Interactive SVG Charts
* Pure SVG Cubic Bezier Spline charts showing revenue trends across 7 days, 30 days, or all time.
* Real-time KPIs (Total Gross Revenue, Average Order Value, Total Units Sold, Success Conversion Rate).
* Top 5 Best-Selling Products Leaderboard and Category Revenue Distribution.

### 3. ⚡ Server-Side Criteria API Pagination & Multi-Column Sorting
* Handles large datasets efficiently with JPA `Specification` and dynamic SQL queries (`WHERE`, `LIKE`, `ORDER BY`).
* Responsive pagination controls with custom page size selectors (5, 10, 20, 50).

### 4. 🛡️ Stock Movement Audit Ledger & Warehouse Restock
* Strict audit ledger (`StockMovement`) tracking all quantity adjustments (`SALE`, `RESTOCK`, `INITIAL`, `ADJUSTMENT`).
* Complete chronological audit trail for every SKU to prevent inventory discrepancies.

### 5. 📦 Bulk Multi-Select Operations & CSV / Excel Import-Export
* Multi-select row selection with floating batch delete bar.
* Foreign-key safe product deletion (preserves immutable historical order snapshots).
* RFC4180-compliant CSV Export with UTF-8 BOM (`\uFEFF`) for Microsoft Excel & Google Sheets.
* Multi-row CSV batch import with pre-submit validation error highlighting.

### 6. 🌐 Multi-Language (EN / ID) & 🌓 Dark/Light Theme Switching
* Instant toggle between **English** (`🇬🇧 EN`) and **Bahasa Indonesia** (`🇮🇩 ID`).
* Instant switch between **Dark Mode** (Obsidian Slate Glassmorphism) and **Light Mode** (Crisp Corporate White/Slate-50).
* **SSR Cookie Persistence** (`erp_theme`, `erp_locale`, `erp_auth_user`): Zero layout shift or hydration flash on browser refresh.

### 7. 🔒 Concurrency Control & Pessimistic Locking (Zero Overselling)
* **Pessimistic Write Lock (`SELECT ... FOR UPDATE`)**: Implements database row-level locking via JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` when reading and deducting product inventory.
* **Deadlock Prevention**: Deterministic item ordering (`productId` ASC) during multi-item transactions prevents circular wait locks across concurrent POS cashiers.
* **Collision-Free Invoicing**: Generates microsecond-precise, non-colliding order numbers (`ORD-YYYYMMDDHHMMSS-XXXX`) under heavy simultaneous traffic.
* **Domain Exception**: Returns structured `400 Bad Request` with `INSUFFICIENT_STOCK` payload to prevent race-condition overselling.

### 8. 🛡️ Idempotent API & 💳 Mock Payment Gateway Sandbox (Portfolio Mode)
* **`X-Idempotency-Key` Protection**: Prevents double-billing and duplicate stock deductions on network retry or double-click checkout via SHA-256 payload hashing and atomic response caching (`IdempotencyRecord`).
* **Built-in Fintech Sandbox Simulator**:
  * 📱 **QRIS**: Real SVG dynamic QR matrix with 15-minute countdown and Indonesian Standard NMID.
  * 🏦 **Virtual Account**: BCA / Bank Mandiri copyable VA numbers with ATM payment instructions.
  * 💳 **Credit Card**: Mock 3DS OTP authorization flow.
  * 💵 **Tunai (Cash)**: Instant POS cash settlement.
* **Interactive Webhook Control Panel**:
  * 🟢 **Simulate Pay**: Triggers simulated settlement webhook (`SETTLE` -> Order `PAID`).
  * ⏱️ **Simulate Expire**: Simulates payment timeout (`EXPIRE` -> Order `CANCELLED`, automatic stock restoration `CANCEL_RESTOCK`).
  * 🔴 **Simulate Fail**: Simulates declined card or insufficient funds with auto-restock.
* **Automatic Expiration Scheduler**: Spring Boot `@Scheduled` worker automatically checks and marks expired transactions while releasing reserved inventory back to the warehouse catalog.
* **Zero External Dependencies**: Anyone cloning the repository can test realistic enterprise fintech flows immediately without needing third-party API keys (Midtrans/Stripe).

### 9. ⚡ POS Offline-First Mode & 🗄️ IndexedDB Background Sync Engine
* **Zero Downtime Checkout**: Cashiers can continue creating transactions and generating thermal receipts even when internet connectivity drops.
* **Browser IndexedDB Outbox Queue (`ERP_OFFLINE_POS_DB`)**: Securely queues offline transactions as `PENDING_SYNC` with temporary offline invoice codes (`OFFLINE-ORD-...`).
* **Optimistic Local Inventory**: Automatically decrements local stock counts in real time to prevent physical overselling at the storefront.
* **Resilient Background Sync Dispatcher**:
  * Automatically detects network recovery via `window.addEventListener('online')` and flushes the outbox queue to Spring Boot (`POST /api/orders`).
  * Seamlessly applies `X-Idempotency-Key` (`IDEM-OFFLINE-...`) during synchronization to ensure zero duplicate records.
* **Built-in Offline Simulator & Sync Center**:
  * Dedicated **Offline Sync Center Modal** to monitor queue status (`PENDING_SYNC` $\to$ `SYNCING` $\to$ `SYNCED`).
  * 🔴/🟢 Interactive simulation switch on the top navigation bar for easy portfolio demonstrations without disabling physical Wi-Fi.

---

## 🧪 Automated Testing & Integrity Verification

Run the full Spring Boot test suite:

```bash
cd backend
mvn -s settings-public.xml test
```

### Verified Test Suites:
1. **`OrderServiceConcurrencyTest`** (10 parallel threads):
   * ✅ Exactly 1 winner cashier captures the lock and checks out.
   * ✅ 9 cashiers receive safe `InsufficientStockException`.
   * ✅ Final stock in PostgreSQL remains strictly `0` (`OUT_OF_STOCK`), never negative.
2. **`OrderServiceIdempotencyTest`** (5 repeated submissions with same `X-Idempotency-Key`):
   * ✅ Returns identical invoice metadata across all 5 requests.
   * ✅ Stock is deducted **only once**, proving zero double-billing.

---

## 📂 Project Directory Structure

```
erp-learn/
├── docker-compose.yml              # PostgreSQL 16 Docker service configuration
├── README.md                       # Comprehensive runbook & documentation
│
├── backend/                        # Spring Boot 3 Java Maven Project
│   ├── pom.xml                     # Maven dependencies & plugins
│   ├── settings-public.xml         # Public Maven repository mirror settings
│   └── src/main/java/com/learn/erp/
│       ├── config/                 # DataInitializer (Seeding & Schema tweaks)
│       ├── controller/             # REST API Controllers (Auth, Products, Orders, Export, Audit)
│       ├── dto/                    # Request & Response Transfer Objects
│       ├── model/                  # JPA Entities (User, Product, Order, OrderItem, StockMovement)
│       ├── repository/             # Spring Data JPA Repositories & Specifications
│       └── service/                # Core Business Logic & CSV Engines
│
└── frontend/                       # Nuxt 4 + Vue 3 + Tailwind CSS App
    ├── app/
    │   ├── app.vue                 # Root layout & SSR theme synchronization
    │   ├── components/             # Reusable UI Components, Modals & Analytics Widgets
    │   ├── composables/            # Reactive State Composables (useAuth, useProducts, useOrders, useTheme, useI18n)
    │   ├── middleware/             # Global Auth & Role Route Guards
    │   ├── pages/                  # Dynamic Page Routing (/login, /, /orders, /orders/create, /analytics)
    │   ├── plugins/                # Client boot plugins
    │   └── types/                  # TypeScript Data Models
    ├── nuxt.config.ts              # Nuxt 4 Configuration
    └── tailwind.config.js          # Tailwind CSS Configuration (darkMode: 'class')
```
