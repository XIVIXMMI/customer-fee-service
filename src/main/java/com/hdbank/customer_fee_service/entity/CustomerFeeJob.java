package com.hdbank.customer_fee_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Customer_fee_job đại diện cho MỘT LẦN cần thu phí khách hàng trong 1 kì billing_month cụ thể
 * Ví dụ:
 * - Khách hàng 123 cần thu phí tháng 01/2025 → 1 job
 * - Khách hàng 123 cần thu phí tháng 02/2025 → 1 job khác
 * Số tiền amount được tính dựa trên customer_fee_config (còn hiệu lực)
 * Bao gồm fee_type_id và calculation_params
 */
@Entity
@Table(name = "customer_fee_job")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFeeJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;  // Format: yyyy-MM (e.g., "2025-01")

    @Column(name = "amount", nullable = true, precision = 15, scale = 2)
    private BigDecimal amount;  // null khi status = NEW, có giá trị khi DONE

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeeJobStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;  // Format: {customer_id}_{billing_month}

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
