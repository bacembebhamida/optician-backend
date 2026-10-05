package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String title;
    @Column(length = 1000)
    private String description;
    private Double discountPercentage;
    private Double discountAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private String categoryTarget; // LUNETTES_SOLEIL, TOUT, LENTILLES, 2EME_PAIRE
}
