package com.invoicely.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal rate;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;
}
