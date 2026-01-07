# Customer Fee Service - Mô tả Chức năng

## Tổng quan

Customer Fee Service là hệ thống backend quản lý và tính phí tự động cho khách hàng của HDBank. Hệ thống hỗ trợ nhiều loại phí khác nhau và tự động tính toán, thu phí hàng tháng.

---

## Các Chức năng Chính

### 1. Quản lý Khách hàng (Customer Management)

#### 1.1 Tạo khách hàng mới
**Mục đích:** Đăng ký khách hàng mới vào hệ thống

**Input:**
- Họ tên đầy đủ
- Email (unique)
- Số điện thoại
- Trạng thái (ACTIVE/INACTIVE)

**Output:**
- Thông tin khách hàng đã tạo
- Customer ID

**Business Rules:**
- Email phải unique
- Email phải đúng format
- Số điện thoại phải đúng format

**API:** `POST /api/v1/customers`

---

#### 1.2 Xem thông tin khách hàng
**Mục đích:** Tra cứu thông tin chi tiết của khách hàng

**Input:**
- Customer ID

**Output:**
- Thông tin đầy đủ khách hàng
- Ngày tạo, ngày cập nhật

**Business Rules:**
- Chỉ hiển thị khách hàng chưa bị xóa

**API:** `GET /api/v1/customers/{id}`

---

#### 1.3 Danh sách khách hàng
**Mục đích:** Xem danh sách tất cả khách hàng, hỗ trợ phân trang

**Input:**
- Page number (default: 0)
- Page size (default: 10)

**Output:**
- Danh sách khách hàng
- Tổng số trang
- Tổng số bản ghi

**Business Rules:**
- Chỉ hiển thị khách hàng chưa bị xóa
- Sắp xếp theo ngày tạo giảm dần (mới nhất trước)

**API:** `GET /api/v1/customers?page=0&size=10`

---

#### 1.4 Cập nhật thông tin khách hàng
**Mục đích:** Thay đổi thông tin khách hàng

**Input:**
- Customer ID
- Thông tin cần cập nhật (fullName, email, phoneNumber, status)

**Output:**
- Thông tin khách hàng sau khi cập nhật

**Business Rules:**
- Email mới phải unique
- Không thể cập nhật khách hàng đã xóa

**API:** `PUT /api/v1/customers/{id}`

---

#### 1.5 Xóa khách hàng
**Mục đích:** Xóa khách hàng khỏi hệ thống (soft delete)

**Input:**
- Customer ID

**Output:**
- Thông báo thành công

**Business Rules:**
- Soft delete: không xóa vật lý, chỉ đánh dấu deletedAt
- Không thể xem hoặc sử dụng khách hàng đã xóa

**API:** `DELETE /api/v1/customers/{id}`

---

### 2. Quản lý Loại phí (Fee Type Management)

#### 2.1 Xem danh sách loại phí
**Mục đích:** Xem tất cả các loại phí có sẵn trong hệ thống

**Input:** Không

**Output:**
- Danh sách tất cả fee types
- Code, tên, mô tả, loại tính toán

**Fee Types:**
1. **FIXED_MONTHLY** - Phí cố định hàng tháng
   - Tính toán: Trả số tiền cố định mỗi tháng
   - Ví dụ: Phí duy trì tài khoản 50,000 VND/tháng

2. **TIERED_BALANCE** - Phí theo bậc số dư
   - Tính toán: Dựa vào số dư tài khoản, áp dụng mức phí tương ứng
   - Ví dụ:
     - Dưới 10M: Miễn phí
     - 10M-50M: 20,000 VND
     - Trên 50M: 50,000 VND

3. **PERCENTAGE_OF_BALANCE** - Phí theo % số dư
   - Tính toán: % của số dư, có min/max cap
   - Ví dụ: 0.1% số dư, tối thiểu 10k, tối đa 100k

**Business Rules:**
- Chỉ hiển thị fee types đang active
- Fee types là master data, không cho phép tạo/sửa/xóa qua API

**API:** `GET /api/v1/fee-types`

---

#### 2.2 Xem chi tiết loại phí
**Mục đích:** Xem thông tin chi tiết của 1 loại phí

