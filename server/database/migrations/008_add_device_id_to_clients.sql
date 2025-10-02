-- Migration: Add device_id column to clients table
-- This enables proper device limit enforcement by tracking unique hardware IDs

ALTER TABLE clients
ADD COLUMN device_id VARCHAR(255) AFTER platform;

-- Add index for device_id lookups
CREATE INDEX idx_clients_device_id ON clients(device_id);

-- Add composite index for subscription + device_id queries (device limit checks)
CREATE INDEX idx_clients_subscription_device ON clients(subscription_id, device_id);
