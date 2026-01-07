# Customer Fee Service - Test Cases & Test Data

## Tổng quan

Document này mô tả chi tiết các test cases với dữ liệu mẫu để test toàn bộ chức năng tính phí. Tất cả test data đã được seed sẵn trong database qua Flyway migration `V8__seed_test_data.sql`.

---

## Test Scenarios

### Scenario 1: Fixed Monthly Fee (Phí cố định hàng tháng)

**Business Case:** Customer VIP trả phí cố định 50,000 VND/tháng

**Test Data:**
```sql
-- Customer
ID: 1
Name: "Nguyễn Văn An"
Email: "nguyen.van.an@gmail.com"
Status: ACTIVE

-- Fee Config
Fee Type: FIXED_MONTHLY (ID: 1)
Monthly Amount: 50,000 VND
Effective From: 2025-01-01
Effective To: NULL (vô thời hạn)
Calculation Params: {}
```

**Expected Result:**
- Phí tính được: **50,000 VND** (cố định, không phụ thuộc params)

**API Test:**
```bash
# 1. Preview phí
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "calculationParams": {}
  }'

# Expected Response:
{
  "response_code": "00",
  "data": {
    "customer_id": 1,
    "fee_type_code": "FIXED_MONTHLY",
    "fee_type_name": "Phí cố định hàng tháng",
    "calculation_type": "FIXED",
    "monthly_fee_amount": 50000,
    "calculated_fee": 50000,  # ← Kết quả
    "currency": "VND"
  }
}
```

**Git Link:** Test case này verify logic trong `FixedMonthlyFeeStrategy.java:20`

---

### Scenario 2: Tiered Balance Fee - Tier 1 (Số dư thấp)

**Business Case:** Customer có số dư 0-50M VND → Phí 10,000 VND

**Test Data:**
```sql
-- Customer
ID: 2
Name: "Trần Thị Bình"
Email: "tran.thi.binh@gmail.com"
Status: ACTIVE

-- Fee Config
Fee Type: TIERED_BALANCE (ID: 2)
Monthly Amount: NULL
Effective From: 2025-01-01
Effective To: NULL
Calculation Params:
{
  "balance": 100000000,
  "tiers": [
    {"from": 0, "to": 50000000, "fee": 10000},
    {"from": 50000001, "to": 200000000, "fee": 20000},
    {"from": 200000001, "to": null, "fee": 50000}
  ]
}

-- Test with balance
Balance: 30,000,000 VND
```

**Expected Result:**
- Balance 30M trong range [0 - 50M] → Match tier 1
- Phí tính được: **10,000 VND**

**API Test:**
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 2,
    "calculationParams": {
      "balance": 30000000
    }
  }'

# Expected Response:
{
  "response_code": "00",
  "data": {
    "customer_id": 2,
    "fee_type_code": "TIERED_BALANCE",
    "calculated_fee": 10000,  # ← Tier 1 fee
    "currency": "VND"
  }
}
```

**Git Link:** Test case này verify logic trong `TieredBalanceFeeStrategy.java:40`

---

### Scenario 3: Tiered Balance Fee - Tier 2 (Số dư trung bình)

**Business Case:** Customer có số dư từ 50M-200M VND → Phí 20,000 VND

**Test Data:**
```sql
-- Same config as Scenario 2
Customer ID: 2

-- Test with different balance
Balance: 100,000,000 VND
```

**Expected Result:**
- Balance 100M trong range [50M-200M] → Match tier 2
- Phí tính được: **20,000 VND**

**API Test:**
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 2,
    "calculationParams": {
      "balance": 100000000
    }
  }'

# Expected Response:
{
  "calculated_fee": 20000  # ← Tier 2 fee
}
```

---

### Scenario 4: Tiered Balance Fee - Tier 3 (Số dư rất cao)

**Business Case:** Customer có số dư > 200M VND → Phí 50,000 VND

**Test Data:**
```sql
-- Same config as Scenario 2
Customer ID: 2

-- Test with very high balance
Balance: 250,000,000 VND
```

**Expected Result:**
- Balance 250M trong range [>200M] → Match tier 3
- Phí tính được: **50,000 VND**

**API Test:**
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 2,
    "calculationParams": {
      "balance": 250000000
    }
  }'

