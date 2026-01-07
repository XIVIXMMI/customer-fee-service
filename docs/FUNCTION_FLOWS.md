# Customer Fee Service - Function Flows

## Mục đích

Document này mô tả chi tiết flow các functions gọi nhau trong hệ thống, giúp developers hiểu rõ luồng xử lý từ API endpoint đến database.

---

## Flow Patterns

### Pattern 1: Controller → Service → Repository → Database
```
Controller (REST endpoint)
    ↓ Validate request
Service (Business logic)
    ↓ Process data
Repository (Data access)
    ↓ Execute query
Database (PostgreSQL)
```

### Pattern 2: Scheduler → Service → Kafka
```
Scheduler (Cron job)
    ↓ Acquire lock
Service (Business logic)
    ↓ Process batch
Kafka Producer (Publish event)
    ↓ Send to topic
Kafka Consumer (Process event)
```

---

## Detailed Function Flows

### 1. Create Customer Flow

**API Endpoint:** `POST /api/v1/customers`

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. CustomerController.createCustomer(request)                   │
│    Path: controller/CustomerController.java:73                  │
│    Input: CreateCustomerRequest                                 │
│    Annotation: @PostMapping, @Valid                             │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. @Valid Bean Validation                                       │
│    - Validate fullName (not null, max 255 chars)                │
│    - Validate email (not null, email format)                    │
│    - Validate phoneNumber (pattern)                             │
│    - Validate status (ACTIVE/INACTIVE)                          │
│    If invalid → MethodArgumentNotValidException                 │
│                → GlobalExceptionHandler                         │
│                → Return 400 Bad Request                         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. CustomerService.createCustomer(request)                      │
│    Path: service/CustomerService.java:32                        │
│    Function signature:                                          │
│    public CustomerResponse createCustomer(                      │
│        CreateCustomerRequest request                            │
│    )                                                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Build Customer Entity                                        │
│    Customer customer = Customer.builder()                       │
│        .fullName(request.getFullName())                         │
│        .email(request.getEmail())                               │
│        .phoneNumber(request.getPhoneNumber())                   │
│        .status(request.getStatus())                             │
│        .build();                                                │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. CustomerRepository.save(customer)                            │
│    Path: repository/CustomerRepository.java                     │
│    - JPA auto-generates ID                                      │
│    - BaseEntity auto-fills createdAt, updatedAt                 │
│    - Execute: INSERT INTO customer (...)                        │
│    - Return: Customer entity with ID                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. CustomerResponse.from(savedCustomer)                         │
│    - Convert entity to DTO                                      │
│    - Map fields: id, fullName, email, phoneNumber, status       │
│    - Add timestamps: createdAt, updatedAt                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. Return Response                                              │
│    ResponseEntity<ApiDataResponse<CustomerResponse>>            │
│    - Status: 201 CREATED                                        │
│    - Body: {                                                    │
│        "response_code": "00",                                   │
│        "response_message": "Customer created successfully",     │
│        "data": { customer info }                                │
│      }                                                          │
└─────────────────────────────────────────────────────────────────┘
```

**Error Handling:**
```
CustomerService.save()
    ↓ throws DataIntegrityViolationException (duplicate email)
GlobalExceptionHandler.handleDataIntegrityViolation()
    ↓
