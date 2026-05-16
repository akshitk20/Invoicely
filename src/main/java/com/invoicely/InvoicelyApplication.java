package com.invoicely;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvoicelyApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoicelyApplication.class, args);
    }
}
