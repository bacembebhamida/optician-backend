package com.optician.backend.controller;

import com.optician.backend.model.Store;
import com.optician.backend.repository.StoreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "API de gestion des magasins et boutiques d'optique")
public class StoreController {

    private final StoreRepository storeRepository;

    @GetMapping
    @Operation(summary = "Obtenir la liste de tous les magasins")
    public ResponseEntity<List<Store>> getAllStores() {
        return ResponseEntity.ok(storeRepository.findAll());
    }

    @GetMapping("/active")
    @Operation(summary = "Obtenir uniquement les magasins actifs")
    public ResponseEntity<List<Store>> getActiveStores() {
        return ResponseEntity.ok(storeRepository.findByActiveTrue());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtenir un magasin par son ID")
    public ResponseEntity<Store> getStoreById(@PathVariable Long id) {
        return storeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer ou mettre à jour un magasin (Admin)")
    public ResponseEntity<Store> createOrUpdateStore(@RequestBody Store store) {
        return ResponseEntity.ok(storeRepository.save(store));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un magasin (Admin)")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        if (!storeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        storeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
