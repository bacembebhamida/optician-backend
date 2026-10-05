package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;
    private String productCategory;
    private Double unitPrice;
    private Integer quantity;
    private String lensType; // Anti-reflet, Anti-lumière bleue, Progressive, Standard
}