**Input:**
- Fee Type ID

**Output:**
- Thông tin chi tiết fee type

**API:** `GET /api/v1/fee-types/{id}`

---

### 3. Quản lý Cấu hình phí (Fee Configuration Management)

#### 3.1 Tạo cấu hình phí cho khách hàng
**Mục đích:** Thiết lập chính sách tính phí cho khách hàng

**Input:**
- Customer ID
- Fee Type ID
- Số tiền phí cơ bản (monthlyFeeAmount)
- Đơn vị tiền tệ (currency)
- Ngày bắt đầu hiệu lực (effectiveFrom)
- Ngày kết thúc hiệu lực (effectiveTo) - optional
- Tham số tính toán (calculationParams) - JSON

**Output:**
- Thông tin cấu hình phí đã tạo
- Config ID

**Business Rules:**
- Customer và FeeType phải tồn tại
- effectiveFrom < effectiveTo (nếu có effectiveTo)
- Không được overlap với config khác của cùng customer
- Một customer chỉ có 1 config active tại 1 thời điểm

**Validation Logic:**
```
Config A: 2025-01-01 → 2025-03-31
Config B: 2025-02-01 → 2025-05-31
→ OVERLAP → Reject

Config A: 2025-01-01 → 2025-03-31
Config B: 2025-04-01 → 2025-06-30
→ OK → Accept
```

**API:** `POST /api/v1/fee-configs`

**Example Request:**
```json
{
  "customer_id": 13,
  "fee_type_id": 3,
  "monthly_fee_amount": 10000,
  "currency": "VND",
  "effective_from": "2025-09-01",
  "effective_to": "2035-09-01",
  "calculation_params": {
    "balance": 100000000,
    "percentage": 0.001,
    "min_fee": 5000,
    "max_fee": 50000
  }
}

```

---

#### 3.2 Xem cấu hình phí đang active
**Mục đích:** Xem cấu hình phí hiện đang áp dụng cho khách hàng

**Input:**
- Customer ID

**Output:**
- Cấu hình phí đang active

**Business Rules:**
- Chỉ trả về config có: effectiveFrom <= today <= effectiveTo
- Không trả về config đã xóa

**API:** `GET /api/v1/fee-configs/customer/{customerId}/active`

---

#### 3.3 Xem tất cả cấu hình phí của khách hàng
**Mục đích:** Xem lịch sử tất cả các cấu hình phí (bao gồm expired)

**Input:**
- Customer ID

**Output:**
- Danh sách tất cả configs (active, expired, future)

**API:** `GET /api/v1/fee-configs/customer/{customerId}`

---

#### 3.4 Xem chi tiết cấu hình phí
**Mục đích:** Xem thông tin chi tiết của 1 cấu hình

**Input:**
- Config ID

**Output:**
- Thông tin đầy đủ config

**API:** `GET /api/v1/fee-configs/{id}`

---

#### 3.5 Cập nhật cấu hình phí
**Mục đích:** Thay đổi thông tin cấu hình phí

**Input:**
- Config ID
- Thông tin cần cập nhật

**Output:**
- Thông tin config sau khi cập nhật

**Business Rules:**
- Validate overlap sau khi update
- Validate date range
- Cache bị evict sau khi update

**API:** `PUT /api/v1/fee-configs/{id}`

---

#### 3.6 Xóa cấu hình phí
**Mục đích:** Xóa cấu hình phí (soft delete)

**Input:**
- Config ID

**Output:**
- Thông báo thành công

**Business Rules:**
- Soft delete
- Cache bị evict

**API:** `DELETE /api/v1/fee-configs/{id}`

---

#### 3.7 Preview tính phí
**Mục đích:** Xem trước số tiền phí sẽ được tính (không thực sự charge)

**Input:**
- Customer ID
- Calculation Params (optional - nếu không có dùng params từ config)

**Output:**
- Customer ID
- Fee Type info
- Số tiền phí base
- Số tiền phí được tính (calculated)
- Params đã dùng để tính

**Use Cases:**
- Customer muốn biết trước sẽ bị tính bao nhiêu
- Admin kiểm tra logic tính phí
- Testing cấu hình mới

