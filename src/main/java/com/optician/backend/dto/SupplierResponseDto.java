package com.optician.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponseDto {

    private Long id;
    private String name;
    private String contactName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String taxIdentifier;
    private String notes;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
