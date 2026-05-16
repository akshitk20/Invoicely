package com.invoicely.dto;

import com.invoicely.model.enums.ExpenseCategory;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCreateDto {

    @NotNull
    private LocalDate expenseDate;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotNull
    private ExpenseCategory category;

    private String description;
}
