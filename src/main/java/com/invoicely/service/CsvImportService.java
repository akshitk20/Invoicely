package com.invoicely.service;

import com.invoicely.dto.ImportResultDto;
import com.invoicely.dto.InvoiceCreateDto;
import com.invoicely.dto.PurchaseInvoiceCreateDto;
import com.invoicely.model.Client;
import com.invoicely.model.Product;
import com.invoicely.model.Supplier;
import com.invoicely.model.User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsvImportService {

    private final InvoiceService invoiceService;
    private final PurchaseInvoiceService purchaseInvoiceService;
    private final ClientService clientService;
    private final SupplierService supplierService;
    private final ProductService productService;

    public CsvImportService(InvoiceService invoiceService,
                            PurchaseInvoiceService purchaseInvoiceService,
                            ClientService clientService,
                            SupplierService supplierService,
                            ProductService productService) {
        this.invoiceService = invoiceService;
        this.purchaseInvoiceService = purchaseInvoiceService;
        this.clientService = clientService;
        this.supplierService = supplierService;
        this.productService = productService;
    }

    public ImportResultDto importSalesInvoices(User user, MultipartFile file) {
        ImportResultDto result = new ImportResultDto();
        List<String[]> rows = parseCsv(file, result);
        if (rows == null) return result;

        Map<String, Client> clientMap = clientService.getClientsByUser(user).stream()
            .collect(Collectors.toMap(c -> c.getName().toLowerCase().trim(), c -> c, (a, b) -> a));

        Map<String, List<int[]>> groups = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] cols = rows.get(i);
            if (cols.length < 6) {
                result.addError(i + 2, "Not enough columns (need at least: client_name, invoice_date, description, quantity, rate, gst_rate)");
                continue;
            }
            String key = cols[0].trim().toLowerCase() + "|" + cols[1].trim();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[]{i});
        }

        for (Map.Entry<String, List<int[]>> entry : groups.entrySet()) {
            String[] keyParts = entry.getKey().split("\\|", 2);
            String clientName = keyParts[0];
            String dateStr = keyParts[1];
            List<int[]> rowIndices = entry.getValue();
            int firstRow = rowIndices.get(0)[0] + 2;

            Client client = clientMap.get(clientName);
            if (client == null) {
                result.addError(firstRow, "Client '" + rows.get(rowIndices.get(0)[0])[0].trim() + "' not found");
                continue;
            }

            LocalDate invoiceDate;
            try {
                invoiceDate = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                result.addError(firstRow, "Invalid date format '" + dateStr + "' (use yyyy-MM-dd)");
                continue;
            }

            List<InvoiceCreateDto.LineItemDto> lineItems = new ArrayList<>();
            BigDecimal gstRate = new BigDecimal("18");
            LocalDate dueDate = null;
            String notes = null;
            boolean hasError = false;

            for (int[] idx : rowIndices) {
                int rowIdx = idx[0];
                String[] cols = rows.get(rowIdx);
                int rowNum = rowIdx + 2;

                String description = cols[2].trim();
                if (description.isEmpty()) {
                    result.addError(rowNum, "Description is empty");
                    hasError = true;
                    continue;
                }

                String hsnCode = cols.length > 3 ? cols[3].trim() : "";

                BigDecimal quantity;
                try {
                    quantity = new BigDecimal(cols[4].trim());
                    if (quantity.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    result.addError(rowNum, "Invalid quantity");
                    hasError = true;
                    continue;
                }

                BigDecimal rate;
                try {
                    rate = new BigDecimal(cols[5].trim());
                    if (rate.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    result.addError(rowNum, "Invalid rate");
                    hasError = true;
                    continue;
                }

                if (cols.length > 6 && !cols[6].trim().isEmpty()) {
                    try {
                        gstRate = new BigDecimal(cols[6].trim());
                    } catch (NumberFormatException e) {
                        result.addError(rowNum, "Invalid GST rate");
                        hasError = true;
                        continue;
                    }
                }

                if (cols.length > 7 && !cols[7].trim().isEmpty()) {
                    try {
                        dueDate = LocalDate.parse(cols[7].trim());
                    } catch (DateTimeParseException e) {
                        result.addError(rowNum, "Invalid due date format (use yyyy-MM-dd)");
                        hasError = true;
                        continue;
                    }
                }

                if (cols.length > 8 && !cols[8].trim().isEmpty()) {
                    notes = cols[8].trim();
                }

                InvoiceCreateDto.LineItemDto item = new InvoiceCreateDto.LineItemDto();
                item.setDescription(description);
                item.setHsnCode(hsnCode.isEmpty() ? null : hsnCode);
                item.setQuantity(quantity);
                item.setRate(rate);
                lineItems.add(item);
            }

            if (hasError || lineItems.isEmpty()) continue;

            try {
                InvoiceCreateDto dto = new InvoiceCreateDto();
                dto.setClientId(client.getId());
                dto.setInvoiceDate(invoiceDate);
                dto.setDueDate(dueDate);
                dto.setGstRate(gstRate);
                dto.setNotes(notes);
                dto.setLineItems(lineItems);
                invoiceService.createInvoice(user, dto);
                result.incrementSuccess();
            } catch (Exception e) {
                result.addError(firstRow, "Failed to create invoice: " + e.getMessage());
            }
        }

        return result;
    }

    public ImportResultDto importPurchaseInvoices(User user, MultipartFile file) {
        ImportResultDto result = new ImportResultDto();
        List<String[]> rows = parseCsv(file, result);
        if (rows == null) return result;

        Map<String, Supplier> supplierMap = supplierService.getSuppliersByUser(user).stream()
            .collect(Collectors.toMap(s -> s.getName().toLowerCase().trim(), s -> s, (a, b) -> a));

        Map<String, Product> productMap = productService.getProductsByUser(user).stream()
            .collect(Collectors.toMap(p -> p.getName().toLowerCase().trim(), p -> p, (a, b) -> a));

        Map<String, List<int[]>> groups = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] cols = rows.get(i);
            if (cols.length < 6) {
                result.addError(i + 2, "Not enough columns (need at least: supplier_name, invoice_date, description, quantity, rate, gst_rate)");
                continue;
            }
            String key = cols[0].trim().toLowerCase() + "|" + cols[1].trim();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[]{i});
        }

        for (Map.Entry<String, List<int[]>> entry : groups.entrySet()) {
            String[] keyParts = entry.getKey().split("\\|", 2);
            String supplierName = keyParts[0];
            String dateStr = keyParts[1];
            List<int[]> rowIndices = entry.getValue();
            int firstRow = rowIndices.get(0)[0] + 2;

            Supplier supplier = supplierMap.get(supplierName);
            if (supplier == null) {
                result.addError(firstRow, "Supplier '" + rows.get(rowIndices.get(0)[0])[0].trim() + "' not found");
                continue;
            }

            LocalDate invoiceDate;
            try {
                invoiceDate = LocalDate.parse(dateStr);
            } catch (DateTimeParseException e) {
                result.addError(firstRow, "Invalid date format '" + dateStr + "' (use yyyy-MM-dd)");
                continue;
            }

            List<PurchaseInvoiceCreateDto.PurchaseLineItemDto> lineItems = new ArrayList<>();
            BigDecimal gstRate = new BigDecimal("18");
            String notes = null;
            boolean hasError = false;

            for (int[] idx : rowIndices) {
                int rowIdx = idx[0];
                String[] cols = rows.get(rowIdx);
                int rowNum = rowIdx + 2;

                String description = cols[2].trim();
                if (description.isEmpty()) {
                    result.addError(rowNum, "Description is empty");
                    hasError = true;
                    continue;
                }

                String hsnCode = cols.length > 3 ? cols[3].trim() : "";

                BigDecimal quantity;
                try {
                    quantity = new BigDecimal(cols[4].trim());
                    if (quantity.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    result.addError(rowNum, "Invalid quantity");
                    hasError = true;
                    continue;
                }

                BigDecimal rate;
                try {
                    rate = new BigDecimal(cols[5].trim());
                    if (rate.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    result.addError(rowNum, "Invalid rate");
                    hasError = true;
                    continue;
                }

                if (cols.length > 6 && !cols[6].trim().isEmpty()) {
                    try {
                        gstRate = new BigDecimal(cols[6].trim());
                    } catch (NumberFormatException e) {
                        result.addError(rowNum, "Invalid GST rate");
                        hasError = true;
                        continue;
                    }
                }

                String productName = cols.length > 7 ? cols[7].trim() : "";
                Long productId = null;
                if (!productName.isEmpty()) {
                    Product product = productMap.get(productName.toLowerCase());
                    if (product == null) {
                        result.addError(rowNum, "Product '" + productName + "' not found");
                        hasError = true;
                        continue;
                    }
                    productId = product.getId();
                }

                if (cols.length > 8 && !cols[8].trim().isEmpty()) {
                    notes = cols[8].trim();
                }

                PurchaseInvoiceCreateDto.PurchaseLineItemDto item = new PurchaseInvoiceCreateDto.PurchaseLineItemDto();
                item.setDescription(description);
                item.setHsnCode(hsnCode.isEmpty() ? null : hsnCode);
                item.setQuantity(quantity);
                item.setRate(rate);
                item.setProductId(productId);
                lineItems.add(item);
            }

            if (hasError || lineItems.isEmpty()) continue;

            try {
                PurchaseInvoiceCreateDto dto = new PurchaseInvoiceCreateDto();
                dto.setSupplierId(supplier.getId());
                dto.setInvoiceDate(invoiceDate);
                dto.setGstRate(gstRate);
                dto.setNotes(notes);
                dto.setLineItems(lineItems);
                purchaseInvoiceService.createPurchaseInvoice(user, dto);
                result.incrementSuccess();
            } catch (Exception e) {
                result.addError(firstRow, "Failed to create purchase invoice: " + e.getMessage());
            }
        }

        return result;
    }

    private List<String[]> parseCsv(MultipartFile file, ImportResultDto result) {
        if (file.isEmpty()) {
            result.addError(0, "File is empty");
            return null;
        }

        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                result.addError(0, "File has no content");
                return null;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                rows.add(parseCsvLine(line));
            }
        } catch (Exception e) {
            result.addError(0, "Failed to read file: " + e.getMessage());
            return null;
        }

        if (rows.isEmpty()) {
            result.addError(0, "No data rows found in CSV");
            return null;
        }

        return rows;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }
}
