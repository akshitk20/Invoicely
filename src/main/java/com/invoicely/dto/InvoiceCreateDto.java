package com.invoicely.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreateDto {

    @NotNull
    private Long clientId;

    @NotNull
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @NotNull
    private BigDecimal gstRate;

    private String sacCode;

    private String notes;

    @NotEmpty
    private List<LineItemDto> lineItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItemDto {
        @NotBlank
        private String description;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal quantity;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal rate;
    }
}
