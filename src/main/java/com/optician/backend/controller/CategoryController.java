package com.optician.backend.controller;

import com.optician.backend.dto.CategoryRequestDto;
import com.optician.backend.dto.CategoryResponseDto;
import com.optician.backend.security.ProductPermissions;
import com.optician.backend.service.CategoryService;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Catégories", description = "Gestion des catégories hiérarchiques du catalogue d'optique")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize(ProductPermissions.HAS_CREATE_PERMISSION)
    @Operation(summary = "Créer une nouvelle catégorie")
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(dto));
    }

    @GetMapping
    @Operation(summary = "Obtenir l'arbre hiérarchique des catégories")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir une catégorie par ID")
    public ResponseEntity<CategoryResponseDto> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ProductPermissions.HAS_UPDATE_PERMISSION)
    @Operation(summary = "Mettre à jour une catégorie")
    public ResponseEntity<CategoryResponseDto> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequestDto dto) {
        return ResponseEntity.ok(categoryService.updateCategory(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ProductPermissions.HAS_DELETE_PERMISSION)
    @Operation(summary = "Supprimer une catégorie")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
