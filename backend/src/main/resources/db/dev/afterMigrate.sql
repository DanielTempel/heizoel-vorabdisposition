-- ============================================================
-- DEV DASHBOARD SEED
--
-- Runs after every Flyway migrate in the dev profile.
-- Deletes and recreates only records whose external_order_id
-- begins with DEMO-.
--
-- No Camunda processes are started.
-- ============================================================


-- ------------------------------------------------------------
-- 1. Fail early if the dev company does not exist
-- ------------------------------------------------------------

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM company
            WHERE id = 1
        ) THEN
            RAISE EXCEPTION
                'Dashboard seed requires company with id = 1.';
        END IF;
    END
$$;


-- ------------------------------------------------------------
-- 2. Remove previous seed data
-- ------------------------------------------------------------

DELETE FROM customer_response response
    USING confirmation_request request,
        order_snapshot order_data
WHERE response.confirmation_request_id = request.id
  AND request.order_snapshot_id = order_data.id
  AND order_data.company_id = 1
  AND order_data.external_order_id LIKE 'DEMO-%';


DELETE FROM confirmation_request request
    USING order_snapshot order_data
WHERE request.order_snapshot_id = order_data.id
  AND order_data.company_id = 1
  AND order_data.external_order_id LIKE 'DEMO-%';


DELETE FROM order_snapshot
WHERE company_id = 1
  AND external_order_id LIKE 'DEMO-%';


-- ------------------------------------------------------------
-- 3. Temporary source table
-- ------------------------------------------------------------

DROP TABLE IF EXISTS dashboard_seed_order_data;

CREATE TEMPORARY TABLE dashboard_seed_order_data
(
    external_order_id      VARCHAR(100) PRIMARY KEY,
    tour_number            VARCHAR(255) NOT NULL,
    vehicle_license_plate  VARCHAR(50)  NOT NULL,
    customer_name          VARCHAR(255) NOT NULL,
    customer_email         VARCHAR(255),
    customer_phone_number  VARCHAR(50),
    delivery_address       VARCHAR(1000) NOT NULL,
    product                VARCHAR(255) NOT NULL,
    quantity_liters        INTEGER NOT NULL,
    price_display_text     VARCHAR(100),
    delivery_day_offset    INTEGER NOT NULL,
    delivery_window_start  TIME NOT NULL,
    delivery_window_end    TIME NOT NULL,
    confirmation_status    VARCHAR(30) NOT NULL,
    communication_channel  VARCHAR(20) NOT NULL
) ON COMMIT DROP;


-- ------------------------------------------------------------
-- 4. Hand-crafted data
-- ------------------------------------------------------------

INSERT INTO dashboard_seed_order_data
(
    external_order_id,
    tour_number,
    vehicle_license_plate,
    customer_name,
    customer_email,
    customer_phone_number,
    delivery_address,
    product,
    quantity_liters,
    price_display_text,
    delivery_day_offset,
    delivery_window_start,
    delivery_window_end,
    confirmation_status,
    communication_channel
)
VALUES

-- ============================================================
-- Yesterday: hidden by the default dateFrom = today
-- ============================================================

(
    'DEMO-OLD-001',
    'OLD-1',
    'WÜ-DEMO 090',
    'Gestern GmbH',
    'gestern@example.com',
    '+491700000090',
    'Altstadt 1, 97070 Würzburg',
    'Heizöl schwefelarm',
    2400,
    '96,40 € / 100 L',
    -1,
    TIME '08:00',
    TIME '09:00',
    'CONFIRMED',
    'EMAIL'
),
(
    'DEMO-OLD-002',
    'OLD-1',
    'WÜ-DEMO 090',
    'Keine Antwort Alt',
    'keine-antwort-alt@example.com',
    '+491700000091',
    'Altstadt 2, 97070 Würzburg',
    'Heizöl schwefelarm',
    1800,
    '96,40 € / 100 L',
    -1,
    TIME '09:30',
    TIME '10:30',
    'NO_RESPONSE',
    'SMS'
),


