package com.invoicely.controller;

import com.invoicely.model.Supplier;
import com.invoicely.model.User;
import com.invoicely.service.SupplierService;
import com.invoicely.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final UserService userService;

    public SupplierController(SupplierService supplierService, UserService userService) {
        this.supplierService = supplierService;
        this.userService = userService;
    }

    @GetMapping
    public String listSuppliers(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("suppliers", supplierService.getSuppliersByUser(user));
        return "suppliers/list";
    }

    @GetMapping("/new")
    public String showCreateForm() {
        return "suppliers/create";
    }

    @PostMapping
    public String createSupplier(@AuthenticationPrincipal OAuth2User oAuth2User,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String businessName,
                                 @RequestParam(required = false) String address,
                                 @RequestParam String state,
                                 @RequestParam(required = false) String gstin,
                                 @RequestParam(required = false) String email) {
        User user = userService.getCurrentUser(oAuth2User);
        Supplier supplier = Supplier.builder()
            .user(user)
            .name(name)
            .businessName(businessName)
            .address(address)
            .state(state)
            .gstin(gstin)
            .email(email)
            .build();
        supplierService.save(supplier);
        return "redirect:/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String deleteSupplier(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            supplierService.delete(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete supplier: " + e.getMessage());
        }
        return "redirect:/suppliers";
    }
}
