-- created_by/updated_by = 0 indicates system-generated seed data
INSERT INTO fee_type (code, name, description, calculation_type, is_active, created_by, updated_by) VALUES
    ('FIXED_MONTHLY', 'Phí cố định hàng tháng', 'Thu phí cố định mỗi tháng', 'FIXED', true, 0, 0),
    ('TIERED_BALANCE', 'Phí theo bậc số dư', 'Thu phí theo bậc thang số dư bình quân', 'TIERED', true, 0, 0),
    ('PERCENTAGE_OF_BALANCE', 'Phí theo % số dư', 'Thu phí theo phần trăm số dư bình quân', 'PERCENTAGE', true, 0, 0)
ON CONFLICT (code) DO NOTHING; -- Avoid duplicate entries if the script is run multiple times