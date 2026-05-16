package com.invoicely.service;

import com.invoicely.model.enums.GstType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GstCalculationServiceTest {

    private GstCalculationService gstCalculationService;

    @BeforeEach
    void setUp() {
        gstCalculationService = new GstCalculationService();
    }

    @Test
    void sameState_shouldReturn_CGST_SGST() {
        GstType result = gstCalculationService.determineGstType("Maharashtra", "Maharashtra");
        assertEquals(GstType.CGST_SGST, result);
    }

    @Test
    void differentState_shouldReturn_IGST() {
        GstType result = gstCalculationService.determineGstType("Maharashtra", "Karnataka");
        assertEquals(GstType.IGST, result);
    }

    @Test
    void nullBuyerState_shouldReturn_EXPORT() {
        GstType result = gstCalculationService.determineGstType("Maharashtra", null);
        assertEquals(GstType.EXPORT, result);
    }

    @Test
    void calculateGst_CGST_SGST_at18Percent() {
        BigDecimal subtotal = new BigDecimal("10000");
        BigDecimal rate = new BigDecimal("18");

        Map<String, BigDecimal> result = gstCalculationService.calculateGst(subtotal, rate, GstType.CGST_SGST);

        assertEquals(new BigDecimal("900.00"), result.get("cgst"));
        assertEquals(new BigDecimal("900.00"), result.get("sgst"));
        assertEquals(BigDecimal.ZERO, result.get("igst"));
        assertEquals(new BigDecimal("11800.00"), result.get("total"));
    }

    @Test
    void calculateGst_IGST_at18Percent() {
        BigDecimal subtotal = new BigDecimal("55000");
        BigDecimal rate = new BigDecimal("18");

        Map<String, BigDecimal> result = gstCalculationService.calculateGst(subtotal, rate, GstType.IGST);

        assertEquals(BigDecimal.ZERO, result.get("cgst"));
        assertEquals(BigDecimal.ZERO, result.get("sgst"));
        assertEquals(new BigDecimal("9900.00"), result.get("igst"));
        assertEquals(new BigDecimal("64900.00"), result.get("total"));
    }

    @Test
    void calculateGst_EXPORT_shouldBeZero() {
        BigDecimal subtotal = new BigDecimal("50000");
        BigDecimal rate = new BigDecimal("18");

        Map<String, BigDecimal> result = gstCalculationService.calculateGst(subtotal, rate, GstType.EXPORT);

        assertEquals(BigDecimal.ZERO, result.get("cgst"));
        assertEquals(BigDecimal.ZERO, result.get("sgst"));
        assertEquals(BigDecimal.ZERO, result.get("igst"));
        assertEquals(new BigDecimal("50000"), result.get("total"));
    }

    @Test
    void calculateGst_at5Percent() {
        BigDecimal subtotal = new BigDecimal("20000");
        BigDecimal rate = new BigDecimal("5");

        Map<String, BigDecimal> result = gstCalculationService.calculateGst(subtotal, rate, GstType.CGST_SGST);

        assertEquals(new BigDecimal("500.00"), result.get("cgst"));
        assertEquals(new BigDecimal("500.00"), result.get("sgst"));
        assertEquals(new BigDecimal("21000.00"), result.get("total"));
    }
}