**API:** `POST /api/v1/fee-configs/preview`

**Example Request:**
```json
{
  "customer_id": 5,
  "calculation_params": {
    "balance": 175000000,
    "tiers": [
      {"from": 0, "to": 50000000, "fee": 10000},
      {"from": 50000001, "to": 100000000, "fee": 20000},
      {"from": 100000001, "to": null, "fee": 30000}
    ]
  }
}
```

**Example Response:**
```json
{
  "response_code": "00",
  "response_message": "SUCCESS",
  "response_id": "2393f5ed-d09f-4c0e-a37c-e325b03b6e16",
  "response_time": "2026-01-07T04:04:33.707857Z",
  "data": {
    "customer_id": 5,
    "fee_type_code": "TIERED_BALANCE",
    "fee_type_name": "Phí theo bậc số dư",
    "calculation_type": "TIERED",
    "monthly_fee_amount": null,
    "calculated_fee": 30000,
    "currency": "VND",
    "calculation_params": {
      "balance": 175000000,
      "tiers": [
        {
          "from": 0,
          "to": 50000000,
          "fee": 10000
        },
        {
          "from": 50000001,
          "to": 100000000,
          "fee": 20000
        },
        {
          "from": 100000001,
          "to": null,
          "fee": 30000
        }
      ]
    }
  }
}
```

---

### 4. Quản lý Fee Jobs (Fee Job Management)

#### 4.1 Xem danh sách fee jobs
**Mục đích:** Monitoring các jobs tính phí, hỗ trợ filter

**Input:**
- customerId (optional)
- billingMonth (optional) - Format: YYYY-MM
- status (optional) - NEW, PROCESSING, DONE, FAILED

**Output:**
- Danh sách jobs phù hợp

**Use Cases:**
- Admin monitoring jobs hàng tháng
- Kiểm tra job của 1 customer cụ thể
- Tìm jobs failed để xử lý

**API:** `GET /api/v1/fee-jobs?customerId=1&billingMonth=2025-01&status=DONE`

---

#### 4.2 Xem chi tiết fee job
**Mục đích:** Xem thông tin chi tiết của 1 job

**Input:**
- Job ID

**Output:**
- Thông tin job đầy đủ
- Customer ID
- Billing month
- Status
- Scheduled time
- Processed time

**API:** `GET /api/v1/fee-jobs/{jobId}`

---

#### 4.3 Xem lịch sử charge attempts của job
**Mục đích:** Xem tất cả các lần thử charge của 1 job

**Input:**
- Job ID

**Output:**
- Danh sách attempts
- Mỗi attempt: số tiền, status, error message, timestamp

**Use Cases:**
- Debug job failed
- Audit trail
- Kiểm tra retry history

**API:** `GET /api/v1/fee-jobs/{jobId}/attempts`

---

### 5. Monitoring Fee Charges

#### 5.1 Xem danh sách charges thất bại
**Mục đích:** Monitoring các lần charge bị lỗi để xử lý thủ công

**Input:**
- billingMonth (optional)

**Output:**
- Danh sách attempts có status = FAILED
- Error messages

**Use Cases:**
- Daily check charges failed
- Manual retry
- Root cause analysis

**API:** `GET /api/v1/fee-charges/failures?billingMonth=2025-01`

---

#### 5.2 Thống kê charges theo tháng
**Mục đích:** Dashboard thống kê doanh thu từ phí

**Input:**
- billingMonth (required) - Format: YYYY-MM

**Output:**
- Tổng số attempts
- Số lượng thành công
- Số lượng thất bại
- Tổng số tiền đã thu

**Use Cases:**
- Monthly report
- Business analytics
- Performance monitoring

**API:** `GET /api/v1/fee-charges/stats?billingMonth=2026-01`

**Example Response:**
```json
{
  "response_code": "00",
  "response_message": "SUCCESS",
  "response_id": "b88a27f9-6b20-4547-9c4e-0930c090eb6c",
  "response_time": "2026-01-07T04:06:13.451109Z",
  "data": {
    "billing_month": "2026-01",
    "total_jobs": 7,
    "jobs_done": 7,
    "jobs_failed": 0,
    "jobs_pending": 0,
    "total_attempts": 7,
    "attempts_success": 7,
    "attempts_failed": 0
  }
}
```

