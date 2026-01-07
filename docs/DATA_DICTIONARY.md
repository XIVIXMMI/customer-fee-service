# Customer Fee Service - Data Dictionary & Database Schema

## Tổng quan

Document này mô tả chi tiết cấu trúc database, data dictionary cho từng bảng, và mô hình quan hệ giữa các bảng.

**Database:** PostgreSQL 14+
**Schema:** public
**Migration Tool:** Flyway
**ORM:** Hibernate (JPA)

---

## Entity Relationship Diagram (ERD)

```
┌─────────────────────────────────┐
│         CUSTOMER                │
│─────────────────────────────────│
│ PK  id (BIGINT)                 │
│     full_name (VARCHAR)         │
│     email (VARCHAR) UNIQUE      │
│     phone_number (VARCHAR)      │
│     status (VARCHAR)            │
│     created_at (TIMESTAMP)      │
│     updated_at (TIMESTAMP)      │
│     created_by (BIGINT)         │
│     updated_by (BIGINT)         │
│     deleted_at (TIMESTAMP)      │
│     deleted_by (BIGINT)         │
└────────────┬────────────────────┘
             │
             │ 1:N
             │
             ▼
┌─────────────────────────────────┐         ┌─────────────────────────────────┐
│   CUSTOMER_FEE_CONFIG           │   N:1   │        FEE_TYPE                 │
│─────────────────────────────────│◄────────│─────────────────────────────────│
│ PK  id (BIGINT)                 │         │ PK  id (BIGINT)                 │
│ FK  customer_id (BIGINT) ────┐  │         │     code (VARCHAR) UNIQUE       │
│ FK  fee_type_id (BIGINT) ────┼──┼────────►│     name (VARCHAR)              │
│     monthly_fee_amount (DECIMAL)│         │     description (TEXT)          │
│     currency (VARCHAR)          │         │     calculation_type (VARCHAR)  │
│     effective_from (DATE)       │         │     is_active (BOOLEAN)         │
│     effective_to (DATE)         │         │     created_at (TIMESTAMP)      │
│     calculation_params (JSONB)  │         │     updated_at (TIMESTAMP)      │
│     created_at (TIMESTAMP)      │         └─────────────────────────────────┘
│     updated_at (TIMESTAMP)      │
│     created_by (BIGINT)         │
│     updated_by (BIGINT)         │
│     deleted_at (TIMESTAMP)      │
│     deleted_by (BIGINT)         │
│     version (BIGINT)            │
└────────────┬────────────────────┘
             │
             │
             └──────────────────┐
                                │ 1:N
                                │
                                ▼
             ┌─────────────────────────────────┐
             │   CUSTOMER_FEE_JOB              │
             │─────────────────────────────────│
             │ PK  id (BIGINT)                 │
             │ FK  customer_id (BIGINT) ───────┼──┐
             │     billing_month (VARCHAR)     │  │
             │     amount (DECIMAL) NULLABLE   │  │
             │     status (VARCHAR)            │  │
             │     idempotency_key (VARCHAR)   │  │
             │     created_at (TIMESTAMP)      │  │
             │     updated_at (TIMESTAMP)      │  │
             │     deleted_at (TIMESTAMP)      │  │
             │     version (BIGINT)            │  │
             └────────────┬────────────────────┘  │
                          │                       │
                          │ 1:N                   │
                          │                       │
                          ▼                       │
             ┌─────────────────────────────────┐  │
             │   FEE_CHARGE_ATTEMPT            │  │
             │─────────────────────────────────│  │
             │ PK  id (BIGINT)                 │  │
             │ FK  job_id (BIGINT)             │  │
             │     customer_id (BIGINT)        │  │
             │     billing_month (VARCHAR)     │  │
             │     amount (DECIMAL)            │  │
             │     attempt_no (INTEGER)        │  │
             │     status (VARCHAR)            │  │
             │     error_code (VARCHAR)        │  │
             │     error_message (TEXT)        │  │
             │     external_txn_id (VARCHAR)   │  │
             │     created_at (TIMESTAMP)      │  │
             └─────────────────────────────────┘  │
                                                  │
                 (Customer reference) ◄───────────┘
```

---

## Table Descriptions

### 1. CUSTOMER

**Purpose:** Lưu trữ thông tin khách hàng

