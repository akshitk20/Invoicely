package com.invoicely.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String reportType; // GST, INCOME, EXPENSE, CA_PACK
}
