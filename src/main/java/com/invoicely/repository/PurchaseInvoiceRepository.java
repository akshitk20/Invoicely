package com.invoicely.repository;

import com.invoicely.model.PurchaseInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {

    List<PurchaseInvoice> findByUserIdOrderByInvoiceDateDesc(Long userId);

    @Query("SELECT DISTINCT p FROM PurchaseInvoice p LEFT JOIN FETCH p.lineItems WHERE p.id = :id")
    Optional<PurchaseInvoice> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(p.cgst + p.sgst + p.igst), 0) FROM PurchaseInvoice p WHERE p.user.id = :userId AND p.invoiceDate BETWEEN :start AND :end")
    BigDecimal sumGstPaidBetween(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
