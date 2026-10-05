package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private LocalDateTime appointmentDateTime;
    private String serviceType; // EXAMEN_VUE, ESSAYAGE, CONSULTATION, AJUSTEMENT
    private String storeLocation;
    private String status; // EN_ATTENTE, CONFIRME, TERMINE, ANNULE

    @Column(length = 1000)
    private String notes;
}