---

#### 5.3 Xem lịch sử charges của khách hàng
**Mục đích:** Customer service tra cứu lịch sử phí của khách hàng

**Input:**
- Customer ID

**Output:**
- Danh sách tất cả charges của customer
- Sắp xếp theo thời gian giảm dần

**Use Cases:**
- Customer hỏi "tôi bị tính phí gì?"
- Dispute resolution
- Audit trail

**API:** `GET /api/v1/fee-charges/customer/{customerId}`

---

### 6. Tính phí Tự động (Automated Fee Charging)

#### 6.1 Scheduler tạo jobs hàng tháng
**Chức năng:** Tự động tạo fee jobs cho tất cả customers vào đầu tháng

**Thời gian chạy:** Mùng 1 hàng tháng lúc 01:00 AM

**Flow:**
```
1. Acquire distributed lock
2. Lấy danh sách tất cả customers active
3. For each customer:
   - Kiểm tra đã có job cho tháng này chưa (idempotency)
   - Nếu chưa có: tạo job mới với status = NEW
4. Release lock
5. Log kết quả
```

**Business Rules:**
- Chỉ tạo job cho customer có status = ACTIVE
- Idempotency: không tạo duplicate jobs
- Distributed lock: đảm bảo chỉ 1 instance chạy

**Implementation:** `FeeJobPrepareScheduler`

---

#### 6.2 Scheduler execute jobs
**Chức năng:** Tự động execute các jobs đã tạo

**Thời gian chạy:** Mỗi 5 phút

**Flow:**
```
1. Acquire distributed lock
2. Lấy danh sách jobs có status = NEW
3. For each job:
   a. Update status = PROCESSING
   b. Load customer info
   c. Load active fee config
   d. Load fee type
   e. Calculate fee (Strategy pattern)
   f. Create FeeChargedAttempt record
   g. Publish Kafka event
   h. Update status = DONE
   i. Log success
4. Release lock
```

**Error Handling:**
- Nếu job fail: status = FAILED, log error
- Continue với jobs khác
- Không tự động retry (cần manual intervention)

**Implementation:** `FeeJobExecuteScheduler`

---

### 7. Kafka Event Processing

#### 7.1 Publish fee charged events
**Chức năng:** Sau khi charge thành công, publish event lên Kafka

**Topic:** `payment.fee.charged.v1`

**Event Schema:**
```json
{
  "job_id": 123,
  "customer_id": 456,
  "charged_amount": 50000,
  "currency": "VND",
  "billing_month": "2025-01",
  "charged_at": "2026-01-06T10:00:00Z"
}
```

**Use Cases:**
- Downstream services lắng nghe (accounting, notification, etc.)
- Audit trail
- Real-time analytics

**Implementation:** `FeeChargedProducer`

---

#### 7.2 Consume và xử lý events
**Chức năng:** Consume events từ main topic, xử lý và retry nếu lỗi

**Topic:** `payment.fee.charged.v1`

**Flow:**
```
1. Consume event
2. Process (log, update external system, send notification, etc.)
3. Nếu thành công: Done
4. Nếu lỗi:
   - Retry 1: sau 1s
   - Retry 2: sau 2s
   - Retry 3: sau 4s
   - Nếu vẫn fail: send to retry topic
```

**Retry Strategy:**
- Exponential backoff: 1s, 2s, 4s
- Max 3 attempts
- Dùng @Retryable annotation

**Implementation:** `FeeChargedConsumer`

---

#### 7.3 Re-process failed events
**Chức năng:** Consumer cho retry topic, xử lý lại events đã fail

**Topic:** `payment.fee.charged.retry.v1`

**Flow:**
```
1. Consume from retry topic
2. Try process again
3. Nếu thành công: Done
4. Nếu vẫn fail: send to DLQ
```

**Use Cases:**
- External service tạm thời down
- Network timeout
- Rate limiting

**Implementation:** `FeeChargedRetryConsumer`

---

