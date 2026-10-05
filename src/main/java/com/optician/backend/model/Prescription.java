package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private String patientName;
    private String prescriberName;
    private LocalDate prescriptionDate;

    // Œil Droit (OD)
    private Double odSphere;
    private Double odCylinder;
    private Integer odAxis;
    private Double odAddition;

    // Œil Gauche (OG)
    private Double ogSphere;
    private Double ogCylinder;
    private Integer ogAxis;
    private Double ogAddition;

    private Double pupillaryDistance; // Écart pupillaire en mm
    private String fileUrl;
    
    @Column(length = 1000)
    private String notes;
}