Return 400 Bad Request
{
  "response_code": "01",
  "response_message": "Email already exists"
}
```

---

### 2. Create Fee Config Flow (Complex)

**API Endpoint:** `POST /api/v1/fee-configs`

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. CustomerFeeConfigController.createFeeConfig(request)         │
│    Path: controller/CustomerFeeConfigController.java:45         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. FeeConfigService.createFeeConfig(request)                    │
│    Path: service/FeeConfigService.java:43                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Validate Customer Exists                                     │
│    Customer customer = customerRepository                       │
│        .findById(request.getCustomerId())                       │
│        .filter(c -> c.getDeletedAt() == null)                   │
│        .orElseThrow(() -> new EntityNotFoundException(          │
│            "Customer not found with id: " + customerId          │
│        ));                                                      │
│    → If not found: throw exception → 404 response               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Validate FeeType Exists and Active                           │
│    FeeType feeType = feeTypeRepository                          │
│        .findById(request.getFeeTypeId())                        │
│        .filter(FeeType::getIsActive)                            │
│        .orElseThrow(() -> new EntityNotFoundException(          │
│            "Fee type not found or inactive"                     │
│        ));                                                      │
│    → If not found/inactive: throw exception → 404 response      │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Validate Date Range                                          │
│    if(request.getEffectiveTo() != null &&                       │
│       request.getEffectiveFrom()                                │
│           .isAfter(request.getEffectiveTo())) {                 │
│        throw new ValidationException(                           │
│            "Effective from must be before effective to"         │
│        );                                                       │
│    }                                                            │
│    → If invalid: throw exception → 400 response                 │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. checkOverlappingConfigs()                                    │
│    Path: service/FeeConfigService.java:210                      │
│    Input:                                                       │
│    - customerId                                                 │
│    - effectiveFrom                                              │
│    - effectiveTo                                                │
│    - excludeConfigId = null (vì đang create)                    │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6.1 Get Existing Configs                                        │
│     List<CustomerFeeConfig> existingConfigs =                   │
│         feeConfigRepository                                     │
│             .findByCustomerIdAndDeletedAtIsNull(customerId);    │
│     → Execute: SELECT * FROM customer_fee_config                │
│                WHERE customer_id = ? AND deleted_at IS NULL     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6.2 Loop Through Existing Configs                               │
│     for (CustomerFeeConfig existing : existingConfigs) {        │
│         // Skip if checking against itself (for update case)    │
│         if (existing.getId().equals(excludeConfigId)) {         │
│             continue;                                           │
│         }                                                       │
│                                                                 │
│         LocalDate existingFrom = existing.getEffectiveFrom();   │
│         LocalDate existingTo = existing.getEffectiveTo();       │
│                                                                 │
│         boolean hasOverlap = isDateRangeOverlap(                │
│             effectiveFrom, effectiveTo,                         │
│             existingFrom, existingTo                            │
│         );                                                      │
│                                                                 │
│         if (hasOverlap) {                                       │
│             throw new ValidationException(                      │
│                 "Fee config overlaps with existing config (id: "│
│                 + existing.getId() + ")"                        │
│             );                                                  │
│         }                                                       │
│     }                                                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6.3 isDateRangeOverlap() Logic                                  │
│     Path: service/FeeConfigService.java:238                     │
│                                                                 │
│     private boolean isDateRangeOverlap(                         │
│         LocalDate from1, LocalDate to1,                         │
│         LocalDate from2, LocalDate to2                          │
│     ) {                                                         │
│         // Handle null effectiveTo (= vô thời hạn)              │
│         LocalDate end1 = (to1 != null) ? to1 : LocalDate.MAX;   │
│         LocalDate end2 = (to2 != null) ? to2 : LocalDate.MAX;   │
│                                                                 │
│         // Overlap if: start1 <= end2 AND start2 <= end1        │
│         return !from1.isAfter(end2) && !from2.isAfter(end1);    │
│     }                                                           │
│                                                                 │
│     Example:                                                    │
│     Config A: 2025-01-01 to 2025-03-31                          │
│     Config B: 2025-02-01 to 2025-05-31                          │
│     → 2025-01-01 <= 2025-05-31 ✓                                │
│     → 2025-02-01 <= 2025-03-31 ✓                                │
│     → OVERLAP = true → Throw exception                          │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. Build and Save Config                                        │
│    CustomerFeeConfig config = CustomerFeeConfig.builder()       │
│        .customerId(request.getCustomerId())                     │
│        .feeTypeId(request.getFeeTypeId())                       │
│        .monthlyFeeAmount(request.getMonthlyFeeAmount())         │
│        .currency(request.getCurrency())                         │
│        .effectiveFrom(request.getEffectiveFrom())               │
│        .effectiveTo(request.getEffectiveTo())                   │
│        .calculationParams(request.getCalculationParams())       │
│        .build();                                                │
│                                                                 │
│    CustomerFeeConfig saved =                                    │
│        customerFeeConfigRepository.save(config);                │
│    → Execute: INSERT INTO customer_fee_config (...)             │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8. Evict Cache                                                  │
│    @CacheEvict(value = "feeConfigs", allEntries = true)         │
│    - Clear all cache entries in "feeConfigs" cache              │
│    - Ensures fresh data on next read                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 9. Return Response                                              │
│    return FeeConfigResponse.from(saved);                        │
│    Status: 201 CREATED                                          │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3. Fee Preview Flow (Strategy Pattern)

**API Endpoint:** `POST /api/v1/fee-configs/preview`

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. CustomerFeeConfigController.feePreview(request)              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. FeeConfigService.feeReview(request)                          │
│    Path: service/FeeConfigService.java:170                      │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Get Active Fee Config                                        │
│    LocalDate today = LocalDate.now();                           │
│    CustomerFeeConfig config =                                   │
│        customerFeeConfigRepository                              │
│            .findActiveConfigByCustomerIdAndDate(                │
│                request.getCustomerId(),                         │
│                today                                            │
│            )                                                    │
│            .orElseThrow(() ->                                   │
│                new EntityNotFoundException(                     │
│                    "No active config found"                     │
│                )                                                │
│            );                                                   │
│    → Execute custom query:                                      │
│      SELECT * FROM customer_fee_config                          │
│      WHERE customer_id = ?                                      │
│        AND deleted_at IS NULL                                   │
│        AND effective_from <= ?                                  │
│        AND (effective_to IS NULL OR effective_to >= ?)          │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Get Fee Type                                                 │
│    FeeType feeType = feeTypeRepository                          │
│        .findById(config.getFeeTypeId())                         │
│        .orElseThrow(() ->                                       │
│            new EntityNotFoundException("Fee type not found")    │
│        );                                                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Merge Calculation Params                                     │
│    Map<String, Object> calculationParams =                      │
│        request.getCalculationParams() != null                   │
│            ? request.getCalculationParams()                     │
│            : config.getCalculationParams();                     │
│                                                                 │
│    // Use params from request if provided,                      │
│    // otherwise use params from config                          │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. FeeCalculationContext.calculateFee()                         │
│    Path: service/strategy/FeeCalculationContext.java:31         │
│    Input:                                                       │
│    - calculationType: "FIXED" | "TIERED" | "PERCENTAGE"         │
│    - monthlyFeeAmount: 50000                                    │
│    - calculationParams: { balance: 75000000, ... }              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. Get Strategy Implementation                                  │
│    FeeCalculationStrategy strategy =                            │
│        strategies.get(calculationType);                         │
│                                                                 │
│    if (strategy == null) {                                      │
│        throw new IllegalArgumentException(                      │
│            "Unknown calculation type: " + calculationType       │
│        );                                                       │
│    }                                                            │
│                                                                 │
│    // strategies Map is auto-populated by Spring:               │
│    // - "FIXED" → FixedMonthlyFeeStrategy                       │
│    // - "TIERED" → TieredBalanceFeeStrategy                     │
│    // - "PERCENTAGE" → PercentageBalanceFeeStrategy             │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼ (Example: TIERED type)
┌─────────────────────────────────────────────────────────────────┐
│ 8. TieredBalanceFeeStrategy.calculate()                         │
│    Path: service/strategy/TieredBalanceFeeStrategy.java:20      │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8.1 Validate Required Params                                    │
│     strategy.validate(params);                                  │
│                                                                 │
│     if (!params.containsKey("balance")) {                       │
│         throw new ValidationException(                          │
│             "balance is required"                               │
│         );                                                      │
│     }                                                           │
│     if (!params.containsKey("tiers") ||                         │
│         ((List)params.get("tiers")).isEmpty()) {                │
│         throw new ValidationException(                          │
│             "tiers is required and must not be empty"           │
│         );                                                      │
│     }                                                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8.2 Extract Params                                              │
│     double balance = ((Number) params.get("balance"))           │
│         .doubleValue();                                         │
│     List<Map<String, Object>> tiers =                           │
│         (List<Map<String, Object>>) params.get("tiers");        │
│                                                                 │
│     Example tiers:                                              │
│     [                                                           │
│       {"max": 10000000, "fee": 0},                              │
│       {"max": 50000000, "fee": 20000},                          │
│       {"max": 100000000, "fee": 50000}                          │
│     ]                                                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8.3 Find Matching Tier                                          │
│     for (Map<String, Object> tier : tiers) {                    │
│         double maxBalance = ((Number) tier.get("max"))          │
│             .doubleValue();                                     │
│                                                                 │
│         if (balance <= maxBalance) {                            │
│             BigDecimal fee = new BigDecimal(                    │
│                 tier.get("fee").toString()                      │
│             );                                                  │
│             return fee;                                         │
│         }                                                       │
│     }                                                           │
│                                                                 │
│     Example: balance = 75,000,000                               │
│     - Check tier 1: 75M <= 10M? NO                              │
│     - Check tier 2: 75M <= 50M? NO                              │
│     - Check tier 3: 75M <= 100M? YES → return 50,000            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8.4 Fallback to Monthly Fee                                     │
│     // If no tier matches (balance > all max values)            │
│     return monthlyFeeAmount;                                    │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 9. Build Preview Response                                       │
│    return FeePreviewResponse.builder()                          │
│        .customerId(request.getCustomerId())                     │
│        .feeTypeCode(feeType.getCode())                          │
│        .feeTypeName(feeType.getName())                          │
│        .calculationType(feeType.getCalculationType())           │
│        .monthlyFeeAmount(config.getMonthlyFeeAmount())          │
│        .calculatedFee(calculationFee)  // Result from strategy  │
│        .currency(config.getCurrency())                          │
│        .calculationParams(calculationParams)                    │
│        .build();                                                │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 10. Return Response                                             │
│     Status: 200 OK                                              │
│     Body: {                                                     │
│       "customerId": 1,                                          │
│       "feeTypeCode": "TIERED_BALANCE",                          │
│       "calculatedFee": 50000,                                   │
│       ...                                                       │
│     }                                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

### 4. Charge Fee Flow (Core Business Logic)

**Trigger:** `FeeJobExecuteScheduler` 

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FeeChargeService.chargeFee(jobId)                            │
│    Path: service/FeeChargeService.java:43                       │
│    Input: jobId (Long)                                          │
│    Annotation: @Transactional                                   │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Load and Validate Job                                        │
│    CustomerFeeJob job = feeJobRepository.findById(jobId)        │
│        .orElseThrow(() ->                                       │
│            new EntityNotFoundException("Job not found")         │
│        );                                                       │
│                                                                 │
│    if (job.getStatus() != FeeJobStatus.NEW) {                   │
│        throw new BusinessException(                             │
│            "Job status must be NEW, current: "                  │
│            + job.getStatus()                                    │
│        );                                                       │
│    }                                                            │
│                                                                 │
│    // Idempotency: prevent reprocessing                         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Load Customer                                                │
│    Customer customer = customerRepository                       │
│        .findById(job.getCustomerId())                           │
│        .orElseThrow(() ->                                       │
│            new EntityNotFoundException("Customer not found")    │
│        );                                                       │
│                                                                 │
│    // Validate customer is active                               │
│    if (!"ACTIVE".equals(customer.getStatus())) {                │
│        throw new BusinessException(                             │
│            "Customer is not active"                             │
│        );                                                       │
│    }                                                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Load Active Fee Config                                       │
│    LocalDate billingDate = LocalDate.parse(                     │
│        job.getBillingMonth() + "-01"                            │
│    );                                                           │
│                                                                 │
│    CustomerFeeConfig config = feeConfigRepository               │
│        .findActiveConfigByCustomerIdAndDate(                    │
│            job.getCustomerId(),                                 │
│            billingDate                                          │
│        )                                                        │
│        .orElseThrow(() ->                                       │
│            new EntityNotFoundException(                         │
│                "No active fee config found"                     │
│            )                                                    │
│        );                                                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Load Fee Type                                                │
│    FeeType feeType = feeTypeRepository                          │
│        .findById(config.getFeeTypeId())                         │
│        .orElseThrow(() ->                                       │
│            new EntityNotFoundException("Fee type not found")    │
│        );                                                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. Calculate Fee (Strategy Pattern)                             │
│    BigDecimal calculatedFee = feeCalculationContext             │
│        .calculateFee(                                           │
│            feeType.getCalculationType(),                        │
│            config.getMonthlyFeeAmount(),                        │
│            config.getCalculationParams()                        │
│        );                                                       │
│                                                                 │
│    // Delegates to appropriate strategy based on type           │
│    // See "Fee Preview Flow" for detailed strategy logic        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. Create Fee Charged Attempt                                   │
│    FeeChargedAttempt attempt = FeeChargedAttempt.builder()      │
│        .feeJobId(jobId)                                         │
│        .chargedAmount(calculatedFee)                            │
│        .currency(config.getCurrency())                          │
│        .status(AttemptStatus.SUCCESS)                           │
│        .attemptedAt(Instant.now())                              │
│        .build();                                                │
│                                                                 │
│    feeChargedAttemptRepository.save(attempt);                   │
│    → Execute: INSERT INTO fee_charged_attempt (...)             │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8. Publish Kafka Event                                          │
│    FeeChargedEvent event = FeeChargedEvent.builder()            │
│        .jobId(jobId)                                            │
│        .customerId(customer.getId())                            │
│        .chargedAmount(calculatedFee)                            │
│        .currency(config.getCurrency())                          │
│        .billingMonth(job.getBillingMonth())                     │
│        .chargedAt(Instant.now())                                │
│        .build();                                                │
│                                                                 │
│    feeChargedProducer.sendFeeChargedEvent(event);               │
│    → See "Kafka Flow" for details                               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 9. Update Job Status                                            │
│    job.setStatus(FeeJobStatus.DONE);                            │
│    job.setProcessedAt(Instant.now());                           │
│    feeJobRepository.save(job);                                  │
│    → Execute: UPDATE customer_fee_job                           │
│                SET status = 'DONE', processed_at = ?            │
│                WHERE id = ?                                     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 10. Build and Return Result                                     │
│     return FeeChargeResult.builder()                            │
│         .status("SUCCESS")                                      │
│         .customerId(customer.getId())                           │
│         .chargedAmount(calculatedFee)                           │
│         .currency(config.getCurrency())                         │
│         .build();                                               │
└─────────────────────────────────────────────────────────────────┘
```

