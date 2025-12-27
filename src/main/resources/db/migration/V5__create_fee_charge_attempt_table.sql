CREATE TABLE fee_charge_attempt (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES customer_fee_job(id) ON DELETE RESTRICT,
    customer_id BIGINT NOT NULL,
    billing_month VARCHAR(7) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    attempt_no INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    external_txn_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT
);

CREATE INDEX idx_charge_attempt_job ON fee_charge_attempt(job_id);
CREATE INDEX idx_charge_attempt_status ON fee_charge_attempt(status);