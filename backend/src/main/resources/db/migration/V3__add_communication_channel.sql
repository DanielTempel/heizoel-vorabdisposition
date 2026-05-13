ALTER TABLE order_snapshot
    ALTER COLUMN customer_email DROP NOT NULL;

ALTER TABLE order_snapshot
    ADD COLUMN customer_phone_number VARCHAR(50);

ALTER TABLE confirmation_request
    ADD COLUMN communication_channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL';