-- created_by/updated_by = 0 indicates system-generated seed data

INSERT INTO customer (full_name, email, phone_number, status, created_by, updated_by) VALUES
    ('Nguyễn Văn An', 'nguyen.van.an@gmail.com', '0901234567', 'ACTIVE', 0, 0),
    ('Trần Thị Bình', 'tran.thi.binh@gmail.com', '0902345678', 'ACTIVE', 0, 0),
    ('Lê Văn Cường', 'le.van.cuong@gmail.com', '0903456789', 'ACTIVE', 0, 0),
    ('Phạm Thị Dung', 'pham.thi.dung@gmail.com', '0904567890', 'ACTIVE', 0, 0),
    ('Hoàng Văn Em', 'hoang.van.em@gmail.com', '0905678901', 'ACTIVE', 0, 0),
    ('Vũ Thị Phương', 'vu.thi.phuong@gmail.com', '0906789012', 'ACTIVE', 0, 0),
    ('Đặng Văn Giang', 'dang.van.giang@gmail.com', '0907890123', 'ACTIVE', 0, 0),
    ('Bùi Thị Hoa', 'bui.thi.hoa@gmail.com', '0908901234', 'INACTIVE', 0, 0),
    ('Ngô Văn Inh', 'ngo.van.inh@gmail.com', '0909012345', 'INACTIVE', 0, 0)
ON CONFLICT (email) DO NOTHING;

-- Customer 1: FIXED fee - 50,000 VND/month
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, created_by, updated_by)
VALUES
    (1, 1, 50000.00, 'VND', '2025-01-01', NULL, 0, 0)
ON CONFLICT DO NOTHING;

-- Customer 2: TIERED fee based on balance
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params, created_by, updated_by)
VALUES
    (2, 2, NULL, 'VND', '2025-01-01', NULL,
     '{"balance": 100000000, "tiers": [
         {"from": 0, "to": 50000000, "fee": 10000},
         {"from": 50000001, "to": 200000000, "fee": 20000},
         {"from": 200000001, "to": null, "fee": 50000}
     ]}'::jsonb,
     0, 0)
ON CONFLICT DO NOTHING;

-- Customer 3: PERCENTAGE fee - 0.1% of balance with min/max
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params, created_by, updated_by)
VALUES
    (3, 3, NULL, 'VND', '2025-01-01', NULL,
     '{"balance": 50000000, "percentage": 0.001, "min_fee": 5000, "max_fee": 100000}'::jsonb,
     0, 0)
ON CONFLICT DO NOTHING;

-- Customer 4: FIXED fee - 100,000 VND/month (premium customer)
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, created_by, updated_by)
VALUES
    (4, 1, 100000.00, 'VND', '2025-01-01', NULL, 0, 0)
ON CONFLICT DO NOTHING;

-- Customer 5: TIERED fee (high balance customer)
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params, created_by, updated_by)
VALUES
    (5, 2, NULL, 'VND', '2025-01-01', NULL,
     '{"balance": 300000000, "tiers": [
         {"from": 0, "to": 50000000, "fee": 10000},
         {"from": 50000001, "to": 200000000, "fee": 20000},
         {"from": 200000001, "to": null, "fee": 50000}
     ]}'::jsonb,
     0, 0)
ON CONFLICT DO NOTHING;

-- Customer 6: PERCENTAGE fee - 0.05% with higher max
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params, created_by, updated_by)
VALUES
    (6, 3, NULL, 'VND', '2025-01-01', NULL,
     '{"balance": 200000000, "percentage": 0.0005, "min_fee": 10000, "max_fee": 200000}'::jsonb,
     0, 0)
ON CONFLICT DO NOTHING;

-- Customer 7: FIXED fee - 30,000 VND/month (basic)
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, created_by, updated_by)
VALUES
    (7, 1, 30000.00, 'VND', '2025-01-01', NULL, 0, 0)
ON CONFLICT DO NOTHING;

-- Customer 8: Has config but should not be charged
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, created_by, updated_by)
VALUES
    (8, 1, 50000.00, 'VND', '2025-01-01', NULL, 0, 0)
ON CONFLICT DO NOTHING;


-- Customer 1 had different fee before 2025
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, created_by, updated_by)
VALUES
    (1, 1, 30000.00, 'VND', '2024-01-01', '2024-12-31', 0, 0)
ON CONFLICT DO NOTHING;

-- Customer 3 had FIXED fee before switching to PERCENTAGE
INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, created_by, updated_by)
VALUES
    (3, 1, 40000.00, 'VND', '2024-06-01', '2024-12-31', 0, 0)
ON CONFLICT DO NOTHING;

