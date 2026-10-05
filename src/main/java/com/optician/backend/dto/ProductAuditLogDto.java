package com.optician.backend.dto;

import com.optician.backend.model.enums.AuditAction;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAuditLogDto {

    private Long id;
    private String user;
    private AuditAction action;
    private String entityName;
    private Long entityId;
    private LocalDateTime timestamp;
    private String oldValue;
    private String newValue;
}
