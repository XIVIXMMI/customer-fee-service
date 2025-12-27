CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_by BIGINT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_customer_email ON customer(email);
CREATE INDEX idx_customer_status ON customer(status);

CREATE INDEX idx_customer_deleted_at ON customer(deleted_at) WHERE deleted_at IS NULL;
