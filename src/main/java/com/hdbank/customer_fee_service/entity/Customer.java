package com.hdbank.customer_fee_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "customer")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "status", nullable = false, length = 50)
    private String status;  // ACTIVE, INACTIVE

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Builder.Default
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Helper method: Check if customer is deleted (soft delete)
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Helper method: Check if customer is active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status) && !isDeleted();
    }
}
