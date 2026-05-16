CREATE TABLE IF NOT EXISTS users (
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

CREATE TABLE IF NOT EXISTS clients (
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

CREATE TABLE IF NOT EXISTS invoices (
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

CREATE TABLE IF NOT EXISTS line_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    rate DECIMAL(12,2) NOT NULL,
    amount DECIMAL(12,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    expense_date DATE NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    receipt_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);
