package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String city;
    private String address;
    private String zipCode;
    private String phone;
    private String email;
    private String openingHours;
    private Boolean active;
    private Double latitude;
    private Double longitude;
}