-- ============================================================
-- Today: Tour A-17
--
-- Counts:
-- SENT       = 0
-- CONFIRMED  = 2
-- REJECTED   = 2
-- NO_RESPONSE = 1
-- ============================================================

(
    'DEMO-TODAY-001',
    'A-17',
    'WÜ-DEMO 100',
    'Max Müller',
    'max.mueller@example.com',
    '+491700001001',
    'Musterstraße 10, 97070 Würzburg',
    'Heizöl schwefelarm',
    3000,
    '95,90 € / 100 L',
    0,
    TIME '08:00',
    TIME '09:00',
    'CONFIRMED',
    'EMAIL'
),
(
    'DEMO-TODAY-002',
    'A-17',
    'WÜ-DEMO 100',
    'Anna Schmidt',
    'anna.schmidt@example.com',
    '+491700001002',
    'Theaterstraße 5, 97070 Würzburg',
    'Heizöl schwefelarm',
    2500,
    '95,90 € / 100 L',
    0,
    TIME '09:30',
    TIME '10:30',
    'REJECTED',
    'EMAIL'
),
(
    'DEMO-TODAY-003',
    'A-17',
    'WÜ-DEMO 100',
    'Peter Weber',
    'peter.weber@example.com',
    '+491700001003',
    'Sanderstraße 12, 97070 Würzburg',
    'Heizöl schwefelarm',
    2100,
    '95,90 € / 100 L',
    0,
    TIME '11:00',
    TIME '12:00',
    'NO_RESPONSE',
    'SMS'
),
(
    'DEMO-TODAY-004',
    'A-17',
    'WÜ-DEMO 100',
    'Julia Fischer',
    'julia.fischer@example.com',
    '+491700001004',
    'Haugerpfarrgasse 7, 97070 Würzburg',
    'Heizöl schwefelarm',
    3400,
    '95,90 € / 100 L',
    0,
    TIME '13:00',
    TIME '14:00',
    'CONFIRMED',
    'WHATSAPP'
),
(
    'DEMO-TODAY-005',
    'A-17',
    'WÜ-DEMO 100',
    'Emre Özdemir',
    'emre.oezdemir@example.com',
    '+491700001005',
    'Semmelstraße 25, 97070 Würzburg',
    'Heizöl schwefelarm',
    1900,
    '95,90 € / 100 L',
    0,
    TIME '15:00',
    TIME '16:00',
    'REJECTED',
    'EMAIL'
),


-- ============================================================
-- Today: Tour BETA-2
--
-- Two orders begin at 08:00.
-- 08:00-09:00 must be shown before 08:00-10:00.
-- ============================================================

(
    'DEMO-BETA-001',
    'BETA-2',
    'WÜ-DEMO 200',
    'Martin Keller',
    'martin.keller@example.com',
    '+491700002001',
    'Röntgenring 20, 97070 Würzburg',
    'Heizöl schwefelarm',
    2700,
    '96,10 € / 100 L',
    0,
    TIME '08:00',
    TIME '10:00',
    'CONFIRMED',
    'EMAIL'
),
(
    'DEMO-BETA-002',
    'BETA-2',
    'WÜ-DEMO 200',
    'Sabine Braun',
    'sabine.braun@example.com',
    '+491700002002',
    'Röntgenring 22, 97070 Würzburg',
    'Heizöl schwefelarm',
    2300,
    '96,10 € / 100 L',
    0,
    TIME '08:00',
    TIME '09:00',
    'REJECTED',
    'SMS'
),
(
    'DEMO-BETA-003',
    'BETA-2',
    'WÜ-DEMO 200',
    'Thomas Lang',
    'thomas.lang@example.com',
    '+491700002003',
    'Röntgenring 24, 97070 Würzburg',
    'Heizöl schwefelarm',
    3100,
    '96,10 € / 100 L',
    0,
    TIME '10:00',
    TIME '11:00',
    'NO_RESPONSE',
    'EMAIL'
),


-- ============================================================
-- Tomorrow: one order for every status
-- ============================================================

