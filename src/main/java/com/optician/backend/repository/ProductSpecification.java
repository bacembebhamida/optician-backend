package com.optician.backend.repository;

import com.optician.backend.dto.ProductSearchFilter;
import com.optician.backend.model.Product;
import com.optician.backend.model.ProductVariant;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Recherche des Products (MODÈLES COMMERCIAUX).
 *
 * Les critères liés aux déclinaisons (sku, barcode, couleur, prix) sont résolus
 * via des sous-requêtes corrélées sur ProductVariant : Product ne porte plus
 * aucune donnée de déclinaison.
 */
public class ProductSpecification {

    public static Specification<Product> filterBy(ProductSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return cb.conjunction();
            }

            // Recherche textuelle globale
            if (filter.getQuery() != null && !filter.getQuery().trim().isEmpty()) {
                String searchPattern = "%" + filter.getQuery().trim().toLowerCase() + "%";
                List<Predicate> textMatches = new ArrayList<>();
                textMatches.add(cb.like(cb.lower(root.get("name")), searchPattern));
                textMatches.add(cb.like(cb.lower(root.get("model")), searchPattern));
                textMatches.add(cb.like(cb.lower(root.get("description")), searchPattern));
                textMatches.add(cb.like(cb.lower(root.get("legacyBrand")), searchPattern));

                var brandJoin = root.join("brandEntity", jakarta.persistence.criteria.JoinType.LEFT);
                textMatches.add(cb.like(cb.lower(brandJoin.get("name")), searchPattern));
                var catJoin = root.join("categoryEntity", jakarta.persistence.criteria.JoinType.LEFT);
                textMatches.add(cb.like(cb.lower(catJoin.get("name")), searchPattern));
                textMatches.add(cb.like(cb.lower(root.get("legacyCategory")), searchPattern));

                String skuPattern = "%" + filter.getQuery().trim().toLowerCase() + "%";
                textMatches.add(variantExists(root, query, cb, (sub, subRoot) ->
                        sub.where(cb.like(cb.lower(subRoot.get("sku")), skuPattern))));

                predicates.add(cb.or(textMatches.toArray(new Predicate[0])));
            }

            // SKU exact (déclinaison)
            if (filter.getSku() != null && !filter.getSku().trim().isEmpty()) {
                String sku = filter.getSku().trim();
                predicates.add(variantExists(root, query, cb, (sub, subRoot) ->
                        sub.where(cb.equal(cb.lower(subRoot.get("sku")), sku.toLowerCase()))));
            }

            // Barcode exact (déclinaison)
            if (filter.getBarcode() != null && !filter.getBarcode().trim().isEmpty()) {
                String barcode = filter.getBarcode().trim();
                predicates.add(variantExists(root, query, cb, (sub, subRoot) ->
                        sub.where(cb.equal(cb.lower(subRoot.get("barcode")), barcode.toLowerCase()))));
            }

            // Marque
            if (filter.getBrandId() != null) {
                predicates.add(cb.equal(root.join("brandEntity", jakarta.persistence.criteria.JoinType.LEFT).get("id"), filter.getBrandId()));
            } else if (filter.getBrand() != null && !filter.getBrand().trim().isEmpty()) {
                String brandPattern = "%" + filter.getBrand().trim().toLowerCase() + "%";
                var brandJoin = root.join("brandEntity", jakarta.persistence.criteria.JoinType.LEFT);
                Predicate entityBrand = cb.like(cb.lower(brandJoin.get("name")), brandPattern);
                Predicate legacyBrand = cb.like(cb.lower(root.get("legacyBrand")), brandPattern);
                predicates.add(cb.or(entityBrand, legacyBrand));
            }

            // Catégorie
            if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.join("categoryEntity", jakarta.persistence.criteria.JoinType.LEFT).get("id"), filter.getCategoryId()));
            } else if (filter.getCategory() != null && !filter.getCategory().trim().isEmpty()) {
                String catPattern = "%" + filter.getCategory().trim().toLowerCase() + "%";
                var catJoin = root.join("categoryEntity", jakarta.persistence.criteria.JoinType.LEFT);
                Predicate entityCat = cb.like(cb.lower(catJoin.get("name")), catPattern);
                Predicate legacyCat = cb.like(cb.lower(root.get("legacyCategory")), catPattern);
                predicates.add(cb.or(entityCat, legacyCat));
            }

            if (filter.getProductType() != null) {
                predicates.add(cb.equal(root.get("productType"), filter.getProductType()));
            }

            // Couleur (déclinaison)
            if (filter.getColor() != null && !filter.getColor().trim().isEmpty()) {
                String colorPattern = "%" + filter.getColor().trim().toLowerCase() + "%";
                predicates.add(variantExists(root, query, cb, (sub, subRoot) ->
                        sub.where(cb.like(cb.lower(subRoot.get("color")), colorPattern))));
            }

            if (filter.getMaterial() != null && !filter.getMaterial().trim().isEmpty()) {
                String matPattern = "%" + filter.getMaterial().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("material")), matPattern));
            }

            if (filter.getGender() != null) {
                predicates.add(cb.equal(root.get("gender"), filter.getGender()));
            }

            if (filter.getFrameShape() != null) {
                predicates.add(cb.equal(root.get("frameShape"), filter.getFrameShape()));
            }

            // Prix (plage sur les prix de vente des déclinaisons)
            if (filter.getMinPrice() != null) {
                predicates.add(variantExists(root, query, cb, (sub, subRoot) ->
                        sub.where(cb.greaterThanOrEqualTo(subRoot.get("sellingPrice"), filter.getMinPrice()))));
            }

            if (filter.getMaxPrice() != null) {
                predicates.add(variantExists(root, query, cb, (sub, subRoot) ->
                        sub.where(cb.lessThanOrEqualTo(subRoot.get("sellingPrice"), filter.getMaxPrice()))));
            }

            if (filter.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), filter.getActive()));
            }

            if (filter.getFeatured() != null) {
                predicates.add(cb.equal(root.get("featured"), filter.getFeatured()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Sous-requête corrélée : produit ayant au moins une variante satisfaisant le prédicat.
     */
    private static Predicate variantExists(Root<Product> root, CriteriaQuery<?> query,
                                           CriteriaBuilder cb,
                                           BiConsumer<Subquery<Long>, Root<ProductVariant>> configurator) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<ProductVariant> subRoot = sub.from(ProductVariant.class);
        sub.select(subRoot.get("id"));
        sub.where(cb.equal(subRoot.get("product"), root));
        configurator.accept(sub, subRoot);
        return cb.exists(sub);
    }
}
