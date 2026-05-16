package com.invoicely.repository;

import com.invoicely.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByUserIdOrderByNameAsc(Long userId);
}
