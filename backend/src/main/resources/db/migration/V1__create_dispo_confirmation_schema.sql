CREATE TABLE order_snapshot (
    id BIGSERIAL PRIMARY KEY,

    external_order_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    delivery_address VARCHAR(1000) NOT NULL,
    product VARCHAR(255) NOT NULL,
    quantity_liters INTEGER NOT NULL,

    confirmation_status VARCHAR(50) NOT NULL,

    CONSTRAINT uk_order_snapshot_external_order_id
        UNIQUE (external_order_id)
);

CREATE TABLE confirmation_request (
    id BIGSERIAL PRIMARY KEY,

    order_snapshot_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,

    delivery_date DATE NOT NULL,
    delivery_window_start TIME NOT NULL,
    delivery_window_end TIME NOT NULL,

    active BOOLEAN NOT NULL,

    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_confirmation_request_token
        UNIQUE (token),

    CONSTRAINT fk_confirmation_request_order_snapshot
        FOREIGN KEY (order_snapshot_id)
        REFERENCES order_snapshot (id)
);

CREATE INDEX idx_confirmation_request_order_snapshot_id
    ON confirmation_request (order_snapshot_id);

CREATE TABLE customer_response (
    id BIGSERIAL PRIMARY KEY,

    confirmation_request_id BIGINT NOT NULL,
    response_type VARCHAR(50) NOT NULL,
    comment VARCHAR(2000),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_customer_response_confirmation_request
        UNIQUE (confirmation_request_id),

    CONSTRAINT fk_customer_response_confirmation_request
        FOREIGN KEY (confirmation_request_id)
        REFERENCES confirmation_request (id)
);