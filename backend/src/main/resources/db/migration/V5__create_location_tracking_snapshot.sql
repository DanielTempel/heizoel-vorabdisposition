CREATE TABLE location_tracking_snapshot (
    id BIGSERIAL PRIMARY KEY,
    external_order_id VARCHAR(100) NOT NULL,
    confirmation_token VARCHAR(255),
    delivery_address VARCHAR(1000) NOT NULL,
    location_x DOUBLE PRECISION NOT NULL,
    location_y DOUBLE PRECISION NOT NULL,
    target_location_x DOUBLE PRECISION NOT NULL,
    target_location_y DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_location_tracking_snapshot_external_order_id
        UNIQUE (external_order_id),
    CONSTRAINT uk_location_tracking_snapshot_confirmation_token
        UNIQUE (confirmation_token)
);
