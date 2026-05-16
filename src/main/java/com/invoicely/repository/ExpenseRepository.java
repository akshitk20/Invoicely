package com.invoicely.repository;

import com.invoicely.model.Expense;
import com.invoicely.model.enums.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);

    List<Expense> findByUserIdAndExpenseDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Expense> findByUserIdAndCategory(Long userId, ExpenseCategory category);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumAmountBetween(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
