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

    @Transactional
    public PurchaseInvoice updatePurchaseInvoice(User user, Long id, PurchaseInvoiceCreateDto dto) {
        PurchaseInvoice purchase = getById(id);
        if (!purchase.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Purchase invoice not found: " + id);
        }
        if (purchase.getStatus() == PurchaseStatus.PAID) {
            throw new IllegalStateException("Cannot edit a paid purchase invoice");
        }

        for (PurchaseLineItem oldItem : purchase.getLineItems()) {
            if (oldItem.getProductId() != null) {
                productService.deductStock(oldItem.getProductId(), oldItem.getQuantity());
            }
        }

        purchase.getLineItems().clear();

        Supplier supplier = supplierService.getById(dto.getSupplierId());
        GstType gstType = gstCalculationService.determineGstType(supplier.getState(), user.getState());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (PurchaseInvoiceCreateDto.PurchaseLineItemDto item : dto.getLineItems()) {
            subtotal = subtotal.add(item.getQuantity().multiply(item.getRate()));
        }

        Map<String, BigDecimal> gstResult = gstCalculationService.calculateGst(
            subtotal, dto.getGstRate(), gstType);

        purchase.setSupplier(supplier);
        purchase.setInvoiceNumber(dto.getInvoiceNumber());
        purchase.setInvoiceDate(dto.getInvoiceDate());
        purchase.setSubtotal(subtotal);
        purchase.setCgst(gstResult.get("cgst"));
        purchase.setSgst(gstResult.get("sgst"));
        purchase.setIgst(gstResult.get("igst"));
        purchase.setTotal(gstResult.get("total"));
        purchase.setGstRate(dto.getGstRate());
        purchase.setNotes(dto.getNotes());

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

        for (PurchaseInvoiceCreateDto.PurchaseLineItemDto itemDto : dto.getLineItems()) {
            if (itemDto.getProductId() != null) {
                productService.increaseStock(itemDto.getProductId(), itemDto.getQuantity());
            }
        }

        return purchaseInvoiceRepository.save(purchase);
    }

    @Transactional
    public void deletePurchaseInvoice(User user, Long id) {
        PurchaseInvoice purchase = getById(id);
        if (!purchase.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Purchase invoice not found: " + id);
        }
        if (purchase.getStatus() == PurchaseStatus.PAID) {
            throw new IllegalStateException("Cannot delete a paid purchase invoice");
        }

        for (PurchaseLineItem item : purchase.getLineItems()) {
            if (item.getProductId() != null) {
                productService.deductStock(item.getProductId(), item.getQuantity());
            }
        }

        purchaseInvoiceRepository.delete(purchase);
    }

    public BigDecimal getInputGstCredit(User user, LocalDate start, LocalDate end) {
        return purchaseInvoiceRepository.sumGstPaidBetween(user.getId(), start, end);
    }
}
