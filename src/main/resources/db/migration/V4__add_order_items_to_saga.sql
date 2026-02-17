-- V4: Add order items and addresses to saga for event-driven architecture
-- This allows the saga to store all necessary data from OrderCreatedEvent
-- without needing to make HTTP calls to other services

-- Add columns idempotently
DO $$
BEGIN
    -- Add order items as JSON (contains product details, quantities, prices)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'order_items') THEN
        ALTER TABLE order_processing_saga ADD COLUMN order_items JSONB;
    END IF;
    
    -- Add shipping address as JSON
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'shipping_address') THEN
        ALTER TABLE order_processing_saga ADD COLUMN shipping_address JSONB;
    END IF;
    
    -- Add billing address as JSON
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'order_processing_saga' AND column_name = 'billing_address') THEN
        ALTER TABLE order_processing_saga ADD COLUMN billing_address JSONB;
    END IF;
END $$;

-- Add index for querying by product in order items (idempotent)
CREATE INDEX IF NOT EXISTS idx_order_processing_saga_order_items 
ON order_processing_saga USING GIN (order_items);

-- Add comments
COMMENT ON COLUMN order_processing_saga.order_items IS 'Order items from OrderCreatedEvent - prevents need for HTTP calls to Order Service';
COMMENT ON COLUMN order_processing_saga.shipping_address IS 'Shipping address from OrderCreatedEvent';
COMMENT ON COLUMN order_processing_saga.billing_address IS 'Billing address from OrderCreatedEvent';
