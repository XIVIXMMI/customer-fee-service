-- Make amount column nullable (amount chỉ có giá trị khi job DONE)
ALTER TABLE customer_fee_job
ALTER COLUMN amount DROP NOT NULL;

-- Add comment
COMMENT ON COLUMN customer_fee_job.amount IS 'Amount to charge (null when status=NEW, has value when status=DONE)';
