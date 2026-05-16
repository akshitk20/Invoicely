package com.invoicely.model;

import com.invoicely.model.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(length = 50)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal cgst;

    @Column(precision = 12, scale = 2)
    private BigDecimal sgst;

    @Column(precision = 12, scale = 2)
    private BigDecimal igst;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    @Column(precision = 4, scale = 2)
    private BigDecimal gstRate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private PurchaseStatus status = PurchaseStatus.UNPAID;

    @OneToMany(mappedBy = "purchaseInvoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<PurchaseLineItem> lineItems = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void addLineItem(PurchaseLineItem item) {
        lineItems.add(item);
        item.setPurchaseInvoice(this);
    }
}
