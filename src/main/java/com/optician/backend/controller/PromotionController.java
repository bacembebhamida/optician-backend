package com.optician.backend.controller;

import com.optician.backend.model.Promotion;
import com.optician.backend.repository.PromotionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "API de gestion des offres promotionnelles et codes réductions")
public class PromotionController {

    private final PromotionRepository promotionRepository;

    @GetMapping
    @Operation(summary = "Lister toutes les promotions")
    public ResponseEntity<List<Promotion>> getAllPromotions() {
        return ResponseEntity.ok(promotionRepository.findAll());
    }

    @GetMapping("/active")
    @Operation(summary = "Lister les promotions en cours")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        return ResponseEntity.ok(promotionRepository.findByActiveTrue());
    }

    @GetMapping("/validate/{code}")
    @Operation(summary = "Vérifier un code promo")
    public ResponseEntity<Promotion> validateCode(@PathVariable String code) {
        return promotionRepository.findByCodeAndActiveTrue(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer une promotion (Admin)")
    public ResponseEntity<Promotion> createPromotion(@RequestBody Promotion promotion) {
        return ResponseEntity.ok(promotionRepository.save(promotion));
    }
}