#### 7.4 Dead Letter Queue handling
**Chức năng:** Log events không thể process được, cần manual fix

**Topic:** `payment.fee.charged.dlq.v1`

**Flow:**
```
1. Consume from DLQ
2. Log ERROR với full context
3. Không retry nữa
4. Alert admin
```

**Manual Handling:**
- Admin check logs
- Identify root cause
- Fix issue (code bug, data corruption, etc.)
- Manually replay event nếu cần

**Implementation:** `FeeChargedDLQConsumer`

---

### 8. Caching

#### 8.1 Fee Types caching
**Chức năng:** Cache fee types để giảm database load

**Cache Name:** `feeTypes`

**TTL:** 24 hours

**Keys:**
- `feeTypes::all` - Tất cả fee types
- `feeTypes::{id}` - Fee type by ID

**Eviction:** Không có (master data ít thay đổi)

---

#### 8.2 Fee Configs caching
**Chức năng:** Cache fee configs để tăng performance

**Cache Name:** `feeConfigs`

**TTL:** 30 minutes

**Keys:**
- `feeConfigs::{configId}` - By config ID
- `feeConfigs::customerId:{id}:active` - Active config của customer
- `feeConfigs::customerId:{id}:all` - All configs của customer

**Eviction:**
- @CacheEvict khi create/update/delete config
- Strategy: evict all entries (allEntries=true)

**Reasoning:**
- Một config có nhiều cache keys
- Key-specific eviction phức tạp
- AllEntries đơn giản và đảm bảo correctness

---

## Security Features

### Authentication
- JWT token-based
- Token expiration: 1 hour
- Refresh token: 7 days

### Authorization
- Role-based (future enhancement)
- API key validation

### Input Validation
- Bean Validation (JSR-380)
- Email format
- Phone number format
- Date range validation
- Business rule validation

### Soft Delete
- Không xóa vật lý data
- Preserve audit trail
- Có thể restore nếu cần

---

## Reporting & Analytics

### 1. Monthly Fee Report
- Tổng số customers
- Tổng số jobs processed
- Success rate
- Total revenue

### 2. Customer Fee History
- Lịch sử tất cả charges
- Breakdown theo tháng
- Trend analysis

### 3. Failed Charges Report
- Danh sách charges failed
- Error categories
- Action items

---

## Use Case Scenarios

### Scenario 1: Khách hàng mới mở tài khoản VIP
```
1. POST /api/v1/customers
   - Tạo customer mới

2. POST /api/v1/fee-configs
   - Set fee type: FIXED_MONTHLY
   - Amount: 100,000 VND/tháng
   - Effective from: today
   - Effective to: null (vô thời hạn)

3. Scheduler tự động:
   - Mùng 1 hàng tháng: tạo job
   - Mỗi 5 phút: execute job → charge 100k

4. Customer nhận notification
```

### Scenario 2: Thay đổi chính sách phí từ Fixed sang Tiered
```
1. PUT /api/v1/fee-configs/{oldConfigId}
   - Set effectiveTo = 2025-03-31 (kết thúc config cũ)

2. POST /api/v1/fee-configs
   - Fee type: TIERED_BALANCE
   - Effective from: 2025-04-01
   - Tiers: [0-10M: 0, 10M-50M: 20k, 50M+: 50k]

3. Từ tháng 4:
   - Scheduler dùng config mới
   - Phí tính theo số dư
```

### Scenario 3: Customer dispute về phí
```
1. GET /api/v1/fee-charges/customer/{customerId}
   - Xem lịch sử charges

2. GET /api/v1/fee-jobs/{jobId}/attempts
   - Xem chi tiết job

3. GET /api/v1/fee-configs/{configId}
   - Xem config đã áp dụng

4. POST /api/v1/fee-configs/preview
   - Re-calculate để verify

5. Giải quyết dispute dựa trên evidence
```

### Scenario 4: Monthly reconciliation
```
1. GET /api/v1/fee-charges/stats?billingMonth=2025-01
   - Lấy thống kê tháng 1

2. GET /api/v1/fee-charges/failures?billingMonth=2025-01
   - Check charges failed

3. Manual retry hoặc investigate

4. Export report cho finance team
```

---