# Expected Response:
{
  "calculated_fee": 50000  # ← Tier 3 fee
}
```

---

### Scenario 5: Tiered Balance Fee - No Fallback Needed

**NOTE:** Scenario này không còn apply với V8 seed data vì Customer 2 có monthly_fee_amount = NULL. Tất cả balance ranges đều có tier tương ứng.

---

### Scenario 6: Percentage Balance Fee - Normal Case

**Business Case:** Customer trả 0.1% số dư, tối thiểu 5k, tối đa 100k

**Test Data:**
```sql
-- Customer
ID: 3
Name: "Lê Văn Cường"
Email: "le.van.cuong@gmail.com"
Status: ACTIVE

-- Fee Config
Fee Type: PERCENTAGE_OF_BALANCE (ID: 3)
Monthly Amount: NULL
Effective From: 2025-01-01
Effective To: NULL
Calculation Params:
{
  "balance": 50000000,
  "percentage": 0.001,    # 0.1%
  "min_fee": 5000,
  "max_fee": 100000
}

-- Test with balance
Balance: 50,000,000 VND
```

**Calculation:**
```
50,000,000 × 0.001 = 50,000 VND
50,000 >= min_fee (5,000) ✓
50,000 <= max_fee (100,000) ✓
→ Result: 50,000 VND
```

**Expected Result:** **50,000 VND**

**API Test:**
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 3,
    "calculationParams": {
      "balance": 50000000
    }
  }'

# Expected Response:
{
  "calculated_fee": 50000  # ← 0.1% của 50M
}
```

**Git Link:** Test case này verify logic trong `PercentageBalanceFeeStrategy.java:30`

---

### Scenario 7: Percentage Balance Fee - Minimum Cap

**Business Case:** Số dư thấp → Phí tính được < min_fee → Apply min_fee

**Test Data:**
```sql
-- Same config as Scenario 6
Customer ID: 3

-- Test with low balance
Balance: 3,000,000 VND
```

**Calculation:**
```
3,000,000 × 0.001 = 3,000 VND
3,000 < min_fee (5,000) → Apply min_fee
→ Result: 5,000 VND
```

**Expected Result:** **5,000 VND** (minimum cap applied)

**API Test:**
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 3,
    "calculationParams": {
      "balance": 3000000
    }
  }'

# Expected Response:
{
  "calculated_fee": 5000  # ← Min cap applied
}
```

---

### Scenario 8: Percentage Balance Fee - Maximum Cap

**Business Case:** Số dư cao → Phí tính được > max_fee → Apply max_fee

**Test Data:**
```sql
-- Same config as Scenario 6
Customer ID: 3

-- Test with high balance
Balance: 200,000,000 VND
```

**Calculation:**
```
200,000,000 × 0.001 = 200,000 VND
200,000 > max_fee (100,000) → Apply max_fee
→ Result: 100,000 VND
```

**Expected Result:** **100,000 VND** (maximum cap applied)

**API Test:**
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 3,
    "calculationParams": {
      "balance": 200000000
    }
  }'

# Expected Response:
{
  "calculated_fee": 100000  # ← Max cap applied
}
```

---

### Scenario 9: End-to-End Fee Charging Flow

**Business Case:** Scheduler tự động tạo job và execute để charge phí

**Test Data:**
```sql
-- Customer
ID: 1 (Fixed fee: 50,000 VND)

-- Billing Month
2025-01
```

**Flow:**
```
1. FeeJobPrepareScheduler runs (mùng 1 hàng tháng)
   ↓
2. Create CustomerFeeJob
   - customer_id: 1
   - billing_month: "2025-01"
   - status: NEW
   - amount: NULL
   - idempotency_key: "1_2025-01"
   ↓
3. FeeJobExecuteScheduler runs (mỗi 5 phút)
   ↓
4. FeeChargeService.chargeFee(jobId)
   ↓
5. Calculate fee: 50,000 VND
   ↓
6. Create FeeChargeAttempt
   - job_id: {job_id}
   - customer_id: 1
   - billing_month: "2025-01"
   - amount: 50,000
   - attempt_no: 1
   - status: SUCCESS
   ↓
7. Update job
   - status: DONE
   - amount: 50,000
   ↓
8. Publish Kafka event
```

**Verification Queries:**
```sql
-- Check job created
SELECT * FROM customer_fee_job
WHERE customer_id = 1 AND billing_month = '2025-01';

-- Check attempt created
SELECT a.* FROM fee_charge_attempt a
WHERE a.customer_id = 1 AND a.billing_month = '2025-01';
```

**Expected Results:**
- Job status: DONE
- Job amount: 50,000 VND
- Attempt status: SUCCESS
- Attempt amount: 50,000 VND
- Attempt no: 1

---

### Scenario 10: Overlapping Config Validation

**Business Case:** Không cho phép tạo config overlap với config existing