**Business Rules:**
- Email phải unique
- Soft delete: không xóa vật lý, chỉ set `deleted_at`
- Status: ACTIVE hoặc INACTIVE
- Chỉ ACTIVE customers mới được tính phí

---

### 2. FEE_TYPE

**Purpose:** Master data định nghĩa các loại phí (read-only)

**Business Rules:**
- Code phải unique
- Là reference data, không cho phép tạo/sửa/xóa qua API
- Được seed sẵn qua Flyway migration
- Chỉ active fee types được sử dụng

**Supported Fee Types:**
1. **FIXED_MONTHLY** - Phí cố định hàng tháng
2. **TIERED_BALANCE** - Phí theo bậc số dư
3. **PERCENTAGE_OF_BALANCE** - Phí theo % số dư

---

### 3. CUSTOMER_FEE_CONFIG

**Purpose:** Cấu hình phí cho từng customer trong từng khoảng thời gian

**Business Rules:**
- Một customer chỉ có 1 config active tại 1 thời điểm
- Không được overlap về khoảng thời gian
- `effective_from` < `effective_to` (nếu có `effective_to`)
- `effective_to` = null nghĩa là vô thời hạn
- `calculation_params` là JSONB, cấu trúc phụ thuộc vào `fee_type`
- Soft delete
- Optimistic locking với `version` field

**calculation_params Examples:**

**FIXED type:**
```json
{}  // Không cần params
```

**TIERED type:**
```json
{
  "tiers": [
    {"max": 10000000, "fee": 0},
    {"max": 50000000, "fee": 20000},
    {"max": 100000000, "fee": 50000}
  ]
}
```

**PERCENTAGE type:**
```json
{
  "percentage": 0.001,
  "min_fee": 10000,
  "max_fee": 100000
}
```

---

### 4. CUSTOMER_FEE_JOB

**Purpose:** Jobs tính phí hàng tháng, được tạo tự động bởi scheduler

**Business Rules:**
- Tạo bởi `FeeJobPrepareScheduler` vào mùng 1 hàng tháng
- Idempotency key: `{customer_id}_{billing_month}` (unique constraint)
- Status: NEW → DONE hoặc FAILED
- Một customer chỉ có 1 job cho mỗi billing month
- Amount null khi status = NEW, có giá trị khi status = DONE

**Status Flow:**
```
NEW (initial, amount = null)
  ↓
DONE (success, amount filled) hoặc FAILED (error, amount may be null)
```

---

### 5. FEE_CHARGE_ATTEMPT

**Purpose:** Audit trail cho mỗi lần thử charge phí

**Business Rules:**
- Mỗi job có thể có nhiều attempts (nếu retry)
- Status: SUCCESS hoặc FAILED
- `error_message` và `error_code` chỉ có khi status = FAILED
- `attempt_no` tự động increment (1, 2, 3...)
- `external_txn_id` lưu transaction ID từ hệ thống bên ngoài
- Dùng cho reporting, monitoring, dispute resolution

---

## Data Dictionary

### Table: CUSTOMER

| Column Name    | Data Type       | Nullable | Default | Constraints        | Description                               |
|----------------|-----------------|----------|---------|--------------------|-------------------------------------------|
| id             | BIGINT          | NO       | AUTO    | PK, AUTO_INCREMENT | Unique identifier                         |
| full_name      | VARCHAR(255)    | NO       | -       | -                  | Họ tên đầy đủ khách hàng                  |
| email          | VARCHAR(255)    | NO       | -       | UNIQUE             | Email (unique, dùng để login)             |
| phone_number   | VARCHAR(20)     | YES      | NULL    | -                  | Số điện thoại                             |
| status         | VARCHAR(50)     | NO       | ACTIVE  | -                  | ACTIVE hoặc INACTIVE                      |
| created_at     | TIMESTAMP(6) TZ | NO       | NOW()   | -                  | Ngày tạo record                           |
| updated_at     | TIMESTAMP(6) TZ | YES      | NULL    | -                  | Ngày cập nhật gần nhất                    |
| created_by     | BIGINT          | YES      | NULL    | -                  | User ID tạo record                        |
| updated_by     | BIGINT          | YES      | NULL    | -                  | User ID cập nhật gần nhất                 |
| deleted_at     | TIMESTAMP(6) TZ | YES      | NULL    | -                  | Timestamp soft delete (NULL = chưa xóa)   |
| deleted_by     | BIGINT          | YES      | NULL    | -                  | User ID thực hiện soft delete             |

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE INDEX (email)
- INDEX (status, deleted_at) - for filtering active customers

