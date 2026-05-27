ALTER TABLE confirmation_request
ADD COLUMN response_deadline_hours INTEGER;

UPDATE confirmation_request
SET response_deadline_hours = 24
WHERE response_deadline_hours IS NULL;

ALTER TABLE confirmation_request
ALTER COLUMN response_deadline_hours SET NOT NULL;

ALTER TABLE confirmation_request
ADD CONSTRAINT chk_confirmation_request_response_deadline_hours_positive
CHECK (response_deadline_hours > 0);