(
    'DEMO-TOMORROW-001',
    'NORD-3',
    'WÜ-N 303',
    'Lea Becker',
    'lea.becker@example.com',
    '+491700003001',
    'Nordring 3, 97080 Würzburg',
    'Heizöl Premium',
    2600,
    '97,20 € / 100 L',
    1,
    TIME '07:30',
    TIME '08:30',
    'SENT',
    'EMAIL'
),
(
    'DEMO-TOMORROW-002',
    'NORD-3',
    'WÜ-N 303',
    'Tom Winter',
    'tom.winter@example.com',
    '+491700003002',
    'Nordring 7, 97080 Würzburg',
    'Heizöl Premium',
    2200,
    '97,20 € / 100 L',
    1,
    TIME '09:00',
    TIME '10:00',
    'CONFIRMED',
    'SMS'
),
(
    'DEMO-TOMORROW-003',
    'NORD-3',
    'WÜ-N 303',
    'Maria Sommer',
    'maria.sommer@example.com',
    '+491700003003',
    'Nordring 11, 97080 Würzburg',
    'Heizöl Premium',
    2800,
    '97,20 € / 100 L',
    1,
    TIME '10:30',
    TIME '11:30',
    'REJECTED',
    'WHATSAPP'
),
(
    'DEMO-TOMORROW-004',
    'NORD-3',
    'WÜ-N 303',
    'Felix Roth',
    'felix.roth@example.com',
    '+491700003004',
    'Nordring 15, 97080 Würzburg',
    'Heizöl Premium',
    2000,
    '97,20 € / 100 L',
    1,
    TIME '12:00',
    TIME '13:00',
    'NO_RESPONSE',
    'EMAIL'
),


-- ============================================================
-- Day after tomorrow
-- ============================================================

(
    'DEMO-ALPHA-001',
    'T-ALPHA',
    'SW-AL 42',
    'Alpha Kunde',
    'alpha@example.com',
    '+491700004001',
    'Hauptstraße 1, 97421 Schweinfurt',
    'Heizöl schwefelarm',
    3200,
    '95,70 € / 100 L',
    2,
    TIME '08:00',
    TIME '09:00',
    'SENT',
    'EMAIL'
),
(
    'DEMO-ALPHA-002',
    'T-ALPHA',
    'SW-AL 42',
    'Spezialadresse Suche',
    'hafen@example.com',
    '+491700004002',
    'Am Hafen 77, 97421 Schweinfurt',
    'Heizöl schwefelarm',
    2900,
    '95,70 € / 100 L',
    2,
    TIME '10:00',
    TIME '11:00',
    'CONFIRMED',
    'EMAIL'
),


-- ============================================================
-- Same date, string tour numbers
-- ============================================================

(
    'DEMO-T10-001',
    'T-10',
    'WÜ-T 010',
    'Tour Zehn Kunde',
    'tour10@example.com',
    '+491700005010',
    'Teststraße 10, 97074 Würzburg',
    'Heizöl schwefelarm',
    2400,
    '96,00 € / 100 L',
    3,
    TIME '08:00',
    TIME '09:00',
    'REJECTED',
    'EMAIL'
),
(
    'DEMO-T2-001',
    'T-2',
    'WÜ-T 002',
    'Tour Zwei Kunde',
    'tour2@example.com',
    '+491700005002',
    'Teststraße 2, 97074 Würzburg',
    'Heizöl schwefelarm',
    2400,
    '96,00 € / 100 L',
    3,
    TIME '09:00',
    TIME '10:00',
    'SENT',
    'EMAIL'
);


-- ------------------------------------------------------------
-- 5. Additional tours for pagination
--
-- 18 additional tours.
-- Together with the six non-historical tours above:
-- default Dashboard result = 24 tours.
--
-- Page size 20:
-- page 0 = 20 tours
-- page 1 = 4 tours
-- ------------------------------------------------------------

