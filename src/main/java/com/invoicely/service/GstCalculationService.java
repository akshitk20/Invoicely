package com.invoicely.service;

import com.invoicely.model.enums.GstType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class GstCalculationService {

    public GstType determineGstType(String sellerState, String buyerState) {
        if (sellerState == null || buyerState == null) {
            return GstType.EXPORT;
        }
        if (sellerState.equalsIgnoreCase(buyerState)) {
            return GstType.CGST_SGST;
        }
        return GstType.IGST;
    }

    public Map<String, BigDecimal> calculateGst(BigDecimal subtotal, BigDecimal gstRate, GstType gstType) {
        BigDecimal totalGst = subtotal.multiply(gstRate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;

        switch (gstType) {
            case CGST_SGST -> {
                cgst = totalGst.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
                sgst = totalGst.subtract(cgst);
            }
            case IGST -> igst = totalGst;
            case EXPORT -> {
                // No GST for exports
            }
        }

        BigDecimal total = subtotal.add(cgst).add(sgst).add(igst);

        return Map.of(
            "cgst", cgst,
            "sgst", sgst,
            "igst", igst,
            "totalGst", totalGst,
            "total", total
        );
    }
}