**Sample Data:**
```sql
INSERT INTO customer (full_name, email, phone_number, status) VALUES
('Nguyen Van A', 'nguyenvana@example.com', '0901234567', 'ACTIVE'),
('Tran Thi B', 'tranthib@example.com', '0912345678', 'ACTIVE'),
('Le Van C', 'levanc@example.com', '0923456789', 'INACTIVE');
```

---

### Table: FEE_TYPE

| Column Name       | Data Type       | Nullable | Default | Constraints        | Description                               |
|-------------------|-----------------|----------|---------|--------------------|-------------------------------------------|
| id                | BIGINT          | NO       | AUTO    | PK, AUTO_INCREMENT | Unique identifier                         |
| code              | VARCHAR(50)     | NO       | -       | UNIQUE             | Code unique (FIXED_MONTHLY, TIERED_BALANCE, etc.) |
| name              | VARCHAR(255)    | NO       | -       | -                  | Tên hiển thị (Phí cố định hàng tháng)     |
| description       | TEXT            | YES      | NULL    | -                  | Mô tả chi tiết                            |
| calculation_type  | VARCHAR(50)     | NO       | -       | -                  | FIXED, TIERED, PERCENTAGE                 |
| is_active         | BOOLEAN         | NO       | true    | -                  | Active status                             |
| created_at        | TIMESTAMP(6) TZ | NO       | NOW()   | -                  | Ngày tạo                                  |
| updated_at        | TIMESTAMP(6) TZ | YES      | NULL    | -                  | Ngày cập nhật                             |

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE INDEX (code)
- INDEX (is_active) - for filtering active types

**Master Data (Seeded via Flyway):**
```sql
INSERT INTO fee_type (code, name, description, calculation_type, is_active) VALUES
('FIXED_MONTHLY', 'Phí cố định hàng tháng', 'Phí cố định không phụ thuộc vào số dư', 'FIXED', true),
('TIERED_BALANCE', 'Phí theo bậc số dư', 'Phí được tính dựa vào bậc số dư tài khoản', 'TIERED', true),
('PERCENTAGE_OF_BALANCE', 'Phí theo % số dư', 'Phí tính theo % số dư với min/max cap', 'PERCENTAGE', true);
```

---

### Table: CUSTOMER_FEE_CONFIG

| Column Name         | Data Type       | Nullable | Default | Constraints        | Description                                  |
|---------------------|-----------------|----------|---------|--------------------|----------------------------------------------|
| id                  | BIGINT          | NO       | AUTO    | PK, AUTO_INCREMENT | Unique identifier                            |
| customer_id         | BIGINT          | NO       | -       | FK → customer.id   | Reference đến customer                       |
| fee_type_id         | BIGINT          | NO       | -       | FK → fee_type.id   | Reference đến fee type                       |
| monthly_fee_amount  | DECIMAL(15,2)   | YES      | NULL    | -                  | Số tiền phí cơ bản (base amount)             |
| currency            | VARCHAR(3)      | NO       | VND     | -                  | Đơn vị tiền tệ (VND, USD, etc.)              |
| effective_from      | DATE            | NO       | -       | -                  | Ngày bắt đầu hiệu lực                        |
| effective_to        | DATE            | YES      | NULL    | -                  | Ngày kết thúc (NULL = vô thời hạn)           |
| calculation_params  | JSONB           | YES      | NULL    | -                  | Params cho calculation strategy (JSON)       |
| created_at          | TIMESTAMP(6) TZ | NO       | NOW()   | -                  | Ngày tạo config                              |
| updated_at          | TIMESTAMP(6) TZ | YES      | NULL    | -                  | Ngày cập nhật config                         |
| created_by          | BIGINT          | YES      | NULL    | -                  | User tạo config                              |
| updated_by          | BIGINT          | YES      | NULL    | -                  | User cập nhật config                         |
| deleted_at          | TIMESTAMP(6) TZ | YES      | NULL    | -                  | Soft delete timestamp                        |
| deleted_by          | BIGINT          | YES      | NULL    | -                  | User thực hiện soft delete                   |
| version             | BIGINT          | NO       | 0       | -                  | Optimistic locking version                   |

