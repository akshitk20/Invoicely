package com.invoicely.controller;

import com.invoicely.model.Client;
import com.invoicely.model.User;
import com.invoicely.service.ClientService;
import com.invoicely.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;
    private final UserService userService;

    public ClientController(ClientService clientService, UserService userService) {
        this.clientService = clientService;
        this.userService = userService;
    }

    @GetMapping
    public String listClients(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        User user = userService.getCurrentUser(oAuth2User);
        model.addAttribute("clients", clientService.getClientsByUser(user));
        return "clients/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("client", new Client());
        return "clients/create";
    }

    @PostMapping
    public String createClient(@AuthenticationPrincipal OAuth2User oAuth2User,
                               @RequestParam String name,
                               @RequestParam(required = false) String businessName,
                               @RequestParam(required = false) String address,
                               @RequestParam String state,
                               @RequestParam(required = false) String gstin,
                               @RequestParam(required = false) String email) {
        User user = userService.getCurrentUser(oAuth2User);
        Client client = Client.builder()
            .user(user)
            .name(name)
            .businessName(businessName)
            .address(address)
            .state(state)
            .gstin(gstin)
            .email(email)
            .build();
        clientService.save(client);
        return "redirect:/clients";
    }

    @PostMapping("/{id}/delete")
    public String deleteClient(@PathVariable Long id,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            clientService.delete(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clients";
    }
}