INSERT INTO dashboard_seed_order_data
(
    external_order_id,
    tour_number,
    vehicle_license_plate,
    customer_name,
    customer_email,
    customer_phone_number,
    delivery_address,
    product,
    quantity_liters,
    price_display_text,
    delivery_day_offset,
    delivery_window_start,
    delivery_window_end,
    confirmation_status,
    communication_channel
)
SELECT
    'DEMO-PAGE-' || LPAD(series_number::TEXT, 2, '0'),
    'P-' || LPAD(series_number::TEXT, 2, '0'),
    'WÜ-P ' || LPAD((1000 + series_number)::TEXT, 4, '0'),
    'Pagination Kunde ' || LPAD(series_number::TEXT, 2, '0'),
    'pagination-' || series_number || '@example.com',
    '+49171' || LPAD(series_number::TEXT, 7, '0'),
    'Testweg ' || series_number || ', 97074 Würzburg',
    'Heizöl schwefelarm',
    1800 + series_number * 100,
    '96,50 € / 100 L',
    4 + series_number,
    (
        TIME '07:00'
            + ((series_number - 1) % 8) * INTERVAL '1 hour'
        )::TIME,
    (
        TIME '07:45'
            + ((series_number - 1) % 8) * INTERVAL '1 hour'
        )::TIME,
    CASE series_number % 4
        WHEN 1 THEN 'SENT'
        WHEN 2 THEN 'CONFIRMED'
        WHEN 3 THEN 'REJECTED'
        ELSE 'NO_RESPONSE'
        END,
    CASE series_number % 3
        WHEN 1 THEN 'EMAIL'
        WHEN 2 THEN 'SMS'
        ELSE 'WHATSAPP'
        END
FROM generate_series(1, 18) AS generated(series_number);


-- ------------------------------------------------------------
-- 6. Insert Order snapshots
-- ------------------------------------------------------------

INSERT INTO order_snapshot
(
    company_id,
    external_order_id,
    tour_number,
    vehicle_license_plate,
    customer_name,
    customer_email,
    customer_phone_number,
    delivery_address,
    product,
    quantity_liters,
    price_display_text,
    confirmation_status
)
SELECT
    1,
    seed.external_order_id,
    seed.tour_number,
    seed.vehicle_license_plate,
    seed.customer_name,
    seed.customer_email,
    seed.customer_phone_number,
    seed.delivery_address,
    seed.product,
    seed.quantity_liters,
    seed.price_display_text,
    seed.confirmation_status
FROM dashboard_seed_order_data seed;


-- ------------------------------------------------------------
-- 7. Historical request for latest-request filtering
--
-- DEMO-TODAY-001 receives an old REJECTED request first.
-- A current CONFIRMED request is inserted afterwards.
--
-- Dashboard must display only the current request.
-- ------------------------------------------------------------

INSERT INTO confirmation_request
(
    token,
    order_snapshot_id,
    communication_channel,
    delivery_date,
    delivery_window_start,
    delivery_window_end,
    active,
    sent_at,
    expires_at,
    response_deadline_hours
)
SELECT
    'demo-history-demo-today-001',
    order_data.id,
    'EMAIL',
    CURRENT_DATE - 1,
    TIME '14:00',
    TIME '15:00',
    FALSE,
    (
        (CURRENT_DATE - 2 + TIME '12:00')
            AT TIME ZONE 'Europe/Berlin'
        ),
    (
        (CURRENT_DATE - 1 + TIME '14:00')
            AT TIME ZONE 'Europe/Berlin'
        ),
    24
FROM order_snapshot order_data
WHERE order_data.company_id = 1
  AND order_data.external_order_id = 'DEMO-TODAY-001';


-- ------------------------------------------------------------
-- 8. Insert current ConfirmationRequests
-- ------------------------------------------------------------