**Indexes:**
- PRIMARY KEY (id)
- INDEX (customer_id, deleted_at)
- INDEX (customer_id, effective_from, effective_to) - for finding active config
- FOREIGN KEY (customer_id) REFERENCES customer(id)
- FOREIGN KEY (fee_type_id) REFERENCES fee_type(id)

**Business Constraints (enforced in code):**
- No overlapping configs for same customer
- effective_from < effective_to (if effective_to is not null)

**Sample Data:**
```sql
-- Fixed monthly fee config
INSERT INTO customer_fee_config
(customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params)
VALUES
(1, 1, 50000.00, 'VND', '2025-01-01', NULL, '{}');

-- Tiered balance fee config
INSERT INTO customer_fee_config
(customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params)
VALUES
(2, 2, 50000.00, 'VND', '2025-01-01', '2025-12-31',
 '{"tiers": [{"max": 10000000, "fee": 0}, {"max": 50000000, "fee": 20000}, {"max": 100000000, "fee": 50000}]}');

-- Percentage balance fee config
INSERT INTO customer_fee_config
(customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params)
VALUES
(3, 3, 100000.00, 'VND', '2025-01-01', NULL,
 '{"percentage": 0.001, "min_fee": 10000, "max_fee": 100000}');
```

---

### Table: CUSTOMER_FEE_JOB

| Column Name      | Data Type       | Nullable | Default | Constraints        | Description                                  |
|------------------|-----------------|----------|---------|--------------------|----------------------------------------------|
| id               | BIGINT          | NO       | AUTO    | PK, AUTO_INCREMENT | Unique identifier                            |
| customer_id      | BIGINT          | NO       | -       | FK → customer.id   | Reference đến customer                       |
| billing_month    | VARCHAR(7)      | NO       | -       | -                  | Tháng tính phí (format: YYYY-MM)             |
| amount           | DECIMAL(15,2)   | YES      | NULL    | -                  | Số tiền phí (null khi NEW, có giá trị khi DONE) |
| status           | VARCHAR(20)     | NO       | NEW     | -                  | NEW, DONE, FAILED                            |
| idempotency_key  | VARCHAR(255)    | NO       | -       | UNIQUE             | {customer_id}_{billing_month}                |
| created_at       | TIMESTAMP(6) TZ | NO       | NOW()   | -                  | Ngày tạo                                     |
| updated_at       | TIMESTAMP(6) TZ | NO       | NOW()   | -                  | Ngày cập nhật                                |
| deleted_at       | TIMESTAMP(6) TZ | YES      | NULL    | -                  | Soft delete timestamp                        |
| created_by       | BIGINT          | YES      | NULL    | -                  | User tạo job                                 |
| updated_by       | BIGINT          | YES      | NULL    | -                  | User cập nhật job                            |
| deleted_by       | BIGINT          | YES      | NULL    | -                  | User thực hiện soft delete                   |
| version          | BIGINT          | NO       | 0       | -                  | Optimistic locking version                   |

**Indexes:**
- PRIMARY KEY (id)
- UNIQUE INDEX (idempotency_key) - prevent duplicate jobs
- INDEX (customer_id)
- INDEX (billing_month)
- INDEX (status)
- INDEX (deleted_at) WHERE deleted_at IS NULL
- FOREIGN KEY (customer_id) REFERENCES customer(id) ON DELETE RESTRICT

**Sample Data:**
```sql
INSERT INTO customer_fee_job (customer_id, billing_month, amount, status, idempotency_key)
VALUES
(1, '2025-01', NULL, 'NEW', '1_2025-01'),
(2, '2025-01', 50000.00, 'DONE', '2_2025-01'),
(3, '2025-01', NULL, 'FAILED', '3_2025-01');
```

---

### Table: FEE_CHARGE_ATTEMPT

