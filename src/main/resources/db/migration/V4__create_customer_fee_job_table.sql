CREATE TABLE customer_fee_job (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    billing_month VARCHAR(7) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_fee_job_customer ON customer_fee_job(customer_id);
CREATE INDEX idx_fee_job_billing_month ON customer_fee_job(billing_month);
CREATE INDEX idx_fee_job_status ON customer_fee_job(status);
CREATE UNIQUE INDEX idx_fee_job_idempotency ON customer_fee_job(idempotency_key);

CREATE INDEX idx_customer_fee_job_deleted_at ON customer_fee_job(deleted_at) WHERE deleted_at IS NULL;