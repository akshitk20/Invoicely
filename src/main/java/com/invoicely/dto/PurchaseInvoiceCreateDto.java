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
public class PurchaseInvoiceCreateDto {

    @NotNull
    private Long supplierId;

    private String invoiceNumber;

    @NotNull
    private LocalDate invoiceDate;

    @NotNull
    private BigDecimal gstRate;

    private String notes;

    @NotEmpty
    private List<PurchaseLineItemDto> lineItems;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseLineItemDto {
        @NotBlank
        private String description;

        private String hsnCode;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal quantity;

        @NotNull
        @DecimalMin("0.01")
        private BigDecimal rate;

        private Long productId;
    }
}
