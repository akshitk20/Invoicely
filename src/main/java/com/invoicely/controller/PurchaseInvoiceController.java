package com.invoicely.controller;

import com.invoicely.dto.PurchaseInvoiceCreateDto;
import com.invoicely.model.PurchaseInvoice;
import com.invoicely.model.User;
import com.invoicely.service.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/purchases")
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService purchaseInvoiceService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final UserService userService;
    private final PdfGenerationService pdfGenerationService;

    public PurchaseInvoiceController(PurchaseInvoiceService purchaseInvoiceService,
                                     SupplierService supplierService,
                                     ProductService productService,
                                     UserService userService,
                                     PdfGenerationService pdfGenerationService) {
        this.purchaseInvoiceService = purchaseInvoiceService;
        this.supplierService = supplierService;
        this.productService = productService;
        this.userService = userService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @GetMapping
    public String listPurchases(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("purchases", purchaseInvoiceService.getByUser(user));
        return "purchases/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("suppliers", supplierService.getSuppliersByUser(user));
        model.addAttribute("products", productService.getProductsByUser(user));
        return "purchases/create";
    }

    @PostMapping
    public String createPurchase(@AuthenticationPrincipal OAuth2User oAuth2User,
                                 @ModelAttribute PurchaseInvoiceCreateDto dto,
                                 RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(oAuth2User);
        try {
            PurchaseInvoice purchase = purchaseInvoiceService.createPurchaseInvoice(user, dto);
            return "redirect:/purchases/" + purchase.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/purchases/new";
        }
    }

    @GetMapping("/{id}")
    public String viewPurchase(@PathVariable Long id, Model model) {
        PurchaseInvoice purchase = purchaseInvoiceService.getById(id);
        model.addAttribute("purchase", purchase);
        return "purchases/view";
    }

    @PostMapping("/{id}/mark-paid")
    public String markAsPaid(@PathVariable Long id) {
        purchaseInvoiceService.markAsPaid(id);
        return "redirect:/purchases";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id,
                                              @AuthenticationPrincipal OAuth2User oAuth2User) {
        User user = userService.getCurrentUser(oAuth2User);
        PurchaseInvoice purchase = purchaseInvoiceService.getById(id);
        byte[] pdf = pdfGenerationService.generatePurchaseInvoicePdf(purchase, user);

        String filename = (purchase.getInvoiceNumber() != null ? purchase.getInvoiceNumber() : "Purchase_" + id) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
