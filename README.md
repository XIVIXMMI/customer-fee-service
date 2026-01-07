# Customer Fee Service Documentation

## Danh mục Documents

Project này có 4 documents chính, mỗi document phục vụ 1 mục đích cụ thể:

### 1. [FEATURES.md](docs/FEATURES.md) - Mô tả Chức năng
**Nội dung:**
- Tổng quan hệ thống
- Mô tả chi tiết 19 API endpoints
- Business rules cho từng chức năng
- Use case scenarios thực tế
- Future enhancements

**Dùng cho:**
- Product Owners
- Business Analysts
- QA Team
- New developers onboarding

---

### 2. [FUNCTION_FLOWS.md](docs/FUNCTION_FLOWS.md) - Function Flows
**Nội dung:**
- Detailed flow từng function gọi nhau
- Flow diagrams với line numbers
- Error handling flows
- Scheduler flows
- Kafka event flows
- Strategy pattern implementation

**Dùng cho:**
- Developers
- Code reviewers
- Debugging
- Performance optimization

---

### 3. [DATA_DICTIONARY.md](docs/DATA_DICTIONARY.md) - Data Dictionary
**Nội dung:**
- Entity Relationship Diagram (ERD)
- Data dictionary cho 5 tables chính
- Column descriptions với constraints
- Indexes và foreign keys
- Sample data và common queries
- Data volume estimates
- Migration scripts

**Dùng cho:**
- Database Administrators
- Backend Developers
- Data Analysts
- Schema migrations

---

### 4. [TEST_CASES.md](docs/TEST_CASES.md) - Test Cases & Test Data
**Nội dung:**
- 10 test scenarios chi tiết
- Test data matrix
- API test examples (curl commands)
- Expected results
- Seed data SQL scripts
- Test execution steps

**Dùng cho:**
- QA Team
- Developers (TDD)
- Manual testing
- Integration testing

---

## Quick Start

### Đọc để hiểu hệ thống (Top-down approach)
```
1. FEATURES.md        → Hiểu WHAT (chức năng là gì)
2. FUNCTION_FLOWS.md  → Hiểu HOW (làm thế nào)
3. DATA_DICTIONARY.md → Hiểu WHERE (data lưu ở đâu)
4. TEST_CASES.md      → Verify (test như thế nào)
```

### Đọc để implement feature mới (Bottom-up approach)
```
1. DATA_DICTIONARY.md → Design schema
2. FUNCTION_FLOWS.md  → Design flows
3. FEATURES.md        → Write specs
4. TEST_CASES.md      → Write tests
```

### Đọc để debug issue
```
1. FEATURES.md        → Understand expected behavior
2. TEST_CASES.md      → Reproduce with test data
3. FUNCTION_FLOWS.md  → Trace execution flow
4. DATA_DICTIONARY.md → Check database state
```

---

## Document Relationships

```
┌──────────────────────────────────────────────────────────┐
│                    FEATURES.md                           │
│  - 19 API endpoints                                      │
│  - Business rules                                        │
│  - Use cases                                             │
└────────────────┬─────────────────────────────────────────┘
                 │
                 │ implements
                 ▼
┌──────────────────────────────────────────────────────────┐
│              FUNCTION_FLOWS.md                           │
│  - Controller → Service → Repository → DB                │
│  - Strategy pattern                                      │
│  - Error handling                                        │
└────────────────┬─────────────────────────────────────────┘
                 │
                 │ uses
                 ▼
┌──────────────────────────────────────────────────────────┐
│            DATA_DICTIONARY.md                            │
│  - 5 tables (customer, fee_type, config, job, attempt)   │
│  - Relationships                                         │
│  - Constraints                                           │
└────────────────┬─────────────────────────────────────────┘
                 │
                 │ verified by
                 ▼
┌──────────────────────────────────────────────────────────┐
│              TEST_CASES.md                               │
│  - 10 scenarios                                          │
│  - Test data (IDs 100+)                                  │
│  - Expected results                                      │
└──────────────────────────────────────────────────────────┘
```

---

## 🔍 Cross-References

### Example: Fixed Fee Flow

**FEATURES.md:**
- Section 6.1: "Tính phí tự động"
- Use Case Scenario 1: "Khách hàng mới mở tài khoản VIP"

**FUNCTION_FLOWS.md:**
- Section 4: "Charge Fee Flow"
- Section 3: "Fee Preview Flow"
- Strategy: `FixedMonthlyFeeStrategy.java:20`

**DATA_DICTIONARY.md:**
- Table: `customer_fee_config`
- Field: `calculation_params` = `{}`
- Relationship: `customer_fee_config` → `fee_type`

**TEST_CASES.md:**
- Scenario 1: "Fixed Monthly Fee"
- Test Data: Customer 100
- Expected: 100,000 VND

---

## Document Statistics

| Document             | Size    | Sections | Code Examples | SQL Queries |
|---------------------|---------|----------|---------------|-------------|
| FEATURES.md         | ~25 KB  | 9        | 15            | 0           |
| FUNCTION_FLOWS.md   | ~28 KB  | 6        | 20            | 0           |
| DATA_DICTIONARY.md  | ~22 KB  | 8        | 5             | 10          |
| TEST_CASES.md       | ~18 KB  | 10       | 25            | 5           |
| **TOTAL**           | **~93 KB** | **33** | **65**     | **15**      |

---
## Related Files

### Source Code References
```
src/main/java/.../
├── controller/         → FEATURES.md API descriptions
├── service/
│   └── strategy/       → FUNCTION_FLOWS.md Strategy patterns
├── repository/         → DATA_DICTIONARY.md Queries
└── entity/             → DATA_DICTIONARY.md Table structures

src/test/java/.../
├── service/strategy/   → TEST_CASES.md Unit tests
└── controller/         → TEST_CASES.md Integration tests

src/main/resources/db/migration/
└── V9__seed_test_data_standardized.sql  → TEST_CASES.md Seed data
```

---