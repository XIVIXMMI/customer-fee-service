CREATE TABLE customer_fee_config (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    fee_type_id BIGINT NOT NULL REFERENCES fee_type(id) ON DELETE RESTRICT,
    monthly_fee_amount NUMERIC(15,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    effective_from DATE NOT NULL,
    effective_to DATE,
    calculation_params JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT check_effective_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX idx_customer_fee_config_customer ON customer_fee_config(customer_id);
CREATE INDEX idx_customer_fee_config_dates ON customer_fee_config(effective_from, effective_to);
CREATE INDEX idx_customer_fee_config_deleted_at ON customer_fee_config(deleted_at) WHERE deleted_at IS NULL;
