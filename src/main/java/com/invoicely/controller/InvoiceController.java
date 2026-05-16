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

    public InvoiceController(InvoiceService invoiceService,
                             ClientService clientService,
                             UserService userService,
                             PdfGenerationService pdfGenerationService,
                             SubscriptionService subscriptionService) {
        this.invoiceService = invoiceService;
        this.clientService = clientService;
        this.userService = userService;
        this.pdfGenerationService = pdfGenerationService;
        this.subscriptionService = subscriptionService;
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
}
