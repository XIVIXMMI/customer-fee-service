package com.hdbank.customer_fee_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fee_type")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;  // FIXED_MONTHLY, TIERED_BALANCE, PERCENTAGE_OF_BALANCE

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "calculation_type", nullable = false, length = 50)
    private String calculationType;  // FIXED, TIERED, PERCENTAGE

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
