package com.invoicely.controller;

import com.invoicely.model.Invoice;
import com.invoicely.model.User;
import com.invoicely.model.enums.InvoiceStatus;
import com.invoicely.service.ExpenseService;
import com.invoicely.service.InvoiceService;
import com.invoicely.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    private final UserService userService;
    private final InvoiceService invoiceService;
    private final ExpenseService expenseService;

    public DashboardController(UserService userService,
                               InvoiceService invoiceService,
                               ExpenseService expenseService) {
        this.userService = userService;
        this.invoiceService = invoiceService;
        this.expenseService = expenseService;
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

        BigDecimal monthExpenses = expenseService.getTotalExpenses(user, monthStart, monthEnd);

        BigDecimal totalCgst = monthInvoices.stream()
            .map(Invoice::getCgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSgst = monthInvoices.stream()
            .map(Invoice::getSgst).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIgst = monthInvoices.stream()
            .map(Invoice::getIgst).reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
        long unpaidCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.UNPAID).count();
        long overdueCount = allInvoices.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE).count();

        model.addAttribute("user", user);
        model.addAttribute("monthIncome", monthIncome);
        model.addAttribute("monthExpenses", monthExpenses);
        model.addAttribute("profit", monthIncome.subtract(monthExpenses));
        model.addAttribute("totalCgst", totalCgst);
        model.addAttribute("totalSgst", totalSgst);
        model.addAttribute("totalIgst", totalIgst);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("unpaidCount", unpaidCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("recentInvoices", allInvoices.stream().limit(5).toList());

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
