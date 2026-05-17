package com.invoicely.service;

import com.invoicely.model.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Service
public class PdfGenerationService {

    public byte[] generateInvoicePdf(Invoice invoice, User user) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL);

            // Title
            Paragraph title = new Paragraph("TAX INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Invoice details
            document.add(new Paragraph("Invoice No: " + invoice.getInvoiceNumber(), headerFont));
            document.add(new Paragraph("Date: " + invoice.getInvoiceDate(), normalFont));
            document.add(new Paragraph("Due Date: " + invoice.getDueDate(), normalFont));
            document.add(new Paragraph("\n"));

            // Seller details
            document.add(new Paragraph("From:", headerFont));
            document.add(new Paragraph(user.getBusinessName() != null ? user.getBusinessName() : user.getName(), normalFont));
            document.add(new Paragraph(user.getAddress() != null ? user.getAddress() : "", normalFont));
            document.add(new Paragraph("State: " + user.getState(), normalFont));
            document.add(new Paragraph("GSTIN: " + (user.getGstin() != null ? user.getGstin() : "N/A"), normalFont));
            document.add(new Paragraph("\n"));

            // Buyer details
            document.add(new Paragraph("To:", headerFont));
            document.add(new Paragraph(invoice.getClient().getBusinessName() != null ?
                invoice.getClient().getBusinessName() : invoice.getClient().getName(), normalFont));
            document.add(new Paragraph(invoice.getClient().getAddress() != null ?
                invoice.getClient().getAddress() : "", normalFont));
            document.add(new Paragraph("State: " + invoice.getClient().getState(), normalFont));
            document.add(new Paragraph("GSTIN: " + (invoice.getClient().getGstin() != null ?
                invoice.getClient().getGstin() : "N/A"), normalFont));
            document.add(new Paragraph("\n"));

            // Line items table
            PdfPTable table = new PdfPTable(new float[]{3.5f, 1.5f, 1, 2, 2});
            table.setWidthPercentage(100);

            addTableHeader(table, headerFont, "Description", "HSN", "Qty", "Rate (₹)", "Amount (₹)");

            for (LineItem item : invoice.getLineItems()) {
                table.addCell(new PdfPCell(new Phrase(item.getDescription(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getHsnCode() != null ? item.getHsnCode() : "", normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getQuantity().stripTrailingZeros().toPlainString(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getRate().toPlainString(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getAmount().toPlainString(), normalFont)));
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            // Totals
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addTotalRow(totalsTable, normalFont, "Subtotal:", "₹" + invoice.getSubtotal().toPlainString());

            if (invoice.getCgst().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, normalFont, "CGST (" + invoice.getGstRate().divide(new BigDecimal("2")).toPlainString() + "%):", "₹" + invoice.getCgst().toPlainString());
                addTotalRow(totalsTable, normalFont, "SGST (" + invoice.getGstRate().divide(new BigDecimal("2")).toPlainString() + "%):", "₹" + invoice.getSgst().toPlainString());
            }
            if (invoice.getIgst().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, normalFont, "IGST (" + invoice.getGstRate().toPlainString() + "%):", "₹" + invoice.getIgst().toPlainString());
            }

            addTotalRow(totalsTable, headerFont, "Total:", "₹" + invoice.getTotal().toPlainString());

            document.add(totalsTable);
            document.add(new Paragraph("\n"));

            // SAC Code
            if (invoice.getSacCode() != null) {
                document.add(new Paragraph("SAC Code: " + invoice.getSacCode(), smallFont));
            }

            // Notes
            if (invoice.getNotes() != null && !invoice.getNotes().isEmpty()) {
                document.add(new Paragraph("\nNotes: " + invoice.getNotes(), smallFont));
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        return baos.toByteArray();
    }

    private void addTableHeader(PdfPTable table, Font font, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTotalRow(PdfPTable table, Font font, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    public byte[] generatePurchaseInvoicePdf(PurchaseInvoice purchase, User user) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL);

            Paragraph title = new Paragraph("PURCHASE INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Invoice No: " + (purchase.getInvoiceNumber() != null ? purchase.getInvoiceNumber() : "N/A"), headerFont));
            document.add(new Paragraph("Date: " + purchase.getInvoiceDate(), normalFont));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Buyer (You):", headerFont));
            document.add(new Paragraph(user.getBusinessName() != null ? user.getBusinessName() : user.getName(), normalFont));
            document.add(new Paragraph(user.getAddress() != null ? user.getAddress() : "", normalFont));
            document.add(new Paragraph("State: " + user.getState(), normalFont));
            document.add(new Paragraph("GSTIN: " + (user.getGstin() != null ? user.getGstin() : "N/A"), normalFont));
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Supplier:", headerFont));
            document.add(new Paragraph(purchase.getSupplier().getBusinessName() != null ?
                purchase.getSupplier().getBusinessName() : purchase.getSupplier().getName(), normalFont));
            document.add(new Paragraph(purchase.getSupplier().getAddress() != null ?
                purchase.getSupplier().getAddress() : "", normalFont));
            document.add(new Paragraph("State: " + purchase.getSupplier().getState(), normalFont));
            document.add(new Paragraph("GSTIN: " + (purchase.getSupplier().getGstin() != null ?
                purchase.getSupplier().getGstin() : "N/A"), normalFont));
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(new float[]{3.5f, 1.5f, 1, 2, 2});
            table.setWidthPercentage(100);

            addTableHeader(table, headerFont, "Description", "HSN", "Qty", "Rate (₹)", "Amount (₹)");

            for (PurchaseLineItem item : purchase.getLineItems()) {
                table.addCell(new PdfPCell(new Phrase(item.getDescription(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getHsnCode() != null ? item.getHsnCode() : "", normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getQuantity().stripTrailingZeros().toPlainString(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getRate().toPlainString(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(item.getAmount().toPlainString(), normalFont)));
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            addTotalRow(totalsTable, normalFont, "Subtotal:", "₹" + purchase.getSubtotal().toPlainString());

            if (purchase.getCgst().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, normalFont, "CGST (" + purchase.getGstRate().divide(new BigDecimal("2")).toPlainString() + "%):", "₹" + purchase.getCgst().toPlainString());
                addTotalRow(totalsTable, normalFont, "SGST (" + purchase.getGstRate().divide(new BigDecimal("2")).toPlainString() + "%):", "₹" + purchase.getSgst().toPlainString());
            }
            if (purchase.getIgst().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, normalFont, "IGST (" + purchase.getGstRate().toPlainString() + "%):", "₹" + purchase.getIgst().toPlainString());
            }

            addTotalRow(totalsTable, headerFont, "Total:", "₹" + purchase.getTotal().toPlainString());

            document.add(totalsTable);

            if (purchase.getNotes() != null && !purchase.getNotes().isEmpty()) {
                document.add(new Paragraph("\nNotes: " + purchase.getNotes(), smallFont));
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Error generating purchase invoice PDF", e);
        }

        return baos.toByteArray();
    }
}
