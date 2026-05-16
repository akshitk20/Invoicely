package com.invoicely.service;

import com.invoicely.model.Invoice;
import com.invoicely.model.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {

    private final ReportService reportService;
    private final PdfGenerationService pdfGenerationService;
    private final InvoiceService invoiceService;

    public ExportService(ReportService reportService,
                         PdfGenerationService pdfGenerationService,
                         InvoiceService invoiceService) {
        this.reportService = reportService;
        this.pdfGenerationService = pdfGenerationService;
        this.invoiceService = invoiceService;
    }

    public byte[] generateCaExportPack(User user, LocalDate start, LocalDate end) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // GST Summary
            byte[] gstSummary = reportService.generateGstSummaryExcel(user, start, end);
            addToZip(zos, "gst_summary.xlsx", gstSummary);

            // Income Report
            byte[] incomeReport = reportService.generateIncomeReportExcel(user, start, end);
            addToZip(zos, "income_summary.xlsx", incomeReport);

            // All invoices as individual PDFs
            List<Invoice> invoices = invoiceService.getInvoicesByUserAndDateRange(user, start, end);
            for (Invoice invoice : invoices) {
                byte[] pdf = pdfGenerationService.generateInvoicePdf(invoice, user);
                addToZip(zos, "invoices/" + invoice.getInvoiceNumber() + ".pdf", pdf);
            }
        }

        return baos.toByteArray();
    }

    private void addToZip(ZipOutputStream zos, String filename, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }
}