| Column Name      | Data Type       | Nullable | Default | Constraints        | Description                                  |
|------------------|-----------------|----------|---------|--------------------|----------------------------------------------|
| id               | BIGINT          | NO       | AUTO    | PK, AUTO_INCREMENT | Unique identifier                            |
| job_id           | BIGINT          | NO       | -       | FK → customer_fee_job.id | Reference đến fee job              |
| customer_id      | BIGINT          | NO       | -       | -                  | Customer ID (denormalized)                   |
| billing_month    | VARCHAR(7)      | NO       | -       | -                  | Billing month (denormalized)                 |
| amount           | DECIMAL(15,2)   | NO       | -       | -                  | Số tiền charge                               |
| attempt_no       | INTEGER         | NO       | 1       | -                  | Số lần thử (1, 2, 3...)                      |
| status           | VARCHAR(20)     | NO       | -       | -                  | SUCCESS hoặc FAILED                          |
| error_code       | VARCHAR(50)     | YES      | NULL    | -                  | Error code nếu failed                        |
| error_message    | TEXT            | YES      | NULL    | -                  | Error message nếu failed                     |
| external_txn_id  | VARCHAR(255)    | YES      | NULL    | -                  | External transaction ID                      |
| created_at       | TIMESTAMP(6) TZ | NO       | NOW()   | -                  | Ngày tạo                                     |
| created_by       | BIGINT          | YES      | NULL    | -                  | User tạo attempt                             |

**Indexes:**
- PRIMARY KEY (id)
- INDEX (job_id)
- INDEX (status)
- FOREIGN KEY (job_id) REFERENCES customer_fee_job(id) ON DELETE RESTRICT

**Sample Data:**
```sql
INSERT INTO fee_charge_attempt (job_id, customer_id, billing_month, amount, attempt_no, status)
VALUES
(1, 1, '2025-01', 50000.00, 1, 'SUCCESS'),
(2, 2, '2025-01', 20000.00, 1, 'SUCCESS'),
(3, 3, '2025-01', 100000.00, 1, 'FAILED');

-- Failed attempt with error message
INSERT INTO fee_charge_attempt (job_id, customer_id, billing_month, amount, attempt_no, status, error_code, error_message)
VALUES
(3, 3, '2025-01', 100000.00, 2, 'FAILED', 'TIMEOUT', 'External payment gateway timeout');
```

---

## Relationships

### 1. Customer → Customer Fee Config (1:N)
```
One customer can have multiple fee configs (for different time periods)
But only ONE active config at any given time

Example:
Customer A:
  - Config 1: 2024-01-01 to 2024-12-31 (FIXED: 50k)
  - Config 2: 2025-01-01 to NULL (TIERED: varies)

At 2025-02-01: Only Config 2 is active
```

### 2. Fee Type → Customer Fee Config (1:N)
```
One fee type can be used by multiple customer configs

Example:
FIXED_MONTHLY fee type is used by:
  - Customer A's config
  - Customer B's config
  - Customer C's config
```

### 3. Customer → Customer Fee Job (1:N)
```
One customer has multiple jobs (one per billing month)

Example:
Customer A:
  - Job 1: 2024-12 (DONE)
  - Job 2: 2025-01 (DONE)
  - Job 3: 2025-02 (NEW)
```

### 4. Customer Fee Job → Fee Charge Attempt (1:N)
```
One job can have multiple attempts (if retry)

Example:
Job 123 (Customer A, 2025-01):
  - Attempt 1 (attempt_no=1): SUCCESS (50,000 VND)

Job 456 (Customer B, 2025-01):
  - Attempt 1 (attempt_no=1): FAILED (error_code: TIMEOUT)
  - Attempt 2 (attempt_no=2): FAILED (error_code: SERVICE_UNAVAILABLE)
  - Attempt 3 (attempt_no=3): SUCCESS (20,000 VND)
```

---

## Common Queries

### 1. Get active fee config for a customer
```sql
SELECT * FROM customer_fee_config
WHERE customer_id = 1
  AND deleted_at IS NULL
  AND effective_from <= CURRENT_DATE
  AND (effective_to IS NULL OR effective_to >= CURRENT_DATE);
```

### 2. Get pending jobs (for scheduler)
```sql
SELECT * FROM customer_fee_job
WHERE status = 'NEW'
  AND deleted_at IS NULL
ORDER BY created_at ASC
LIMIT 100;
```

### 3. Get charge history for a customer
```sql
SELECT a.*, j.billing_month, j.amount as job_amount
FROM fee_charge_attempt a
JOIN customer_fee_job j ON a.job_id = j.id
WHERE a.customer_id = 1
ORDER BY a.created_at DESC;
```

