package com.invoicely.service;

import com.invoicely.dto.PurchaseInvoiceCreateDto;
import com.invoicely.model.*;
import com.invoicely.model.enums.GstType;
import com.invoicely.model.enums.PurchaseStatus;
import com.invoicely.repository.PurchaseInvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseInvoiceService {

    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final SupplierService supplierService;
    private final GstCalculationService gstCalculationService;
    private final ProductService productService;

    public PurchaseInvoiceService(PurchaseInvoiceRepository purchaseInvoiceRepository,
                                  SupplierService supplierService,
                                  GstCalculationService gstCalculationService,
                                  ProductService productService) {
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.supplierService = supplierService;
        this.gstCalculationService = gstCalculationService;
        this.productService = productService;
    }

    public List<PurchaseInvoice> getByUser(User user) {
        return purchaseInvoiceRepository.findByUserIdOrderByInvoiceDateDesc(user.getId());
    }

    public PurchaseInvoice getById(Long id) {
        return purchaseInvoiceRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new RuntimeException("Purchase invoice not found: " + id));
    }

    @Transactional
    public PurchaseInvoice createPurchaseInvoice(User user, PurchaseInvoiceCreateDto dto) {
        Supplier supplier = supplierService.getById(dto.getSupplierId());

        GstType gstType = gstCalculationService.determineGstType(supplier.getState(), user.getState());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (PurchaseInvoiceCreateDto.PurchaseLineItemDto item : dto.getLineItems()) {
            subtotal = subtotal.add(item.getQuantity().multiply(item.getRate()));
        }

        Map<String, BigDecimal> gstResult = gstCalculationService.calculateGst(
            subtotal, dto.getGstRate(), gstType);

        PurchaseInvoice purchase = PurchaseInvoice.builder()
            .user(user)
            .supplier(supplier)
            .invoiceNumber(dto.getInvoiceNumber())
            .invoiceDate(dto.getInvoiceDate())
            .subtotal(subtotal)
            .cgst(gstResult.get("cgst"))
            .sgst(gstResult.get("sgst"))
            .igst(gstResult.get("igst"))
            .total(gstResult.get("total"))
            .gstRate(dto.getGstRate())
            .notes(dto.getNotes())
            .build();

        for (PurchaseInvoiceCreateDto.PurchaseLineItemDto itemDto : dto.getLineItems()) {
            PurchaseLineItem lineItem = PurchaseLineItem.builder()
                .description(itemDto.getDescription())
                .hsnCode(itemDto.getHsnCode())
                .quantity(itemDto.getQuantity())
                .rate(itemDto.getRate())
                .amount(itemDto.getQuantity().multiply(itemDto.getRate()))
                .productId(itemDto.getProductId())
                .build();
            purchase.addLineItem(lineItem);
        }

        PurchaseInvoice saved = purchaseInvoiceRepository.save(purchase);

        for (PurchaseInvoiceCreateDto.PurchaseLineItemDto itemDto : dto.getLineItems()) {
            if (itemDto.getProductId() != null) {
                productService.increaseStock(itemDto.getProductId(), itemDto.getQuantity());
            }
        }

        return saved;
    }

    public PurchaseInvoice markAsPaid(Long id) {
        PurchaseInvoice purchase = getById(id);
        purchase.setStatus(PurchaseStatus.PAID);
        return purchaseInvoiceRepository.save(purchase);
    }

    public BigDecimal getInputGstCredit(User user, LocalDate start, LocalDate end) {
        return purchaseInvoiceRepository.sumGstPaidBetween(user.getId(), start, end);
    }
}
