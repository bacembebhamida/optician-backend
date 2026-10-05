package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderReference;
    private LocalDateTime orderDate;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String shippingAddress;
    private Double totalAmount;
    private String status; // PAYEE, EN_PREPARATION, EXPEDIEE, LIVREE

    private Long prescriptionId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}
