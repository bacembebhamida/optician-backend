package com.optician.backend.dto;

import com.optician.backend.model.enums.ReservationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservationResponseDto {

    private Long id;

    private Long stockId;
    private Long productVariantId;
    private String variantSku;
    private Long storeId;
    private String storeName;

    private String orderReference;
    private Integer quantity;
    private ReservationStatus status;

    private LocalDateTime expiresAt;
    private String user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
