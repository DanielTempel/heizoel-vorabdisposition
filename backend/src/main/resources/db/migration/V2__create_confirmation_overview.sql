CREATE OR REPLACE VIEW confirmation_overview AS
SELECT
    os.external_order_id,
    os.confirmation_status,

    os.customer_name,
    os.customer_email,
    os.delivery_address,
    os.product,
    os.quantity_liters,

    cr.id AS confirmation_request_id,
    cr.active AS confirmation_request_active,
    cr.delivery_date,
    cr.delivery_window_start,
    cr.delivery_window_end,
    cr.sent_at,
    cr.expires_at,
    cr.token,

    resp.id AS customer_response_id,
    resp.response_type,
    resp.comment AS customer_comment,
    resp.received_at AS customer_response_received_at

FROM order_snapshot os
LEFT JOIN confirmation_request cr
    ON cr.order_snapshot_id = os.id
LEFT JOIN customer_response resp
    ON resp.confirmation_request_id = cr.id;