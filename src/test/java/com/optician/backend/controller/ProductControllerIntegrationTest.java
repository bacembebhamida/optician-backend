package com.optician.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optician.backend.dto.ProductRequestDto;
import com.optician.backend.dto.ProductVariantRequestDto;
import com.optician.backend.dto.StockEntryRequestDto;
import com.optician.backend.model.enums.ProductType;
import com.optician.backend.model.enums.StockMovementType;
import com.optician.backend.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreRepository storeRepository;

    @Test
    @DisplayName("GET /api/products should return list of products")
    void getAllProducts_Success() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/products/search should filter with pagination")
    void searchProducts_Success() throws Exception {
        mockMvc.perform(get("/api/products/search")
                        .param("query", "Ray-Ban")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(authorities = {"PRODUCT_CREATE"})
    @DisplayName("POST /api/products with invalid data returns structured 400 error")
    void createProduct_ValidationErrorFormat() throws Exception {
        ProductRequestDto invalidDto = ProductRequestDto.builder()
                .name("")
                .productType(null)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields").exists());
    }

    @Test
    @WithMockUser(authorities = {"PRODUCT_CREATE"})
    @DisplayName("POST /api/products successfully creates a commercial model (without variant data)")
    void createProduct_Success() throws Exception {
        ProductRequestDto validDto = ProductRequestDto.builder()
                .reference("TEST-MODEL-99")
                .name("Monture Test Titanium")
                .productType(ProductType.OPTICAL_FRAME)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.reference").value("TEST-MODEL-99"))
                .andExpect(jsonPath("$.sku").doesNotExist())
                .andExpect(jsonPath("$.variantCount").value(0))
                .andExpect(jsonPath("$.stock").value(0));
    }

    @Test
    @WithMockUser(authorities = {"PRODUCT_CREATE", "STOCK_ENTRY"})
    @DisplayName("Product derives price and stock from its variants and per-store stocks")
    void createProductWithVariant_DerivedPriceAndStock() throws Exception {
        // 1. Créer un modèle commercial
        ProductRequestDto modelDto = ProductRequestDto.builder()
                .reference("TEST-MODEL-200")
                .name("Ray-Ban RX5228")
                .productType(ProductType.OPTICAL_FRAME)
                .build();

        String createBody = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modelDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long productId = objectMapper.readTree(createBody).get("id").asLong();

        // 2. Ajouter une variante (SKU, barcode et prix au niveau déclinaison)
        ProductVariantRequestDto variantDto = ProductVariantRequestDto.builder()
                .sku("RB-RX5228-BLK")
                .barcode("8052890123456")
                .color("Noir Mat")
                .size("52-18-140")
                .purchasePrice(new BigDecimal("300.00"))
                .sellingPrice(new BigDecimal("450.00"))
                .active(true)
                .build();

        String variantBody = mockMvc.perform(post("/api/products/{id}/variants", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variantDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long variantId = objectMapper.readTree(variantBody).get("id").asLong();

        // 3. Entrée de stock via StockService uniquement
        long storeId = storeRepository.findAll().iterator().next().getId();

        StockEntryRequestDto entryDto = StockEntryRequestDto.builder()
                .productVariantId(variantId)
                .storeId(storeId)
                .quantity(3)
                .type(StockMovementType.PURCHASE)
                .reference("BL-2026-001")
                .reason("Réception fournisseur")
                .build();

        mockMvc.perform(post("/api/stocks/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryDto)))
                .andExpect(status().isOk());

        // 4. Le modèle expose des valeurs dérivées : prix à partir de / stock total / nb variantes
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("TEST-MODEL-200"))
                .andExpect(jsonPath("$.variantCount").value(1))
                .andExpect(jsonPath("$.stock").value(3))
                .andExpect(jsonPath("$.price").value(450.0));
    }
}
