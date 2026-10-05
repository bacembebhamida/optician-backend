package com.optician.backend.service.impl;

import com.optician.backend.dto.ReorderSuggestionResponseDto;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.model.*;
import com.optician.backend.repository.*;
import com.optician.backend.service.ProductAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service de génération et gestion des suggestions de réapprovisionnement.
 *
 * RÈGLE : Aucune commande fournisseur n'est créée automatiquement.
 * Une validation humaine est obligatoire avant toute commande.
 *
 * Déclenchement :
 *   - Manuellement via l'API
 *   - Après chaque mouvement de stock via StockServiceImpl.checkAndGenerateAlerts()
 *
 * Logique :
 *   Si Stock.quantity <= Stock.reorderPoint
 *   ET aucune suggestion PENDING déjà existante
 *   → Créer ReorderSuggestion avec qty = maximumStock - quantity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReorderServiceImpl {

    private final StockRepository stockRepository;
    private final ReorderSuggestionRepository suggestionRepository;
    private final ProductAuditService auditService;

    @Transactional
    public int generateSuggestionsForAllLowStock() {
        List<Stock> lowStockList = stockRepository.findLowStockAll();
        int generated = 0;
        for (Stock stock : lowStockList) {
            if (generateSuggestionForStock(stock)) generated++;
        }
        log.info("Génération suggestions réapprovisionnement: {} générées sur {} stocks sous seuil",
                generated, lowStockList.size());
        return generated;
    }

    @Transactional
    public int generateSuggestionsForStore(Long storeId) {
        List<Stock> lowStockList = stockRepository.findLowStockAll().stream()
                .filter(s -> s.getStore().getId().equals(storeId))
                .toList();
        int generated = 0;
        for (Stock stock : lowStockList) {
            if (generateSuggestionForStock(stock)) generated++;
        }
        return generated;
    }

    @Transactional
    public ReorderSuggestionResponseDto approveSuggestion(Long id) {
        ReorderSuggestion suggestion = findOrThrow(id);
        suggestion.setStatus("APPROVED");
        suggestion.setProcessedBy(auditService.getCurrentUsername());
        return mapToDto(suggestionRepository.save(suggestion));
    }

    @Transactional
    public ReorderSuggestionResponseDto dismissSuggestion(Long id, String reason) {
        ReorderSuggestion suggestion = findOrThrow(id);
        suggestion.setStatus("DISMISSED");
        suggestion.setProcessedBy(auditService.getCurrentUsername());
        suggestion.setNotes(reason);
        return mapToDto(suggestionRepository.save(suggestion));
    }

    @Transactional(readOnly = true)
    public Page<ReorderSuggestionResponseDto> getAll(Pageable pageable) {
        return suggestionRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<ReorderSuggestionResponseDto> getPending(Pageable pageable) {
        return suggestionRepository.findByStatus("PENDING", pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public Page<ReorderSuggestionResponseDto> getPendingByStore(Long storeId, Pageable pageable) {
        return suggestionRepository.findPendingByStore(storeId, pageable).map(this::mapToDto);
    }

    // =========================================================
    private boolean generateSuggestionForStock(Stock stock) {
        // Éviter les doublons
        boolean exists = suggestionRepository.existsPendingForVariantAndStore(
                stock.getProductVariant().getId(), stock.getStore().getId());
        if (exists) return false;

        int current = stock.getQuantity() != null ? stock.getQuantity() : 0;
        int max = stock.getMaximumStock() != null ? stock.getMaximumStock() : 100;
        int suggested = Math.max(1, max - current);

        // Prix d'achat estimé depuis la variante
        BigDecimal unitPrice = stock.getProductVariant().getPurchasePrice();
        Supplier supplier = stock.getProductVariant().getSupplier();

        ReorderSuggestion suggestion = ReorderSuggestion.builder()
                .productVariant(stock.getProductVariant())
                .store(stock.getStore())
                .supplier(supplier)
                .currentQuantity(current)
                .reorderPoint(stock.getReorderPoint())
                .maximumStock(max)
                .suggestedQuantity(suggested)
                .estimatedUnitPrice(unitPrice)
                .estimatedDeliveryDays(supplier != null ? 7 : null)
                .status("PENDING")
                .generatedBy("SYSTEM_AUTO")
                .build();

        suggestionRepository.save(suggestion);
        log.info("Suggestion réapprovisionnement générée: variant={} store={} qty={}",
                stock.getProductVariant().getSku(), stock.getStore().getName(), suggested);
        return true;
    }

    private ReorderSuggestion findOrThrow(Long id) {
        return suggestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReorderSuggestion", id));
    }

    private ReorderSuggestionResponseDto mapToDto(ReorderSuggestion r) {
        ProductVariant v = r.getProductVariant();
        Product p = v != null ? v.getProduct() : null;
        Supplier s = r.getSupplier();

        BigDecimal totalCost = null;
        if (r.getEstimatedUnitPrice() != null && r.getSuggestedQuantity() != null) {
            totalCost = r.getEstimatedUnitPrice().multiply(BigDecimal.valueOf(r.getSuggestedQuantity()));
        }

        return ReorderSuggestionResponseDto.builder()
                .id(r.getId())
                .productVariantId(v != null ? v.getId() : null)
                .variantSku(v != null ? v.getSku() : null)
                .productName(p != null ? p.getName() : null)
                .storeId(r.getStore().getId())
                .storeName(r.getStore().getName())
                .supplierId(s != null ? s.getId() : null)
                .supplierName(s != null ? s.getName() : null)
                .currentQuantity(r.getCurrentQuantity())
                .reorderPoint(r.getReorderPoint())
                .maximumStock(r.getMaximumStock())
                .suggestedQuantity(r.getSuggestedQuantity())
                .estimatedUnitPrice(r.getEstimatedUnitPrice())
                .estimatedTotalCost(totalCost)
                .estimatedDeliveryDays(r.getEstimatedDeliveryDays())
                .status(r.getStatus())
                .generatedBy(r.getGeneratedBy())
                .processedBy(r.getProcessedBy())
                .notes(r.getNotes())
                .generatedAt(r.getGeneratedAt())
                .processedAt(r.getProcessedAt())
                .build();
    }
}
