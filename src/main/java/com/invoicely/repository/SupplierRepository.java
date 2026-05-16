package com.invoicely.repository;

import com.invoicely.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByUserIdOrderByNameAsc(Long userId);
}
