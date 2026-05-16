package com.invoicely.repository;

import com.invoicely.model.LineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LineItemRepository extends JpaRepository<LineItem, Long> {
    List<LineItem> findByInvoiceId(Long invoiceId);
}
