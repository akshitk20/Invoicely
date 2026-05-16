package com.invoicely.service;

import com.invoicely.model.Invoice;
import com.invoicely.model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final InvoiceService invoiceService;

    public ReportService(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public byte[] generateGstSummaryExcel(User user, LocalDate start, LocalDate end) throws IOException {
        List<Invoice> invoices = invoiceService.getInvoicesByUserAndDateRange(user, start, end);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("GST Summary");

            CellStyle headerStyle = createHeaderStyle(workbook);

            Row header = sheet.createRow(0);
            String[] columns = {"Invoice No", "Date", "Client", "Subtotal (₹)", "CGST (₹)", "SGST (₹)", "IGST (₹)", "Total (₹)"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalCgst = BigDecimal.ZERO;
            BigDecimal totalSgst = BigDecimal.ZERO;
            BigDecimal totalIgst = BigDecimal.ZERO;
            BigDecimal grandTotal = BigDecimal.ZERO;

            int rowNum = 1;
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(inv.getInvoiceNumber());
                row.createCell(1).setCellValue(inv.getInvoiceDate().toString());
                row.createCell(2).setCellValue(inv.getClient().getName());
                row.createCell(3).setCellValue(inv.getSubtotal().doubleValue());
                row.createCell(4).setCellValue(inv.getCgst().doubleValue());
                row.createCell(5).setCellValue(inv.getSgst().doubleValue());
                row.createCell(6).setCellValue(inv.getIgst().doubleValue());
                row.createCell(7).setCellValue(inv.getTotal().doubleValue());

                totalSubtotal = totalSubtotal.add(inv.getSubtotal());
                totalCgst = totalCgst.add(inv.getCgst());
                totalSgst = totalSgst.add(inv.getSgst());
                totalIgst = totalIgst.add(inv.getIgst());
                grandTotal = grandTotal.add(inv.getTotal());
            }

            Row totalsRow = sheet.createRow(rowNum);
            totalsRow.createCell(0).setCellValue("TOTAL");
            totalsRow.createCell(3).setCellValue(totalSubtotal.doubleValue());
            totalsRow.createCell(4).setCellValue(totalCgst.doubleValue());
            totalsRow.createCell(5).setCellValue(totalSgst.doubleValue());
            totalsRow.createCell(6).setCellValue(totalIgst.doubleValue());
            totalsRow.createCell(7).setCellValue(grandTotal.doubleValue());

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateIncomeReportExcel(User user, LocalDate start, LocalDate end) throws IOException {
        List<Invoice> invoices = invoiceService.getInvoicesByUserAndDateRange(user, start, end);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Income Report");

            CellStyle headerStyle = createHeaderStyle(workbook);

            Row header = sheet.createRow(0);
            String[] columns = {"Client", "Invoices Count", "Total Amount (₹)", "Paid (₹)", "Pending (₹)"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            Map<String, List<Invoice>> byClient = invoices.stream()
                .collect(Collectors.groupingBy(inv -> inv.getClient().getName()));

            int rowNum = 1;
            for (Map.Entry<String, List<Invoice>> entry : byClient.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                List<Invoice> clientInvoices = entry.getValue();

                BigDecimal total = clientInvoices.stream()
                    .map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal paid = clientInvoices.stream()
                    .filter(i -> i.getPaymentDate() != null)
                    .map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(clientInvoices.size());
                row.createCell(2).setCellValue(total.doubleValue());
                row.createCell(3).setCellValue(paid.doubleValue());
                row.createCell(4).setCellValue(total.subtract(paid).doubleValue());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