**Test Data:**
```sql
-- Existing Config (from V8 seed data)
Customer ID: 1
Effective From: 2025-01-01
Effective To: NULL (vô thời hạn)

-- Try to create overlapping config
Customer ID: 1
Effective From: 2025-06-01  # ← Overlap! Existing config có effective_to = NULL
Effective To: 2025-12-31
```

**Expected Result:** **400 Bad Request** - Validation error

**API Test:**
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "feeTypeId": 1,
    "monthlyFeeAmount": 60000,
    "currency": "VND",
    "effectiveFrom": "2025-06-01",
    "effectiveTo": "2025-12-31",
    "calculationParams": {}
  }'

# Expected Response:
{
  "response_code": "01",
  "response_message": "Fee config overlaps with existing config"
}
```

**Git Link:** Test case này verify logic trong `FeeConfigService.java:210` (checkOverlappingConfigs method)

---

## Complete Test Data Matrix

| Test # | Customer | Fee Type       | Balance      | Expected Fee | Test Point              |
|--------|----------|----------------|--------------|--------------|-------------------------|
| 1      | 1        | FIXED          | N/A          | 50,000       | Fixed amount            |
| 2      | 2        | TIERED         | 30,000,000   | 10,000       | Tier 1 (0-50M)          |
| 3      | 2        | TIERED         | 100,000,000  | 20,000       | Tier 2 (50M-200M)       |
| 4      | 2        | TIERED         | 250,000,000  | 50,000       | Tier 3 (>200M)          |
| 5      | -        | -              | -            | -            | (Not applicable)        |
| 6      | 3        | PERCENTAGE     | 50,000,000   | 50,000       | Normal calculation      |
| 7      | 3        | PERCENTAGE     | 3,000,000    | 5,000        | Min cap applied         |
| 8      | 3        | PERCENTAGE     | 200,000,000  | 100,000      | Max cap applied         |

---

## Running Tests

### 1. Automated Unit Tests

```bash
# Run all tests
./gradlew test

# Run strategy tests only
./gradlew test --tests '*Strategy*'

# Run specific test
./gradlew test --tests PercentageBalanceFeeStrategyTest.shouldApplyMinimumCap
```

**Test Files Location:**
- `src/test/java/com/hdbank/customer_fee_service/service/strategy/`
  - `FixedMonthlyFeeStrategyTest.java`
  - `TieredBalanceFeeStrategyTest.java`
  - `PercentageBalanceFeeStrategyTest.java`

---

### 2. Integration Tests

```bash
# Run integration tests
./gradlew test --tests '*IntegrationTest'

# Specific integration test
./gradlew test --tests CustomerControllerIntegrationTest
./gradlew test --tests FeeConfigControllerIntegrationTest
```

**Test Files Location:**
- `src/test/java/com/hdbank/customer_fee_service/controller/`
  - `CustomerControllerIntegrationTest.java`
  - `FeeConfigControllerIntegrationTest.java`

---

### 3. Manual API Testing with Seeded Data

**Prerequisites:**
```bash
# Start application
./gradlew bootRun

# Or with Docker
docker-compose up
```

**Test Scenarios:**

#### Test 1: Fixed Fee Preview
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1
  }'
```

#### Test 2: Tiered Fee Preview (Low Balance)
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 2,
    "calculationParams": {"balance": 30000000}
  }'
```

#### Test 3: Percentage Fee Preview (High Balance - Max Cap)
```bash
curl -X POST http://localhost:8080/api/v1/fee-configs/preview \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 3,
    "calculationParams": {"balance": 200000000}
  }'
```

#### Test 4: Get Customer Info
```bash
curl http://localhost:8080/api/v1/customers/1
```

#### Test 5: Get Active Fee Config
```bash
curl http://localhost:8080/api/v1/fee-configs/customer/1/active
```

---

## Seed Data SQL

**File:** `src/main/resources/db/migration/V8__seed_test_data.sql`

**NOTE:** V8 migration seeds 9 customers (IDs 1-9) với nhiều fee configs. Xem file để biết chi tiết đầy đủ.

**Key Test Data:**

```sql
-- Customer 1: FIXED fee 50,000 VND
INSERT INTO customer (full_name, email, phone_number, status, created_by, updated_by) VALUES
    ('Nguyễn Văn An', 'nguyen.van.an@gmail.com', '0901234567', 'ACTIVE', 0, 0);

INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, created_by, updated_by)
VALUES
    (1, 1, 50000.00, 'VND', '2025-01-01', NULL, 0, 0);

