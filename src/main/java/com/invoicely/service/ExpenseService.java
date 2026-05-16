package com.invoicely.service;

import com.invoicely.model.Expense;
import com.invoicely.model.User;
import com.invoicely.model.enums.ExpenseCategory;
import com.invoicely.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final String uploadDir;

    public ExpenseService(ExpenseRepository expenseRepository,
                          @Value("${app.upload-dir}") String uploadDir) {
        this.expenseRepository = expenseRepository;
        this.uploadDir = uploadDir;
    }

    public List<Expense> getExpensesByUser(User user) {
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(user.getId());
    }

    public List<Expense> getExpensesByUserAndDateRange(User user, LocalDate start, LocalDate end) {
        return expenseRepository.findByUserIdAndExpenseDateBetween(user.getId(), start, end);
    }

    public BigDecimal getTotalExpenses(User user, LocalDate start, LocalDate end) {
        return expenseRepository.sumAmountBetween(user.getId(), start, end);
    }

    public Expense createExpense(User user, LocalDate expenseDate, BigDecimal amount,
                                  ExpenseCategory category, String description,
                                  MultipartFile receipt) throws IOException {
        String receiptPath = null;
        if (receipt != null && !receipt.isEmpty()) {
            receiptPath = saveReceipt(receipt, user.getId());
        }

        Expense expense = Expense.builder()
            .user(user)
            .expenseDate(expenseDate)
            .amount(amount)
            .category(category)
            .description(description)
            .receiptPath(receiptPath)
            .build();

        return expenseRepository.save(expense);
    }

    public void delete(Long id) {
        expenseRepository.deleteById(id);
    }

    private String saveReceipt(MultipartFile file, Long userId) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path userDir = Paths.get(uploadDir, "receipts", userId.toString());
        Files.createDirectories(userDir);
        Path filePath = userDir.resolve(filename);
        file.transferTo(filePath);
        return filePath.toString();
    }
}
