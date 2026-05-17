package com.invoicely.service;

import com.invoicely.dto.InvoiceCreateDto;
import com.invoicely.model.*;
import com.invoicely.model.enums.GstType;
import com.invoicely.model.enums.InvoiceStatus;
import com.invoicely.repository.InvoiceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ClientService clientService;
    private final GstCalculationService gstCalculationService;
    private final ProductService productService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          ClientService clientService,
                          GstCalculationService gstCalculationService,
                          ProductService productService) {
        this.invoiceRepository = invoiceRepository;
        this.clientService = clientService;
        this.gstCalculationService = gstCalculationService;
        this.productService = productService;
    }

    public List<Invoice> getInvoicesByUser(User user) {
        return invoiceRepository.findByUserIdWithClient(user.getId());
    }

    public List<Invoice> getInvoicesByUserAndDateRange(User user, LocalDate start, LocalDate end) {
        return invoiceRepository.findByUserIdAndDateRangeWithDetails(user.getId(), start, end);
    }

    public Invoice getById(Long id) {
        return invoiceRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
    }

    @Transactional
    public Invoice createInvoice(User user, InvoiceCreateDto dto) {
        Client client = clientService.getById(dto.getClientId());

        GstType gstType = gstCalculationService.determineGstType(user.getState(), client.getState());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceCreateDto.LineItemDto item : dto.getLineItems()) {
            subtotal = subtotal.add(item.getQuantity().multiply(item.getRate()));
        }

        Map<String, BigDecimal> gstResult = gstCalculationService.calculateGst(
            subtotal, dto.getGstRate(), gstType);

        String invoiceNumber = generateInvoiceNumber(user);

        Invoice invoice = Invoice.builder()
            .user(user)
            .client(client)
            .invoiceNumber(invoiceNumber)
            .invoiceDate(dto.getInvoiceDate())
            .dueDate(dto.getDueDate() != null ? dto.getDueDate() : dto.getInvoiceDate().plusDays(30))
            .subtotal(subtotal)
            .cgst(gstResult.get("cgst"))
            .sgst(gstResult.get("sgst"))
            .igst(gstResult.get("igst"))
            .total(gstResult.get("total"))
            .gstRate(dto.getGstRate())
            .sacCode(dto.getSacCode())
            .notes(dto.getNotes())
            .build();

        for (InvoiceCreateDto.LineItemDto itemDto : dto.getLineItems()) {
            LineItem lineItem = LineItem.builder()
                .description(itemDto.getDescription())
                .hsnCode(itemDto.getHsnCode())
                .quantity(itemDto.getQuantity())
                .rate(itemDto.getRate())
                .amount(itemDto.getQuantity().multiply(itemDto.getRate()))
                .build();
            invoice.addLineItem(lineItem);
        }

        Invoice saved = invoiceRepository.save(invoice);

        for (InvoiceCreateDto.LineItemDto itemDto : dto.getLineItems()) {
            if (itemDto.getProductId() != null) {
                productService.deductStock(itemDto.getProductId(), itemDto.getQuantity());
            }
        }

        return saved;
    }

    @Transactional
    public Invoice updateInvoice(User user, Long id, InvoiceCreateDto dto) {
        Invoice invoice = getById(id);
        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Invoice not found: " + id);
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot edit a paid invoice");
        }

        invoice.getLineItems().clear();

        Client client = clientService.getById(dto.getClientId());
        GstType gstType = gstCalculationService.determineGstType(user.getState(), client.getState());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceCreateDto.LineItemDto item : dto.getLineItems()) {
            subtotal = subtotal.add(item.getQuantity().multiply(item.getRate()));
        }

        Map<String, BigDecimal> gstResult = gstCalculationService.calculateGst(
            subtotal, dto.getGstRate(), gstType);

        invoice.setClient(client);
        invoice.setInvoiceDate(dto.getInvoiceDate());
        invoice.setDueDate(dto.getDueDate() != null ? dto.getDueDate() : dto.getInvoiceDate().plusDays(30));
        invoice.setSubtotal(subtotal);
        invoice.setCgst(gstResult.get("cgst"));
        invoice.setSgst(gstResult.get("sgst"));
        invoice.setIgst(gstResult.get("igst"));
        invoice.setTotal(gstResult.get("total"));
        invoice.setGstRate(dto.getGstRate());
        invoice.setSacCode(dto.getSacCode());
        invoice.setNotes(dto.getNotes());

        for (InvoiceCreateDto.LineItemDto itemDto : dto.getLineItems()) {
            LineItem lineItem = LineItem.builder()
                .description(itemDto.getDescription())
                .hsnCode(itemDto.getHsnCode())
                .quantity(itemDto.getQuantity())
                .rate(itemDto.getRate())
                .amount(itemDto.getQuantity().multiply(itemDto.getRate()))
                .build();
            invoice.addLineItem(lineItem);
        }

        for (InvoiceCreateDto.LineItemDto itemDto : dto.getLineItems()) {
            if (itemDto.getProductId() != null) {
                productService.deductStock(itemDto.getProductId(), itemDto.getQuantity());
            }
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void deleteInvoice(User user, Long id) {
        Invoice invoice = getById(id);
        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Invoice not found: " + id);
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot delete a paid invoice");
        }
        invoiceRepository.delete(invoice);
    }

    public Invoice markAsPaid(Long invoiceId, LocalDate paymentDate) {
        Invoice invoice = getById(invoiceId);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
        return invoiceRepository.save(invoice);
    }

    private String generateInvoiceNumber(User user) {
        int year = Year.now().getValue();
        long count = invoiceRepository.countByUserIdAndYear(user.getId(), year);
        return String.format("INV-%d-%03d", year, count + 1);
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markOverdueInvoices() {
        List<Invoice> overdueInvoices = invoiceRepository
            .findByStatusAndDueDateBefore(InvoiceStatus.UNPAID, LocalDate.now());
        for (Invoice invoice : overdueInvoices) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
        }
        invoiceRepository.saveAll(overdueInvoices);
    }
}
