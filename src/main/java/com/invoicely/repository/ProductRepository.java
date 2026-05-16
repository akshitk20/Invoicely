package com.invoicely.repository;

import com.invoicely.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByUserIdOrderByNameAsc(Long userId);

    @Query("SELECT p FROM Product p WHERE p.user.id = :userId AND p.currentStock <= p.lowStockThreshold")
    List<Product> findLowStockByUserId(@Param("userId") Long userId);
}
