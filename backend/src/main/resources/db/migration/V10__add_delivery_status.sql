ALTER TABLE confirmation_request
    ADD COLUMN delivery_status VARCHAR(20) NOT NULL DEFAULT 'SENT';

ALTER TABLE confirmation_request
    ALTER COLUMN delivery_status DROP DEFAULT;

ALTER TABLE confirmation_request
    ALTER COLUMN sent_at DROP NOT NULL;

ALTER TABLE confirmation_request
    ALTER COLUMN expires_at DROP NOT NULL;