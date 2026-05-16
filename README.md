# Invoicely

GST-compliant invoicing, expense tracking, and tax-ready reporting for Indian freelancers.

Invoicely helps freelancers and small businesses in India create professional GST invoices, track expenses, and generate everything their CA needs at tax time — all in one place.

## Features

- **Google OAuth Login** — No passwords to manage, sign in with your Google account
- **GST-Compliant Invoicing** — Auto-calculates CGST/SGST (intra-state) or IGST (inter-state) based on seller/buyer states
- **Professional PDF Invoices** — Download GST-compliant invoices with tax breakup, SAC codes, and all mandatory fields
- **Client Management** — Store client details (GSTIN, state, address) for quick invoice creation
- **Expense Tracking** — Log business expenses by category with optional receipt uploads
- **Reports & Exports**
  - GST Summary (Excel) — Monthly/quarterly tax breakdown for GSTR-1 filing
  - Income Report (Excel) — Client-wise income breakdown
  - Expense Report (Excel) — Category-wise expense breakdown
  - CA Export Pack (ZIP) — All reports + invoice PDFs + receipts in one download
- **Dashboard** — Monthly income, expenses, profit, GST collected, and invoice status at a glance
- **Auto Invoice Numbering** — Sequential per-year format: `INV-2026-001`
- **Overdue Detection** — Scheduled job auto-marks unpaid invoices past due date

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Build | Maven |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Auth | Spring Security + Google OAuth2 |
| Frontend | Thymeleaf + Bootstrap 5 |
| PDF Generation | OpenPDF |
| Excel Export | Apache POI |
| Testing | JUnit 5 + Mockito + H2 (in-memory) |
| Deployment | Docker + Render (free tier) |
| CI/CD | GitHub Actions |

## Project Structure

```
src/main/java/com/invoicely/
├── config/          # Security & app configuration
├── controller/      # MVC controllers (Dashboard, Invoice, Client, Expense, Report)
├── dto/             # Data transfer objects
├── model/           # JPA entities (User, Client, Invoice, LineItem, Expense)
│   └── enums/       # InvoiceStatus, ExpenseCategory, GstType
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic
    ├── GstCalculationService    # CGST/SGST/IGST determination
    ├── PdfGenerationService     # Invoice PDF creation
    ├── ReportService            # Excel report generation
    └── ExportService            # CA ZIP pack assembly
```

## GST Logic

| Scenario | Tax Applied |
|----------|-------------|
| Seller & buyer in same state | CGST (9%) + SGST (9%) |
| Seller & buyer in different states | IGST (18%) |
| Custom rate (5%, 12%, 28%) | Configurable per invoice |

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (or Docker)

### Local Development

1. **Start PostgreSQL:**
   ```bash
   docker run -d --name invoicely-db \
     -e POSTGRES_DB=invoicely \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 postgres:15-alpine
   ```

2. **Create `src/main/resources/application-dev.yml`** (gitignored):
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/invoicely
       username: postgres
       password: postgres
     jpa:
       hibernate:
         ddl-auto: update
       show-sql: true
     sql:
       init:
         mode: never
     security:
       oauth2:
         client:
           registration:
             google:
               client-id: YOUR_GOOGLE_CLIENT_ID
               client-secret: YOUR_GOOGLE_CLIENT_SECRET
               scope: openid, profile, email
   ```

3. **Run the app:**
   ```bash
   mvn spring-boot:run -DskipTests -s .mvn/local-settings.xml -Dspring-boot.run.profiles=dev
   ```

4. **Open:** http://localhost:9080

### Google OAuth Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Create an OAuth 2.0 Client ID (Web application)
3. Add authorized redirect URI: `http://localhost:9080/login/oauth2/code/google`
4. Copy the client ID and secret to your `application-dev.yml`

### Running Tests

```bash
mvn test -s .mvn/local-settings.xml
```

Tests use H2 in-memory database — no external dependencies needed.

## Deployment (Render — Free Tier)

This project includes a `render.yaml` blueprint for one-click deployment:

1. Push code to GitHub
2. Go to [render.com](https://render.com) → "New" → "Blueprint"
3. Connect your repo — Render auto-provisions PostgreSQL + web service
4. Set environment variables: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
5. Add your Render URL to Google OAuth authorized redirect URIs:
   ```
   https://your-app.onrender.com/login/oauth2/code/google
   ```

The free tier sleeps after 15 minutes of inactivity (first request after sleep takes ~30s).

## Screenshots

> TODO: Add screenshots of dashboard, invoice creation, and PDF output

## API / Routes

| Route | Description |
|-------|-------------|
| `/` | Landing page |
| `/dashboard` | Main dashboard (authenticated) |
| `/invoices` | Invoice list |
| `/invoices/new` | Create invoice |
| `/invoices/{id}` | Invoice preview |
| `/invoices/{id}/pdf` | Download invoice PDF |
| `/clients` | Client management |
| `/expenses` | Expense list |
| `/expenses/new` | Log expense |
| `/reports` | Reports & exports |
| `/reports/gst-summary` | Download GST Excel |
| `/reports/income` | Download Income Excel |
| `/reports/expenses` | Download Expense Excel |
| `/reports/ca-pack` | Download CA ZIP pack |

## License

MIT
