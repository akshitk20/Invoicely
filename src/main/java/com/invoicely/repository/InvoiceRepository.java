package com.invoicely.repository;

import com.invoicely.model.Invoice;
import com.invoicely.model.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.client LEFT JOIN FETCH i.lineItems WHERE i.id = :id")
    java.util.Optional<Invoice> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.client WHERE i.user.id = :userId ORDER BY i.invoiceDate DESC")
    List<Invoice> findByUserIdWithClient(@Param("userId") Long userId);

    List<Invoice> findByUserIdOrderByInvoiceDateDesc(Long userId);

    List<Invoice> findByUserIdAndStatus(Long userId, InvoiceStatus status);

    List<Invoice> findByUserIdAndInvoiceDateBetween(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.client LEFT JOIN FETCH i.lineItems WHERE i.user.id = :userId AND i.invoiceDate BETWEEN :start AND :end ORDER BY i.invoiceDate DESC")
    List<Invoice> findByUserIdAndDateRangeWithDetails(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.user.id = :userId AND YEAR(i.invoiceDate) = :year")
    long countByUserIdAndYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT COALESCE(SUM(i.total), 0) FROM Invoice i WHERE i.user.id = :userId AND i.status = 'PAID' AND i.invoiceDate BETWEEN :start AND :end")
    BigDecimal sumPaidAmountBetween(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);

    boolean existsByClientId(Long clientId);
}
