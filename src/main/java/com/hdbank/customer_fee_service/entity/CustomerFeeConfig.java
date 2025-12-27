package com.hdbank.customer_fee_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Mỗi bản ghi CustomerFeeConfig đại diện cho 1 cấu hình phí áp dụng cho 1 khách hàng trong 1 khoảng thời gian xác định
 * Mỗi khách hàng chỉ có thể có tối đa 1 cấu hình phí đang hiệu lực tại 1 thời điểm
 * (effective_from - effective_to không chồng lấn)
 * fee_type_id cho biết rõ khách hàng đang bị tính phí gì (FIXED_MONTHLY, TIERED_BALANCE, PERCENTAGE_OF_BALANCE)
 * Khi cần đổi chính sách, tạo bản ghi mới với effective_from mới, thay vì update bản ghi cũ
 */
@Entity
@Table(name = "customer_fee_config")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFeeConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "fee_type_id", nullable = false)
    private Long feeTypeId;

    @Column(name = "monthly_fee_amount", precision = 15, scale = 2)
    private BigDecimal monthlyFeeAmount;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /**
     * JSONB field to store calculation parameters based on calculation type
     * E.g.
     * - TIERED: {"tiers": [{"max": 10000000, "fee": 0}, ...]}
     * - PERCENTAGE: {"rate": 0.001, "min_fee": 10000, "max_fee": 100000}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calculation_params", columnDefinition = "jsonb")
    private Map<String, Object> calculationParams;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Helper method: Check if config is currently effective
     */
    public boolean isEffectiveOn(LocalDate date) {
        if (deletedAt != null) return false;

        boolean afterStart = !date.isBefore(effectiveFrom);
        boolean beforeEnd = effectiveTo == null || !date.isAfter(effectiveTo);

        return afterStart && beforeEnd;
    }
}
