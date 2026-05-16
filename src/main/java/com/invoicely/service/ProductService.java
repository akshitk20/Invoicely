package com.invoicely.service;

import com.invoicely.model.Product;
import com.invoicely.model.User;
import com.invoicely.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getProductsByUser(User user) {
        return productRepository.findByUserIdOrderByNameAsc(user.getId());
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public List<Product> getLowStockProducts(User user) {
        return productRepository.findLowStockByUserId(user.getId());
    }

    @Transactional
    public void deductStock(Long productId, BigDecimal quantity) {
        Product product = getById(productId);
        BigDecimal newStock = product.getCurrentStock().subtract(quantity);
        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Insufficient stock for '" + product.getName() + "'. Available: " + product.getCurrentStock());
        }
        product.setCurrentStock(newStock);
        productRepository.save(product);
    }

    @Transactional
    public void increaseStock(Long productId, BigDecimal quantity) {
        Product product = getById(productId);
        product.setCurrentStock(product.getCurrentStock().add(quantity));
        productRepository.save(product);
    }
}
