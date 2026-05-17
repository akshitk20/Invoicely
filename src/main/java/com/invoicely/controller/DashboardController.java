package com.invoicely.controller;

import com.invoicely.model.Invoice;
import com.invoicely.model.LineItem;
import com.invoicely.model.PurchaseInvoice;
import com.invoicely.model.User;
import com.invoicely.model.enums.InvoiceStatus;
import com.invoicely.service.InvoiceService;
import com.invoicely.service.ProductService;
import com.invoicely.service.PurchaseInvoiceService;
import com.invoicely.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final UserService userService;
    private final InvoiceService invoiceService;
    private final ProductService productService;
    private final PurchaseInvoiceService purchaseInvoiceService;

    public DashboardController(UserService userService,
                               InvoiceService invoiceService,
                               ProductService productService,
                               PurchaseInvoiceService purchaseInvoiceService) {
        this.userService = userService;
        this.invoiceService = invoiceService;
        this.productService = productService;
        this.purchaseInvoiceService = purchaseInvoiceService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);

        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        List<Invoice> allInvoices = invoiceService.getInvoicesByUser(user);
        List<Invoice> monthInvoices = invoiceService.getInvoicesByUserAndDateRange(user, monthStart, monthEnd);

        BigDecimal monthIncome = monthInvoices.stream()
            .map(Invoice::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCgst = monthInvoices.stream()
            .map(Invoice::getCgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSgst = monthInvoices.stream()
            .map(Invoice::getSgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIgst = monthInvoices.stream()
            .map(Invoice::getIgst).reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        long unpaidCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.UNPAID).count();
        long overdueCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE).count();

        List<PurchaseInvoice> allPurchases = purchaseInvoiceService.getByUser(user);

        BigDecimal monthPurchases = allPurchases.stream()
            .filter(p -> !p.getInvoiceDate().isBefore(monthStart) && !p.getInvoiceDate().isAfter(monthEnd))
            .map(PurchaseInvoice::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Unpaid total (UNPAID + OVERDUE invoices)
        BigDecimal unpaidTotal = allInvoices.stream()
            .filter(i -> i.getStatus() == InvoiceStatus.UNPAID || i.getStatus() == InvoiceStatus.OVERDUE)
            .map(Invoice::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6-month chart data
        List<String> chartLabels = new ArrayList<>();
        List<Double> chartSales = new ArrayList<>();
        List<Double> chartPurchases = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            LocalDate mStart = month.withDayOfMonth(1);
            LocalDate mEnd = month.withDayOfMonth(month.lengthOfMonth());

            chartLabels.add(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));

            double sales = allInvoices.stream()
                .filter(inv -> !inv.getInvoiceDate().isBefore(mStart) && !inv.getInvoiceDate().isAfter(mEnd))
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
            chartSales.add(sales);

            double purchases = allPurchases.stream()
                .filter(p -> !p.getInvoiceDate().isBefore(mStart) && !p.getInvoiceDate().isAfter(mEnd))
                .map(PurchaseInvoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
            chartPurchases.add(purchases);
        }

        // Top 5 products by quantity sold
        Map<String, BigDecimal> productQtyMap = new LinkedHashMap<>();
        for (Invoice inv : allInvoices) {
            if (inv.getLineItems() != null) {
                for (LineItem item : inv.getLineItems()) {
                    productQtyMap.merge(item.getDescription(), item.getQuantity(), BigDecimal::add);
                }
            }
        }
        List<Map<String, Object>> topProducts = productQtyMap.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .limit(5)
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("name", e.getKey());
                m.put("quantity", e.getValue());
                return m;
            })
            .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("monthIncome", monthIncome);
        model.addAttribute("monthPurchases", monthPurchases);
        model.addAttribute("totalCgst", totalCgst);
        model.addAttribute("totalSgst", totalSgst);
        model.addAttribute("totalIgst", totalIgst);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("unpaidTotal", unpaidTotal);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartSales", chartSales);
        model.addAttribute("chartPurchases", chartPurchases);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("recentInvoices", allInvoices.stream().limit(5).toList());
        model.addAttribute("lowStockProducts", productService.getLowStockProducts(user));
        model.addAttribute("inputGstCredit", purchaseInvoiceService.getInputGstCredit(user, monthStart, monthEnd));

        return "dashboard";
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User != null) {
            return "redirect:/dashboard";
        }
        return "landing";
    }

    @GetMapping("/landing")
    public String landing() {
        return "landing";
    }
}
