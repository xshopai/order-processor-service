-- =============================================================================
-- Saga Event Log Table
-- =============================================================================
-- Tracks all events processed by the saga for audit and debugging purposes

CREATE TABLE IF NOT EXISTS saga_event_log (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Link to saga
    saga_id UUID NOT NULL,
    order_id UUID NOT NULL,
    
    -- Event information
    event_type VARCHAR(100) NOT NULL,
    event_payload JSONB NOT NULL,
    event_source VARCHAR(100) NOT NULL,
    
    -- Event metadata
    correlation_id VARCHAR(255),
    trace_id VARCHAR(255),
    
    -- Processing information
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT
);

-- Add foreign key constraint if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_saga_event_log_saga' 
        AND table_name = 'saga_event_log'
    ) THEN
        ALTER TABLE saga_event_log
            ADD CONSTRAINT fk_saga_event_log_saga 
            FOREIGN KEY (saga_id) 
            REFERENCES order_processing_saga(id) 
            ON DELETE CASCADE;
    END IF;
END $$;

-- Create indexes (idempotent)
CREATE INDEX IF NOT EXISTS idx_saga_event_log_saga_id ON saga_event_log(saga_id);
CREATE INDEX IF NOT EXISTS idx_saga_event_log_order_id ON saga_event_log(order_id);
CREATE INDEX IF NOT EXISTS idx_saga_event_log_event_type ON saga_event_log(event_type);
CREATE INDEX IF NOT EXISTS idx_saga_event_log_processed_at ON saga_event_log(processed_at);
CREATE INDEX IF NOT EXISTS idx_saga_event_log_correlation_id ON saga_event_log(correlation_id);

-- Composite index for saga timeline queries
CREATE INDEX IF NOT EXISTS idx_saga_event_log_saga_processed ON saga_event_log(saga_id, processed_at);

-- Comments
COMMENT ON TABLE saga_event_log IS 'Audit log of all events processed by sagas';
COMMENT ON COLUMN saga_event_log.event_type IS 'Type of event: OrderCreated, PaymentProcessed, InventoryReserved, etc.';
COMMENT ON COLUMN saga_event_log.processing_status IS 'Values: SUCCESS, FAILED, RETRY';
COMMENT ON COLUMN saga_event_log.event_payload IS 'Full event data as JSON for debugging';
