package com.invoicely.controller;

import com.invoicely.dto.InvoiceCreateDto;
import com.invoicely.exception.InvoiceLimitExceededException;
import com.invoicely.model.Invoice;
import com.invoicely.model.User;
import com.invoicely.service.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final UserService userService;
    private final PdfGenerationService pdfGenerationService;
    private final SubscriptionService subscriptionService;
    private final ProductService productService;

    public InvoiceController(InvoiceService invoiceService,
                             ClientService clientService,
                             UserService userService,
                             PdfGenerationService pdfGenerationService,
                             SubscriptionService subscriptionService,
                             ProductService productService) {
        this.invoiceService = invoiceService;
        this.clientService = clientService;
        this.userService = userService;
        this.pdfGenerationService = pdfGenerationService;
        this.subscriptionService = subscriptionService;
        this.productService = productService;
    }

    @GetMapping
    public String listInvoices(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("invoices", invoiceService.getInvoicesByUser(user));
        return "invoices/list";
    }

    @GetMapping("/new")
    public String showCreateForm(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("clients", clientService.getClientsByUser(user));
        model.addAttribute("products", productService.getProductsByUser(user));
        model.addAttribute("invoiceDto", new InvoiceCreateDto());
        model.addAttribute("canCreateInvoice", subscriptionService.canCreateInvoice(user));
        model.addAttribute("monthlyInvoiceCount", subscriptionService.getMonthlyInvoiceCount(user));
        return "invoices/create";
    }

    @PostMapping
    public String createInvoice(@AuthenticationPrincipal OAuth2User oAuth2User,
                                @ModelAttribute InvoiceCreateDto dto,
                                RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(oAuth2User);
        try {
            if (!subscriptionService.canCreateInvoice(user)) {
                throw new InvoiceLimitExceededException(
                        "Free plan limit reached (3 invoices/month). Upgrade to Pro for unlimited invoices.");
            }
            Invoice invoice = invoiceService.createInvoice(user, dto);
            return "redirect:/invoices/" + invoice.getId();
        } catch (InvoiceLimitExceededException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/subscription/pricing";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices/new";
        }
    }

    @GetMapping("/{id}")
    public String viewInvoice(@PathVariable Long id, Model model,
                              @AuthenticationPrincipal OAuth2User oAuth2User) {
        User user = userService.getCurrentUser(oAuth2User);
        Invoice invoice = invoiceService.getById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("user", user);
        return "invoices/preview";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id,
                                               @AuthenticationPrincipal OAuth2User oAuth2User) {
        User user = userService.getCurrentUser(oAuth2User);
        Invoice invoice = invoiceService.getById(id);
        byte[] pdf = pdfGenerationService.generateInvoicePdf(invoice, user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename(invoice.getInvoiceNumber() + ".pdf")
            .build());

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/mark-paid")
    public String markAsPaid(@PathVariable Long id,
                             @RequestParam(required = false) LocalDate paymentDate) {
        invoiceService.markAsPaid(id, paymentDate);
        return "redirect:/invoices";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal OAuth2User oAuth2User,
                               Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        Invoice invoice = invoiceService.getById(id);
        if (!invoice.getUser().getId().equals(user.getId())) {
            return "redirect:/invoices";
        }
        if (invoice.getStatus() == com.invoicely.model.enums.InvoiceStatus.PAID) {
            return "redirect:/invoices/" + id;
        }
        model.addAttribute("invoice", invoice);
        model.addAttribute("clients", clientService.getClientsByUser(user));
        model.addAttribute("products", productService.getProductsByUser(user));
        return "invoices/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateInvoice(@PathVariable Long id,
                                @AuthenticationPrincipal OAuth2User oAuth2User,
                                @ModelAttribute InvoiceCreateDto dto,
                                RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(oAuth2User);
        try {
            invoiceService.updateInvoice(user, id, dto);
            redirectAttributes.addFlashAttribute("success", "Invoice updated successfully");
            return "redirect:/invoices/" + id;
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteInvoice(@PathVariable Long id,
                                @AuthenticationPrincipal OAuth2User oAuth2User,
                                RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(oAuth2User);
        try {
            invoiceService.deleteInvoice(user, id);
            redirectAttributes.addFlashAttribute("success", "Invoice deleted successfully");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/invoices";
    }
}
