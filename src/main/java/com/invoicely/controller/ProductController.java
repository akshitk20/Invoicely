package com.invoicely.controller;

import com.invoicely.model.Product;
import com.invoicely.model.User;
import com.invoicely.model.enums.UnitOfMeasure;
import com.invoicely.service.ProductService;
import com.invoicely.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final UserService userService;

    public ProductController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @GetMapping
    public String listProducts(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("products", productService.getProductsByUser(user));
        return "products/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("units", UnitOfMeasure.values());
        return "products/create";
    }

    @PostMapping
    public String createProduct(@AuthenticationPrincipal OAuth2User oAuth2User,
                                @RequestParam String name,
                                @RequestParam(required = false) String hsnCode,
                                @RequestParam UnitOfMeasure unit,
                                @RequestParam(required = false) BigDecimal sellingPrice,
                                @RequestParam(required = false) BigDecimal purchasePrice,
                                @RequestParam(required = false, defaultValue = "0") BigDecimal currentStock,
                                @RequestParam(required = false, defaultValue = "10") BigDecimal lowStockThreshold) {
        User user = userService.getCurrentUser(oAuth2User);
        Product product = Product.builder()
            .user(user)
            .name(name)
            .hsnCode(hsnCode)
            .unit(unit)
            .sellingPrice(sellingPrice)
            .purchasePrice(purchasePrice)
            .currentStock(currentStock)
            .lowStockThreshold(lowStockThreshold)
            .build();
        productService.save(product);
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete product: " + e.getMessage());
        }
        return "redirect:/products";
    }
}
