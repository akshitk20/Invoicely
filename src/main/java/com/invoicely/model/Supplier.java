package com.invoicely.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false)
    private String state;

    @Column(length = 15)
    private String gstin;

    private String email;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
