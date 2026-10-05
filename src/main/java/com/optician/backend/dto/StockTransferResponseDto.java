package com.optician.backend.dto;

import com.optician.backend.model.enums.TransferStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferResponseDto {

    private Long id;
    private String transferReference;

    private Long sourceStoreId;
    private String sourceStoreName;

    private Long targetStoreId;
    private String targetStoreName;

    private TransferStatus status;

    private String createdBy;
    private String approvedBy;
    private String receivedBy;
    private String notes;

    private List<ItemDto> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDto {
        private Long id;
        private Long productVariantId;
        private String variantSku;
        private String productName;
        private Integer quantityRequested;
        private Integer quantityShipped;
        private Integer quantityReceived;
    }
}
