package com.invoicely.controller;

import com.invoicely.dto.ImportResultDto;
import com.invoicely.model.User;
import com.invoicely.service.CsvImportService;
import com.invoicely.service.SubscriptionService;
import com.invoicely.service.UserService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/import")
public class ImportController {

    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final CsvImportService csvImportService;

    public ImportController(UserService userService,
                            SubscriptionService subscriptionService,
                            CsvImportService csvImportService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.csvImportService = csvImportService;
    }

    @GetMapping
    public String showImportPage(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        if (!subscriptionService.canAccessReports(user)) {
            return "redirect:/subscription/pricing";
        }
        return "import/index";
    }

    @PostMapping("/sales")
    public String importSales(@AuthenticationPrincipal OAuth2User oAuth2User,
                              @RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(oAuth2User);
        if (!subscriptionService.canAccessReports(user)) {
            return "redirect:/subscription/pricing";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file");
            return "redirect:/import";
        }

        ImportResultDto result = csvImportService.importSalesInvoices(user, file);
        redirectAttributes.addFlashAttribute("result", result);
        redirectAttributes.addFlashAttribute("importType", "Sales");
        return "redirect:/import";
    }

    @PostMapping("/purchases")
    public String importPurchases(@AuthenticationPrincipal OAuth2User oAuth2User,
                                  @RequestParam("file") MultipartFile file,
                                  RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(oAuth2User);
        if (!subscriptionService.canAccessReports(user)) {
            return "redirect:/subscription/pricing";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file");
            return "redirect:/import";
        }

        ImportResultDto result = csvImportService.importPurchaseInvoices(user, file);
        redirectAttributes.addFlashAttribute("result", result);
        redirectAttributes.addFlashAttribute("importType", "Purchases");
        return "redirect:/import";
    }

    @GetMapping("/sample/sales")
    public ResponseEntity<byte[]> downloadSalesSample() {
        String csv = "client_name,invoice_date,description,hsn_code,quantity,rate,gst_rate,due_date,notes\n"
            + "Acme Corp,2026-01-15,Web Development,,1,50000,18,2026-02-15,January project\n"
            + "Acme Corp,2026-01-15,Hosting (Annual),,1,12000,18,2026-02-15,\n"
            + "Beta Ltd,2026-01-20,Steel Rods 10mm,7213,100,450,18,,Bulk order\n";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sales_import_sample.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }

    @GetMapping("/sample/purchases")
    public ResponseEntity<byte[]> downloadPurchasesSample() {
        String csv = "supplier_name,invoice_date,description,hsn_code,quantity,rate,gst_rate,product_name,notes\n"
            + "Steel Suppliers,2026-01-10,Steel Rod 2mm,7213,500,120,18,Steel Rod 2mm,Monthly stock\n"
            + "Steel Suppliers,2026-01-10,Steel Rod 5mm,7213,200,180,18,,\n"
            + "Office Mart,2026-01-12,Printer Paper A4,,10,350,12,,Office supplies\n";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchases_import_sample.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }
}
