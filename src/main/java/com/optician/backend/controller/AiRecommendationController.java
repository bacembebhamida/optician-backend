package com.optician.backend.controller;

import com.optician.backend.model.*;
import com.optician.backend.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
@Tag(name = "AI Recommendation & Reimbursement API", description = "Moteur IA de recommandation de montures selon morphologie et simulateur 100% Santé Tiers-Payant")
public class AiRecommendationController {

    private final ProductRepository productRepository;

    @PostMapping("/recommend-frames")
    @Operation(summary = "Recommander des montures selon la morphologie du visage et style (IA Vision)")
    public ResponseEntity<AiRecommendationResponse> recommendFrames(@RequestBody AiRecommendationRequest request) {
        List<Product> allProducts = productRepository.findAll();
        List<AiRecommendationResponse.RecommendedProduct> recommendations = new ArrayList<>();

        String shape = request.getFaceShape() != null ? request.getFaceShape().toUpperCase() : "OVAL";

        for (Product p : allProducts) {
            int score = 75;
            String reason = "Harmonie visuelle équilibrée";

            String pFaceShape = p.getFaceShape() != null ? p.getFaceShape() : "";
            String pFrameType = p.getFrameType() != null ? p.getFrameType() : "";

            if (shape.equals("CARRE") && (pFaceShape.equalsIgnoreCase("CARRE") || pFrameType.equalsIgnoreCase("ACETATE"))) {
                score = 96;
                reason = "Atténue les angles anguleux de votre visage avec une touche élégante";
            } else if (shape.equals("ROND") && pFrameType.equalsIgnoreCase("TITANE")) {
                score = 94;
                reason = "Affine et allonge le contour de votre visage";
            } else if (shape.equals("OVALE") || shape.equals("OVAL")) {
                score = 98;
                reason = "Parfaite compatibilité universelle avec les lignes de votre visage";
            }

            if (Boolean.TRUE.equals(p.getTryOn3dAvailable()) || Boolean.TRUE.equals(p.getVirtualTryOnEnabled())) {
                score += 2;
            }
            if (score > 99) score = 99;

            recommendations.add(AiRecommendationResponse.RecommendedProduct.builder()
                    .product(p)
                    .matchScorePercentage(score)
                    .matchReason(reason)
                    .build());
        }

        // Sort descending by match score
        recommendations.sort((a, b) -> Integer.compare(b.getMatchScorePercentage(), a.getMatchScorePercentage()));

        String advice = "Selon l'analyse de votre visage " + shape + ", nous préconisons des montures ultra-légères en titane ou acétate avec verres antireflets haute définition.";

        AiRecommendationResponse response = AiRecommendationResponse.builder()
                .detectedFaceShape(shape)
                .stylingAdvice(advice)
                .recommendations(recommendations)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate-reimbursement")
    @Operation(summary = "Simuler la prise en charge Sécurité Sociale & Mutuelle (100% Santé Tiers Payant)")
    public ResponseEntity<ReimbursementResponse> calculateReimbursement(@RequestBody ReimbursementRequest request) {
        double framePrice = request.getFramePrice() != null ? request.getFramePrice() : 150.0;
        double lensPrice = request.getLensPrice() != null ? request.getLensPrice() : 100.0;
        double total = framePrice + lensPrice;

        double secu;
        double mutuelle;
        boolean is100Sante = "PANIER_100_SANTE".equalsIgnoreCase(request.getOfferCategory());

        if (is100Sante) {
            secu = total * 0.15;
            mutuelle = total * 0.85;
        } else {
            secu = 0.15 * 30.0; // Standard SS base
            String tier = request.getMutuelleLevel() != null ? request.getMutuelleLevel() : "TIER_2_CONFORT";
            if ("TIER_3_PREMIUM".equalsIgnoreCase(tier)) {
                mutuelle = total * 0.80;
            } else if ("TIER_2_CONFORT".equalsIgnoreCase(tier)) {
                mutuelle = total * 0.60;
            } else {
                mutuelle = total * 0.40;
            }
        }

        double totalCoverage = Math.min(total, secu + mutuelle);
        double resteACharge = Math.max(0.0, total - totalCoverage);

        ReimbursementResponse response = ReimbursementResponse.builder()
                .totalPrice(total)
                .secuCoverage(Math.round(secu * 100.0) / 100.0)
                .mutuelleCoverage(Math.round(mutuelle * 100.0) / 100.0)
                .totalCoverage(Math.round(totalCoverage * 100.0) / 100.0)
                .resteAChargeClient(Math.round(resteACharge * 100.0) / 100.0)
                .isFullyCovered(resteACharge <= 0.01)
                .offerLabel(is100Sante ? "Offre 100% Santé (Zero Reste à Charge)" : "Panier Libre avec Mutuelle Complémentaire")
                .build();

        return ResponseEntity.ok(response);
    }
}