-- Customer 2: TIERED fee
INSERT INTO customer (full_name, email, phone_number, status, created_by, updated_by) VALUES
    ('Trần Thị Bình', 'tran.thi.binh@gmail.com', '0902345678', 'ACTIVE', 0, 0);

INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params, created_by, updated_by)
VALUES
    (2, 2, NULL, 'VND', '2025-01-01', NULL,
     '{"balance": 100000000, "tiers": [
         {"from": 0, "to": 50000000, "fee": 10000},
         {"from": 50000001, "to": 200000000, "fee": 20000},
         {"from": 200000001, "to": null, "fee": 50000}
     ]}'::jsonb,
     0, 0);

-- Customer 3: PERCENTAGE fee
INSERT INTO customer (full_name, email, phone_number, status, created_by, updated_by) VALUES
    ('Lê Văn Cường', 'le.van.cuong@gmail.com', '0903456789', 'ACTIVE', 0, 0);

INSERT INTO customer_fee_config
    (customer_id, fee_type_id, monthly_fee_amount, currency, effective_from, effective_to, calculation_params, created_by, updated_by)
VALUES
    (3, 3, NULL, 'VND', '2025-01-01', NULL,
     '{"balance": 50000000, "percentage": 0.001, "min_fee": 5000, "max_fee": 100000}'::jsonb,
     0, 0);
```

---

## Test Execution Steps

### Step 1: Setup Database
```bash
# Start PostgreSQL
docker-compose up -d postgres

# Flyway will auto-run migrations on app start
# Including V8__seed_test_data.sql
```

### Step 2: Start Application
```bash
./gradlew bootRun

# Check logs for:
# - Flyway migration success
# - Test data seeded
# - Application ready
```

### Step 3: Run Unit Tests
```bash
./gradlew test

# Expected: 28 tests pass
# - 18 unit tests
# - 9 integration tests
# - 1 context load test
```

### Step 4: Manual API Testing
```bash
# Test all 8 scenarios above using curl commands
# Or use Postman collection
# Or use Swagger UI: http://localhost:8080/swagger-ui.html
```

### Step 5: Verify Scheduler
```bash
# Wait for scheduler to run
# Check logs for job preparation and execution
# Query database to verify jobs created and processed

SELECT * FROM customer_fee_job WHERE customer_id >= 100;
SELECT * FROM fee_charged_attempt;
```

---

## Expected Test Results Summary

| Test Category           | Total | Pass | Fail | Notes                        |
|------------------------|-------|------|------|------------------------------|
| Unit Tests             | 18    | 18   | 0    | All edge cases covered       |
| Integration Tests      | 9     | 9    | 0    | E2E flows working            |
| Manual API Tests       | 10    | 10   | 0    | All scenarios verified       |
| **TOTAL**              | **37**| **37**| **0**| **100% pass rate**          |

---

## Git Repository Structure

```
customer-fee-service/
├── docs/
│   ├── FEATURES.md              # ← This file references
│   ├── FUNCTION_FLOWS.md        # ← All function flows
│   ├── DATA_DICTIONARY.md       # ← Database schema
│   └── TEST_CASES.md            # ← Current file
├── src/
│   ├── main/
│   │   ├── java/.../
│   │   │   ├── controller/      # ← API endpoints
│   │   │   ├── service/
│   │   │   │   └── strategy/    # ← Fee calculation strategies
│   │   │   └── ...
│   │   └── resources/
│   │       └── db/migration/
│   │           └── V8__seed_test_data.sql  # ← Seed data file
│   └── test/
│       └── java/.../
│           ├── service/
│           │   └── strategy/    # ← Strategy tests
│           └── controller/      # ← Integration tests
└── ...
```

---

## Notes

1. **Test Data IDs Start at 1**
   - V8 migration seeds customers with IDs 1-9
   - Easy to use for manual testing
   - created_by/updated_by = 0 indicates system seed data

2. **Billing Month Dynamic**
   - Uses `TO_CHAR(CURRENT_DATE, 'YYYY-MM')`
   - Always creates jobs for current month
   - No need to update seed data monthly

3. **Calculation Params are JSONB**
   - Flexible structure
   - Type-safe with validation in code
   - Easy to extend with new params

4. **Test Coverage**
   - All 3 fee types covered
   - All edge cases tested
   - Min/max caps validated
   - Overlap validation included

5. **Idempotency**
   - Unique constraint on idempotency_key
   - Format: {customer_id}_{billing_month}
   - Prevents duplicate jobs
   - Safe to re-run scheduler

---
