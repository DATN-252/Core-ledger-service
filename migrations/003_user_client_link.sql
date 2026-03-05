-- Migration 003: Link User accounts to Client profiles
-- Allows mobile customers to login using phoneNumber or idNumber

ALTER TABLE users
ADD COLUMN IF NOT EXISTS client_id BIGINT REFERENCES clients (id) ON DELETE SET NULL;

-- Optional: Unique constraint (one user per client)
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_client_id_key;

ALTER TABLE users
ADD CONSTRAINT users_client_id_key UNIQUE (client_id);

-- Insert sample CUSTOMER user linked to existing client (for testing)
-- Password: customer123 (bcrypt)
-- Replace with actual client IDs from your database
-- INSERT INTO users (username, password, full_name, role, enabled, client_id)
-- SELECT 'customer_' || c.id, '$2a$10$...bcrypt...', c.full_name, 'CUSTOMER', true, c.id
-- FROM clients c WHERE c.client_id = 'CLI_001';