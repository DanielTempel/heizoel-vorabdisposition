ALTER TABLE order_snapshot
    ADD COLUMN tour_number VARCHAR(100) NOT NULL;

ALTER TABLE order_snapshot
    ADD COLUMN vehicle_license_plate VARCHAR(50) NOT NULL;

CREATE INDEX idx_order_snapshot_company_tour_number
    ON order_snapshot (company_id, tour_number);