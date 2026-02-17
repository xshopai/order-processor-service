-- =============================================================================
-- Add audit columns and performance metrics to saga table
-- =============================================================================

-- Add audit trail columns (idempotent - only add if not exists)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'created_by') THEN
        ALTER TABLE order_processing_saga ADD COLUMN created_by VARCHAR(255) DEFAULT 'system';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'updated_by') THEN
        ALTER TABLE order_processing_saga ADD COLUMN updated_by VARCHAR(255) DEFAULT 'system';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'version') THEN
        ALTER TABLE order_processing_saga ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
    END IF;
END $$;

-- Add performance metrics columns (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'payment_processing_started_at') THEN
        ALTER TABLE order_processing_saga ADD COLUMN payment_processing_started_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'payment_processing_completed_at') THEN
        ALTER TABLE order_processing_saga ADD COLUMN payment_processing_completed_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'inventory_processing_started_at') THEN
        ALTER TABLE order_processing_saga ADD COLUMN inventory_processing_started_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'inventory_processing_completed_at') THEN
        ALTER TABLE order_processing_saga ADD COLUMN inventory_processing_completed_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'shipping_processing_started_at') THEN
        ALTER TABLE order_processing_saga ADD COLUMN shipping_processing_started_at TIMESTAMP;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'shipping_processing_completed_at') THEN
        ALTER TABLE order_processing_saga ADD COLUMN shipping_processing_completed_at TIMESTAMP;
    END IF;
END $$;

-- Add correlation ID for distributed tracing (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'correlation_id') THEN
        ALTER TABLE order_processing_saga ADD COLUMN correlation_id VARCHAR(255);
    END IF;
END $$;

-- Create index on correlation ID for tracing queries (idempotent)
CREATE INDEX IF NOT EXISTS idx_order_processing_saga_correlation_id ON order_processing_saga(correlation_id);

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-update updated_at (drop first to make idempotent)
DROP TRIGGER IF EXISTS trg_order_processing_saga_updated_at ON order_processing_saga;
CREATE TRIGGER trg_order_processing_saga_updated_at
    BEFORE UPDATE ON order_processing_saga
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments (idempotent by nature)
COMMENT ON COLUMN order_processing_saga.version IS 'Optimistic locking version number';
COMMENT ON COLUMN order_processing_saga.correlation_id IS 'Distributed tracing correlation ID';
