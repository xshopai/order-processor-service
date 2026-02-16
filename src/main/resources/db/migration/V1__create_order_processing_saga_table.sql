-- =============================================================================
-- Order Processing Saga Table
-- =============================================================================
-- This table tracks the state of order processing sagas in the choreography pattern
-- Each saga represents an order going through payment, inventory, and shipping steps

CREATE TABLE IF NOT EXISTS order_processing_saga (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Order information
    order_id UUID NOT NULL UNIQUE,
    order_number VARCHAR(50) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    
    -- Financial information
    total_amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    
    -- Saga state tracking
    status VARCHAR(50) NOT NULL,
    current_step VARCHAR(50) NOT NULL,
    
    -- External service IDs (set as saga progresses)
    payment_id VARCHAR(255),
    inventory_reservation_id VARCHAR(255),
    shipping_id VARCHAR(255),
    
    -- Retry tracking
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    
    -- Error tracking
    error_message TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_total_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_max_retries CHECK (max_retries >= 0)
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_order_processing_saga_order_id ON order_processing_saga(order_id);
CREATE INDEX IF NOT EXISTS idx_order_processing_saga_customer_id ON order_processing_saga(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_processing_saga_status ON order_processing_saga(status);
CREATE INDEX IF NOT EXISTS idx_order_processing_saga_created_at ON order_processing_saga(created_at);
CREATE INDEX IF NOT EXISTS idx_order_processing_saga_updated_at ON order_processing_saga(updated_at);

-- Composite index for finding stuck sagas
CREATE INDEX IF NOT EXISTS idx_order_processing_saga_status_updated ON order_processing_saga(status, updated_at);

-- Comments for documentation
COMMENT ON TABLE order_processing_saga IS 'Tracks the state of order processing sagas using choreography pattern';
COMMENT ON COLUMN order_processing_saga.status IS 'Values: STARTED, PAYMENT_PROCESSING, INVENTORY_PROCESSING, SHIPPING_PROCESSING, COMPLETED, FAILED, COMPENSATING, COMPENSATED';
COMMENT ON COLUMN order_processing_saga.current_step IS 'Values: STARTED, PAYMENT_PROCESSING, INVENTORY_PROCESSING, SHIPPING_PROCESSING, COMPLETED, FAILED';
