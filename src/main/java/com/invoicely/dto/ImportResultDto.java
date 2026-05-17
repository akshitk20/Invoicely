package com.invoicely.dto;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDto {

    private int successCount;
    private int failedCount;
    private List<ImportError> errors = new ArrayList<>();

    public void addError(int row, String message) {
        errors.add(new ImportError(row, message));
        failedCount++;
    }

    public void incrementSuccess() {
        successCount++;
    }

    @Getter
    @AllArgsConstructor
    public static class ImportError {
        private int row;
        private String message;
    }
}
