-- V2__seed_demo_data.sql: FinCore Demo Seed Data

-- Demo Users
-- admin / Admin@123
-- john_doe / Customer@123
-- jane_smith / Customer@123
INSERT INTO users (id, username, email, password_hash, full_name, role, enabled, created_at, updated_at)
VALUES
(1, 'admin', 'admin@fincore.internal', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'System Administrator', 'ROLE_ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'john_doe', 'john.doe@fincore.demo', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'John Doe', 'ROLE_CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'jane_smith', 'jane.smith@fincore.demo', '$2a$10$7zBvY7p0oQh15dNuA9g0j.09QjS58qIu50PjD6hE8L6Q2L/6R.zSu', 'Jane Smith', 'ROLE_CUSTOMER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Demo Accounts
INSERT INTO accounts (id, account_number, user_id, account_type, currency, balance, status, created_at, updated_at)
VALUES
(1, 'ACC-10001', 2, 'CHECKING', 'INR', 75000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'ACC-10002', 2, 'SAVINGS', 'INR', 150000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'ACC-20001', 3, 'CHECKING', 'INR', 45000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Initial Demo Transaction (John transferring 5,000 INR from ACC-10001 to Jane ACC-20001)
INSERT INTO transactions (id, reference_number, source_account_id, destination_account_id, transaction_type, amount, currency, status, idempotency_key, description, created_at, completed_at)
VALUES
(1, 'TX-INIT-001', 1, 3, 'TRANSFER', 5000.00, 'INR', 'COMPLETED', 'IDEMP-INIT-001', 'Welcome settlement transfer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Balanced Double-Entry Journal for TX-INIT-001
-- Debit Source (ACC-10001): -5000.00
-- Credit Destination (ACC-20001): +5000.00
INSERT INTO journal_entries (id, transaction_id, account_id, entry_type, amount, balance_after, created_at)
VALUES
(1, 1, 1, 'DEBIT', 5000.00, 75000.00, CURRENT_TIMESTAMP),
(2, 1, 3, 'CREDIT', 5000.00, 45000.00, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Initial Demo Notification
INSERT INTO notifications (id, user_id, title, message, notification_type, is_read, created_at)
VALUES
(1, 2, 'Account Initialized', 'Welcome to FinCore. Your checking account ACC-10001 is active.', 'ACCOUNT_CREATED', FALSE, CURRENT_TIMESTAMP),
(2, 3, 'Credit Received', 'You received 5,000.00 INR from ACC-10001. Ref: TX-INIT-001', 'TRANSFER_RECEIVED', FALSE, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Update sequences if PostgreSQL
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('accounts_id_seq', (SELECT COALESCE(MAX(id), 1) FROM accounts));
SELECT setval('transactions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM transactions));
SELECT setval('journal_entries_id_seq', (SELECT COALESCE(MAX(id), 1) FROM journal_entries));
SELECT setval('notifications_id_seq', (SELECT COALESCE(MAX(id), 1) FROM notifications));