### 4. Monthly statistics
```sql
SELECT
    j.billing_month,
    COUNT(DISTINCT j.id) as total_jobs,
    COUNT(DISTINCT CASE WHEN j.status = 'DONE' THEN j.id END) as done_count,
    COUNT(DISTINCT CASE WHEN j.status = 'FAILED' THEN j.id END) as failed_count,
    COUNT(DISTINCT CASE WHEN a.status = 'SUCCESS' THEN a.id END) as success_attempts,
    COUNT(DISTINCT CASE WHEN a.status = 'FAILED' THEN a.id END) as failed_attempts,
    SUM(CASE WHEN a.status = 'SUCCESS' THEN a.amount ELSE 0 END) as total_amount
FROM customer_fee_job j
LEFT JOIN fee_charge_attempt a ON j.id = a.job_id
WHERE j.billing_month = '2025-01'
GROUP BY j.billing_month;
```

### 5. Check overlapping configs
```sql
-- Check if new config (2025-03-01 to 2025-06-30) overlaps with existing
SELECT * FROM customer_fee_config
WHERE customer_id = 1
  AND deleted_at IS NULL
  AND effective_from <= '2025-06-30'  -- new config's end
  AND (effective_to IS NULL OR effective_to >= '2025-03-01');  -- new config's start
```

---

## Data Volume Estimates

**Assumptions:**
- 100,000 active customers
- Average 2 configs per customer (current + 1 historical)
- 12 months of job history
- Average 1.2 attempts per job (some retries)

| Table                  | Records (Approx) | Size (Approx) |
|------------------------|------------------|---------------|
| customer               | 100,000          | 50 MB         |
| fee_type               | 10               | < 1 MB        |
| customer_fee_config    | 200,000          | 100 MB        |
| customer_fee_job       | 1,200,000        | 300 MB        |
| fee_charged_attempt    | 1,440,000        | 400 MB        |
| **TOTAL**              | **2,940,010**    | **~850 MB**   |

**Growth per month:**
- Jobs: +100,000 (1 per active customer)
- Attempts: +120,000 (1.2 per job)
- Data: +70 MB/month

**Retention Policy (recommended):**
- Keep jobs + attempts for 24 months
- Archive older data to cold storage
- Estimated max size: 850 MB + (70 MB × 24) = ~2.5 GB

---

## Data Security

### Sensitive Data
- Customer email (PII)
- Customer phone number (PII)
- Customer full name (PII)

### Encryption
- At rest: PostgreSQL transparent data encryption (TDE)
- In transit: SSL/TLS connections
- Application level: Encrypt sensitive fields if needed

### Access Control
- Database users: read-only vs read-write
- Application uses connection pooling with limited privileges
- No direct database access for end users

### Audit Trail
- All tables have created_at, updated_at
- Soft delete preserves data for audit
- Fee charged attempts = complete audit log

---

## Data Lifecycle

### Create
```
Customer signs up
  ↓
Admin creates fee config
  ↓
Scheduler creates job (monthly)
  ↓
Job executes → creates attempt
```

### Update
```
Business rule changes
  ↓
Admin creates new fee config with new effective_from
  ↓
Old config expires (effective_to set)
  ↓
Future jobs use new config
```

### Archive/Delete
```
Customer closes account
  ↓
Soft delete customer (set deleted_at)
  ↓
Stop creating new jobs
  ↓
Keep historical data for 24 months
  ↓
Archive to cold storage
```

---

## Migration Scripts

**Location:** `src/main/resources/db/migration/`

**Flyway naming:** `V{version}__{description}.sql`

**Example:**
```
V1__create_base_tables.sql
V2__create_fee_type_table.sql
V3__create_customer_fee_config_table.sql
V4__create_job_tables.sql
V5__add_indexes.sql
V6__seed_fee_types.sql
V7__add_version_column.sql
V8__seed_test_data.sql
```

**Best Practices:**
- Never modify executed migrations
- Always test migrations on copy of production data
- Use transactions (BEGIN; ... COMMIT;)
- Add rollback scripts for each migration

---

**Version:** 1.0
**Last Updated:** 2025-01-07
**Author:** HDBank Development Team
