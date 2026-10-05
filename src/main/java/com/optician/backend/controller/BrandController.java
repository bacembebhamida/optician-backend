package com.optician.backend.controller;

import com.optician.backend.dto.BrandRequestDto;
import com.optician.backend.dto.BrandResponseDto;
import com.optician.backend.security.ProductPermissions;
import com.optician.backend.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Tag(name = "Marques", description = "Gestion des marques du catalogue d'optique")
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    @PreAuthorize(ProductPermissions.HAS_CREATE_PERMISSION)
    @Operation(summary = "Créer une nouvelle marque")
    public ResponseEntity<BrandResponseDto> createBrand(@Valid @RequestBody BrandRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.createBrand(dto));
    }

    @GetMapping
    @Operation(summary = "Liste de toutes les marques")
    public ResponseEntity<List<BrandResponseDto>> getAllBrands() {
        return ResponseEntity.ok(brandService.getAllBrands());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une marque par ID")
    public ResponseEntity<BrandResponseDto> getBrandById(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ProductPermissions.HAS_UPDATE_PERMISSION)
    @Operation(summary = "Mettre à jour une marque")
    public ResponseEntity<BrandResponseDto> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequestDto dto) {
        return ResponseEntity.ok(brandService.updateBrand(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ProductPermissions.HAS_DELETE_PERMISSION)
    @Operation(summary = "Supprimer une marque")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}