**Error Handling Flow:**
```
Any exception in chargeFee()
    ↓
@Transactional rollback
    ↓ (all DB changes reverted)
Log error
    ↓
Update job status = FAILED
    ↓
Save error message to attempt
    ↓
Return or re-throw
```

---

### 5. Scheduler Flow

#### 5.1 Prepare Jobs Scheduler

**Trigger:** Cron `0 0 1 * * *` (01:00 AM mùng 1 hàng tháng)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FeeJobPrepareScheduler.prepareMonthlyFeeJobs()               │
│    Path: scheduler/FeeJobPrepareScheduler.java:30               │
│    Annotation: @Scheduled(cron = "0 0 1 * * *")                 │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Acquire Distributed Lock                                     │
│    boolean acquired = distributedLockService                    │
│        .acquireLock(PREPARE_FEE_JOBS_LOCK_ID);                  │
│                                                                 │
│    if (!acquired) {                                             │
│        log.warn("Could not acquire lock, another instance is    │
│                  running");                                     │
│        return; // Exit early                                    │
│    }                                                            │
│                                                                 │
│    // Lock ID = 1001                                            │
│    → Execute: SELECT pg_try_advisory_lock(1001)                 │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Try-Finally Block                                            │
│    try {                                                        │
│        // Main logic                                            │
│    } finally {                                                  │
│        distributedLockService.releaseLock(                      │
│            PREPARE_FEE_JOBS_LOCK_ID                             │
│        );                                                       │
│        // Always release lock                                   │
│    }                                                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Get Current Billing Month                                    │
│    String billingMonth = LocalDate.now()                        │
│        .format(DateTimeFormatter.ofPattern("yyyy-MM"));         │
│    // Example: "2025-01"                                        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Get All Active Customers                                     │
│    List<Customer> activeCustomers = customerRepository          │
│        .findByStatusAndDeletedAtIsNull("ACTIVE");               │
│    → Execute: SELECT * FROM customer                            │
│                WHERE status = 'ACTIVE'                          │
│                  AND deleted_at IS NULL                         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. Loop Through Customers                                       │
│    int createdCount = 0;                                        │
│    for (Customer customer : activeCustomers) {                  │
│        // Process each customer                                 │
│    }                                                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6.1 Check if Job Already Exists (Idempotency)                   │
│     Optional<CustomerFeeJob> existingJob =                      │
│         feeJobRepository.findByCustomerIdAndBillingMonth(       │
│             customer.getId(),                                   │
│             billingMonth                                        │
│         );                                                      │
│                                                                 │
│     if (existingJob.isPresent()) {                              │
│         log.debug("Job already exists for customer {}",         │
│                   customer.getId());                            │
│         continue; // Skip to next customer                      │
│     }                                                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6.2 Create New Job                                              │
│     CustomerFeeJob job = CustomerFeeJob.builder()               │
│         .customerId(customer.getId())                           │
│         .billingMonth(billingMonth)                             │
│         .status(FeeJobStatus.NEW)                               │
│         .scheduledAt(Instant.now())                             │
│         .idempotencyKey(customer.getId() + ":" + billingMonth)  │
│         .build();                                               │
│                                                                 │
│     feeJobRepository.save(job);                                 │
│     createdCount++;                                             │
│     → Execute: INSERT INTO customer_fee_job (...)               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. Log Summary                                                  │
│    log.info("Prepared {} fee jobs for billing month {}",        │
│             createdCount, billingMonth);                        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 8. Release Lock (in finally block)                              │
│    distributedLockService.releaseLock(                          │
│        PREPARE_FEE_JOBS_LOCK_ID                                 │
│    );                                                           │
│    → Execute: SELECT pg_advisory_unlock(1001)                   │
└─────────────────────────────────────────────────────────────────┘
```

#### 5.2 Execute Jobs Scheduler

**Trigger:** Cron `0 */5 * * * *` (Mỗi 5 phút)

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FeeJobExecuteScheduler.executePendingFeeJobs()               │
│    Path: scheduler/FeeJobExecuteScheduler.java:30               │
│    Annotation: @Scheduled(cron = "0 */5 * * * *")               │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Acquire Lock                                                 │
│    boolean acquired = distributedLockService                    │
│        .acquireLock(EXECUTE_FEE_JOBS_LOCK_ID);                  │
│    // Lock ID = 1002                                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Get Pending Jobs                                             │
│    List<CustomerFeeJob> pendingJobs = feeJobRepository          │
│        .findByStatus(FeeJobStatus.NEW);                         │
│    → Execute: SELECT * FROM customer_fee_job                    │
│                WHERE status = 'NEW'                             │
│                ORDER BY scheduled_at ASC                        │
│                LIMIT 100  // Batch size                         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Loop Through Jobs                                            │
│    int successCount = 0;                                        │
│    int failureCount = 0;                                        │
│                                                                 │
│    for (CustomerFeeJob job : pendingJobs) {                     │
│        // Process each job                                      │
│    }                                                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4.1 Process Single Job                                          │
│     try {                                                       │
│         log.info("Processing job {}", job.getId());             │
│                                                                 │
│         FeeChargeResult result =                                │
│             feeChargeService.chargeFee(job.getId());            │
│                                                                 │
│         successCount++;                                         │
│         log.info("Successfully charged customer {}: {}",        │
│                  result.getCustomerId(),                        │
│                  result.getChargedAmount());                    │
│                                                                 │
│     } catch (Exception e) {                                     │
│         failureCount++;                                         │
│         log.error("Failed to process job {}: {}",               │
│                   job.getId(), e.getMessage());                 │
│                                                                 │
│         // Update job status to FAILED                          │
│         job.setStatus(FeeJobStatus.FAILED);                     │
│         job.setProcessedAt(Instant.now());                      │
│         feeJobRepository.save(job);                             │
│                                                                 │
│         // Continue with next job (don't stop batch)            │
│     }                                                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. Log Summary                                                  │
│    log.info("Executed {} jobs: {} success, {} failed",          │
│             pendingJobs.size(), successCount, failureCount);    │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. Release Lock                                                 │
│    distributedLockService.releaseLock(                          │
│        EXECUTE_FEE_JOBS_LOCK_ID                                 │
│    );                                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

### 6. Kafka Event Flow

#### 6.1 Publish Event

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FeeChargedProducer.sendFeeChargedEvent(event)                │
│    Path: kafka/FeeChargedProducer.java:25                       │
│    Input: FeeChargedEvent                                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Convert Event to JSON                                        │
│    String eventJson = objectMapper.writeValueAsString(event);   │
│                                                                 │
│    Example:                                                     │
│    {                                                            │
│      "jobId": 123,                                              │
│      "customerId": 456,                                         │
│      "chargedAmount": 50000,                                    │
│      "currency": "VND",                                         │
│      "billingMonth": "2025-01",                                 │
│      "chargedAt": "2025-01-06T10:00:00Z"                        │
│    }                                                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Send to Kafka                                                │
│    kafkaTemplate.send(                                          │
│        KafkaConfig.TOPIC_FEE_CHARGED,  // Topic                 │
│        String.valueOf(event.getCustomerId()),  // Key           │
│        eventJson  // Value                                      │
│    );                                                           │
│                                                                 │
│    // Key = customerId ensures same customer events go to       │
│    // same partition (ordering guarantee)                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Kafka Broker                                                 │
│    Topic: payment.fee.charged.v1                                │
│    Partitions: 3                                                │
│    Replication: 1 (dev), 3 (prod)                               │
│                                                                 │
│    Partition selection:                                         │
│    hash(customerId) % 3 → Partition 0, 1, or 2                  │
└─────────────────────────────────────────────────────────────────┘
```

#### 6.2 Consume Event with Retry

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FeeChargedConsumer.consumeFeeChargedEvent()                  │
│    Path: kafka/FeeChargedConsumer.java:40                       │
│    Annotation: @KafkaListener(topics = "payment.fee.charged.v1")│
│    Annotation: @Retryable(maxAttempts = 3, backoff = ...)       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Parse JSON to Event                                          │
│    FeeChargedEvent event = objectMapper                         │
│        .readValue(eventJson, FeeChargedEvent.class);            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Track Retry Count                                            │
│    String key = headers.get("kafka_messageKey");                │
│    int retryCount = retryCountMap.getOrDefault(key, 0);         │
│    retryCountMap.put(key, retryCount + 1);                      │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. Process Event                                                │
│    log.info("Processing fee charged event for customer {}",     │
│             event.getCustomerId());                             │
│                                                                 │
│    // Business logic:                                           │
│    // - Update accounting system                                │
│    // - Send notification to customer                           │
│    // - Update analytics                                        │
│    // - etc.                                                    │
│                                                                 │
│    // Simulate external API call                                │
│    externalService.updateAccount(event);                        │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. If Success                                                   │
│    retryCountMap.remove(key);  // Clear retry count             │
│    log.info("Successfully processed event");                    │
└─────────────────────────────────────────────────────────────────┘
                     │
                     ▼ (If exception)
┌─────────────────────────────────────────────────────────────────┐
│ 6. @Retryable Mechanism                                         │
│    Attempt 1: Immediate                                         │
│        ↓ (fail)                                                 │
│    Wait 1s (delay = 1000)                                       │
│        ↓                                                        │
│    Attempt 2:                                                   │
│        ↓ (fail)                                                 │
│    Wait 2s (1000 * 2.0 multiplier)                              │
│        ↓                                                        │
│    Attempt 3:                                                   │
│        ↓ (fail)                                                 │
│    Max attempts reached                                         │
│        ↓                                                        │
│    @Recover method OR throw exception                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 7. After Max Retries Failed                                     │
│    if (retryCountMap.get(key) >= MAX_RETRY_ATTEMPTS) {          │
│        log.error("Max retries exceeded,                         │
│            sending to retry topic");                            │
│                                                                 │
│        // Send to retry topic                                   │
│        kafkaTemplate.send(                                      │
│            KafkaConfig.TOPIC_FEE_CHARGED_RETRY,                 │
│            key,                                                 │
│            eventJson                                            │
│        );                                                       │
│                                                                 │
│        retryCountMap.remove(key);                               │
│    }                                                            │
└─────────────────────────────────────────────────────────────────┘
```

#### 6.3 Retry Consumer

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FeeChargedRetryConsumer.consumeRetryEvent()                  │
│    Path: kafka/FeeChargedRetryConsumer.java:30                  │
│    Topic: payment.fee.charged.retry.v1                          │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Parse Event                                                  │
│    FeeChargedEvent event = objectMapper                         │
│        .readValue(eventJson, FeeChargedEvent.class);            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Try Process Again                                            │
│    try {                                                        │
│        log.info("Retrying event from retry topic");             │
│        externalService.updateAccount(event);                    │
│        log.info("Retry successful");                            │
│    }                                                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼ (If still fails)
┌─────────────────────────────────────────────────────────────────┐
│ 4. Send to DLQ                                                  │
│    catch (Exception e) {                                        │
│        log.error("Retry failed, sending to DLQ: {}",            │
│                  e.getMessage());                               │
│                                                                 │
│        kafkaTemplate.send(                                      │
│            KafkaConfig.TOPIC_FEE_CHARGED_DLQ,                   │
│            key,                                                 │
│            eventJson                                            │
│        );                                                       │
│    }                                                            │
└─────────────────────────────────────────────────────────────────┘
```

#### 6.4 DLQ Consumer

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. FeeChargedDLQConsumer.consumeDLQEvent()                      │
│    Path: kafka/FeeChargedDLQConsumer.java:25                    │
│    Topic: payment.fee.charged.dlq.v1                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. Parse Event                                                  │
│    FeeChargedEvent event = objectMapper                         │
│        .readValue(eventJson, FeeChargedEvent.class);            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. Log Error (No Processing)                                    │
│    log.error(                                                   │
│        "Event sent to DLQ - manual intervention required: {}",  │
│        event                                                    │
│    );                                                           │
│                                                                 │
│    // Alert monitoring system                                   │
│    // Send to admin dashboard                                   │
│    // Create ticket for manual investigation                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## Summary

**Key Flow Patterns:**

1. **API Request → Response**
   - Validation → Service Logic → Repository → DB → Response

2. **Scheduler**
   - Lock → Batch Process → Release Lock

3. **Kafka**
   - Produce → Consume → Retry → DLQ

4. **Strategy Pattern**
   - Context → Select Strategy → Execute → Return

5. **Error Handling**
   - Try-Catch → Log → Update Status → Alert

---
