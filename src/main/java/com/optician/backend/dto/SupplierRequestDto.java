package com.optician.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequestDto {

    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    private String name;

    private String contactName;

    @Email(message = "Format email invalide")
    private String email;

    private String phone;
    private String address;
    private String city;
    private String country;
    private String taxIdentifier;
    private String notes;

    @Builder.Default
    private Boolean active = true;
}
