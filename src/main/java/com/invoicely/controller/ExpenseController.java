package com.invoicely.controller;

import com.invoicely.model.User;
import com.invoicely.model.enums.ExpenseCategory;
import com.invoicely.service.ExpenseService;
import com.invoicely.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserService userService;

    public ExpenseController(ExpenseService expenseService, UserService userService) {
        this.expenseService = expenseService;
        this.userService = userService;
    }

    @GetMapping
    public String listExpenses(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("expenses", expenseService.getExpensesByUser(user));
        model.addAttribute("categories", ExpenseCategory.values());
        return "expenses/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("categories", ExpenseCategory.values());
        return "expenses/create";
    }

    @PostMapping
    public String createExpense(@AuthenticationPrincipal OAuth2User oAuth2User,
                                @RequestParam LocalDate expenseDate,
                                @RequestParam BigDecimal amount,
                                @RequestParam ExpenseCategory category,
                                @RequestParam(required = false) String description,
                                @RequestParam(required = false) MultipartFile receipt) throws IOException {
        User user = userService.getCurrentUser(oAuth2User);
        expenseService.createExpense(user, expenseDate, amount, category, description, receipt);
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/delete")
    public String deleteExpense(@PathVariable Long id) {
        expenseService.delete(id);
        return "redirect:/expenses";
    }
}
