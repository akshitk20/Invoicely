# Invoicely — Implementation Plan

## What is Invoicely?

A SaaS invoicing + expense tracking + tax-ready reporting tool for Indian freelancers.

**One-liner:** Track invoices and expenses all year, export everything your CA needs in one click.

---

## Tech Stack

| Layer | Choice |
|-------|--------|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Build | Maven |
| Auth | Spring Security + Google OAuth2 |
| Database | PostgreSQL (Neon free tier) |
| Frontend | Thymeleaf + Bootstrap 5 + HTMX |
| PDF | OpenPDF |
| Excel | Apache POI |
| File storage | Local filesystem (MVP), S3 later |
| Hosting | Railway / Render |
| CI/CD | GitHub Actions |
| Payments (SaaS billing) | Razorpay |

---

## Project Structure

```
invoicely/
├── pom.xml
├── .gitignore
├── Dockerfile
├── .github/workflows/ci.yml
├── src/main/java/com/invoicely/
│   ├── InvoicelyApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── AppConfig.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Client.java
│   │   ├── Invoice.java
│   │   ├── LineItem.java
│   │   ├── Expense.java
│   │   └── enums/ (InvoiceStatus, ExpenseCategory, GstType)
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ClientRepository.java
│   │   ├── InvoiceRepository.java
│   │   ├── LineItemRepository.java
│   │   └── ExpenseRepository.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── ClientService.java
│   │   ├── InvoiceService.java
│   │   ├── ExpenseService.java
│   │   ├── GstCalculationService.java
│   │   ├── PdfGenerationService.java
│   │   ├── ReportService.java
│   │   └── ExportService.java
│   ├── controller/
│   │   ├── DashboardController.java
│   │   ├── InvoiceController.java
│   │   ├── ClientController.java
│   │   ├── ExpenseController.java
│   │   ├── ReportController.java
│   │   └── OnboardingController.java
│   └── dto/
│       ├── InvoiceCreateDto.java
│       ├── ExpenseCreateDto.java
│       └── ReportRequestDto.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   ├── schema.sql
│   ├── data.sql
│   ├── static/css/style.css
│   ├── static/js/app.js
│   └── templates/
│       ├── layout.html
│       ├── landing.html
│       ├── onboarding.html
│       ├── dashboard.html
│       ├── invoices/ (list, create, preview)
│       ├── expenses/ (list, create)
│       ├── clients/ (list, create)
│       └── reports/index.html
└── src/test/java/com/invoicely/
    ├── service/GstCalculationServiceTest.java
    └── InvoicelyApplicationTests.java
```

---

## Database Schema

```sql
-- users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    business_name VARCHAR(255),
    address TEXT,
    state VARCHAR(100),
    gstin VARCHAR(15),
    pan VARCHAR(10),
    default_sac_code VARCHAR(10),
    subscription_tier VARCHAR(20) DEFAULT 'FREE',
    onboarding_complete BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- clients
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    business_name VARCHAR(255),
    address TEXT,
    state VARCHAR(100) NOT NULL,
    gstin VARCHAR(15),
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- invoices
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    client_id BIGINT REFERENCES clients(id),
    invoice_number VARCHAR(50) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    cgst DECIMAL(12,2) DEFAULT 0,
    sgst DECIMAL(12,2) DEFAULT 0,
    igst DECIMAL(12,2) DEFAULT 0,
    total DECIMAL(12,2) NOT NULL,
    gst_rate DECIMAL(4,2) DEFAULT 18.00,
    sac_code VARCHAR(10),
    status VARCHAR(20) DEFAULT 'UNPAID',
    payment_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- line_items
CREATE TABLE line_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    rate DECIMAL(12,2) NOT NULL,
    amount DECIMAL(12,2) NOT NULL
);

-- expenses
CREATE TABLE expenses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    expense_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    receipt_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## Implementation Phases

### Phase 1: Project Setup + Auth (Week 1)
1. Scaffold Spring Boot project with all dependencies
2. Database schema + seed data (Indian states, SAC codes)
3. Google OAuth2 login
4. Onboarding flow (business profile)
5. Client CRUD

### Phase 2: Invoice Creation + GST Logic (Week 2)
6. GST calculation service (CGST/SGST vs IGST)
7. Invoice creation form (dynamic line items)
8. Invoice PDF generation (GST-compliant format)
9. Invoice list + mark as paid + overdue detection

### Phase 3: Expenses + Dashboard (Week 3)
10. Expense logging + receipt upload
11. Dashboard (monthly summary, GST breakdown, invoice status)

### Phase 4: Reports + Export (Week 4)
12. GST Summary Report (Excel)
13. Income Report (client-wise, month-wise)
14. Expense Report (category-wise)
15. CA Export Pack (ZIP with all reports + receipts)

### Phase 5: Deployment + Billing (Week 5)
16. Docker + GitHub Actions CI/CD
17. Razorpay subscription (Free/Pro/Business tiers)
18. Landing page (marketing + pricing)

---

## Pricing

| Tier | Price | Features |
|------|-------|----------|
| Free | ₹0 | 5 invoices/mo, basic dashboard |
| Pro | ₹499/mo | Unlimited invoices, GST reports, expense tracking |
| Business | ₹999/mo | TDS tracking, CA export, tax estimates, multi-business |
| Annual | 20% off | ₹4,999/yr Pro, ₹9,999/yr Business |

---

## GST Logic

- Same state → CGST (rate/2) + SGST (rate/2)
- Different state → IGST (rate)
- Export → 0%
- Default rate: 18% (configurable per invoice: 5%, 12%, 18%, 28%)

---

## Marketing Plan

1. LinkedIn posts (build in public)
2. SEO blog posts (GST invoice keywords)
3. Reddit/Twitter communities
4. Product Hunt launch
5. YouTube tutorials
6. Google Ads (₹500/day once profitable)
