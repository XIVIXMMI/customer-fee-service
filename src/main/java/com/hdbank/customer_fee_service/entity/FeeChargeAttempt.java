package com.hdbank.customer_fee_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lưu lại log cho MÔI LẦN hệ thống thực hiện thu phí cho 1 customer_fee_job (SUCCESS or FAILED)
 * Nhờ đó có thể theo dõi lịch sử thu phí, số lần thử lại, mã lỗi nếu có
 */
@Entity
@Table(name = "fee_charge_attempt")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeChargeAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId; // avoids N + 1 query issue

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttemptStatus status;  // SUCCESS, FAILED

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "external_txn_id")
    private String externalTxnId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}