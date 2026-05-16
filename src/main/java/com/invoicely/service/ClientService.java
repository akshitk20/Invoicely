package com.invoicely.service;

import com.invoicely.model.Client;
import com.invoicely.model.User;
import com.invoicely.repository.ClientRepository;
import com.invoicely.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;

    public ClientService(ClientRepository clientRepository, InvoiceRepository invoiceRepository) {
        this.clientRepository = clientRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<Client> getClientsByUser(User user) {
        return clientRepository.findByUserIdOrderByNameAsc(user.getId());
    }

    public Client getById(Long id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Client not found: " + id));
    }

    public Client save(Client client) {
        return clientRepository.save(client);
    }

    public boolean hasInvoices(Long clientId) {
        return invoiceRepository.existsByClientId(clientId);
    }

    public void delete(Long id) {
        if (hasInvoices(id)) {
            throw new IllegalStateException("Cannot delete client with existing invoices");
        }
        clientRepository.deleteById(id);
    }
}