WITH request_base AS
         (
             SELECT
                 order_data.id AS order_snapshot_id,
                 seed.*,
                 (
                     (
                                 CURRENT_DATE
                             + seed.delivery_day_offset
                             + seed.delivery_window_start
                         )
                         AT TIME ZONE 'Europe/Berlin'
                     ) AS delivery_starts_at
             FROM dashboard_seed_order_data seed
                      JOIN order_snapshot order_data
                           ON order_data.company_id = 1
                               AND order_data.external_order_id = seed.external_order_id
         ),
     request_times AS
         (
             SELECT
                 request_base.*,
                 LEAST(
                                 CURRENT_TIMESTAMP - INTERVAL '2 hours',
                                 request_base.delivery_starts_at - INTERVAL '1 hour'
                 ) AS calculated_sent_at
             FROM request_base
         )
INSERT INTO confirmation_request
(
    token,
    order_snapshot_id,
    communication_channel,
    delivery_date,
    delivery_window_start,
    delivery_window_end,
    active,
    sent_at,
    expires_at,
    response_deadline_hours
)
SELECT
    'demo-current-' || LOWER(request_times.external_order_id),
    request_times.order_snapshot_id,
    request_times.communication_channel,
    CURRENT_DATE + request_times.delivery_day_offset,
    request_times.delivery_window_start,
    request_times.delivery_window_end,

    request_times.confirmation_status = 'SENT',

    request_times.calculated_sent_at,

    CASE request_times.confirmation_status
        WHEN 'NO_RESPONSE' THEN
            LEAST(
                            CURRENT_TIMESTAMP - INTERVAL '1 hour',
                            request_times.delivery_starts_at
            )

        WHEN 'SENT' THEN
            LEAST(
                            CURRENT_TIMESTAMP + INTERVAL '12 hours',
                            request_times.delivery_starts_at
            )

        ELSE
            LEAST(
                    request_times.calculated_sent_at + INTERVAL '24 hours',
                    request_times.delivery_starts_at
            )
        END,

    24
FROM request_times;


-- ------------------------------------------------------------
-- 9. Insert responses for current CONFIRMED / REJECTED requests
-- ------------------------------------------------------------

INSERT INTO customer_response
(
    confirmation_request_id,
    response_type,
    comment,
    received_at
)
SELECT
    request.id,

    CASE order_data.confirmation_status
        WHEN 'CONFIRMED' THEN 'CONFIRM'
        WHEN 'REJECTED' THEN 'REJECT'
        END,

    CASE
        WHEN order_data.external_order_id = 'DEMO-TODAY-002'
            THEN 'Bitte erst ab 15 Uhr.'

        WHEN order_data.external_order_id = 'DEMO-TODAY-005'
            THEN 'Der vorgeschlagene Termin passt leider nicht.'

        WHEN order_data.confirmation_status = 'REJECTED'
            THEN 'Bitte einen anderen Liefertermin vorschlagen.'

        END,

    request.sent_at + INTERVAL '30 minutes'

FROM confirmation_request request
         JOIN order_snapshot order_data
              ON order_data.id = request.order_snapshot_id

WHERE order_data.company_id = 1
  AND order_data.external_order_id LIKE 'DEMO-%'
  AND request.token LIKE 'demo-current-%'
  AND order_data.confirmation_status IN (
                                         'CONFIRMED',
                                         'REJECTED'
    );


-- ------------------------------------------------------------
-- 10. Response for the historical request
-- ------------------------------------------------------------

INSERT INTO customer_response
(
    confirmation_request_id,
    response_type,
    comment,
    received_at
)
SELECT
    request.id,
    'REJECT',
    'Historische Ablehnung vor der neuen Anfrage.',
    request.sent_at + INTERVAL '2 hours'
FROM confirmation_request request
WHERE request.token = 'demo-history-demo-today-001';

INSERT INTO company_email_settings (
    company_id,
    smtp_host,
    smtp_port,
    security_mode,
    authentication_enabled,
    smtp_username,
    smtp_password_encrypted,
    from_address,
    from_name,
    updated_at
)
SELECT
    c.id,
    'localhost',
    1025,
    'NONE',
    FALSE,
    NULL,
    NULL,
    'dispo@heizoel.local',
    'Heizöl Disposition',
    CURRENT_TIMESTAMP
FROM company c
WHERE c.id = 1
ON CONFLICT (company_id) DO NOTHING;
