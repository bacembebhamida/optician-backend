package com.optician.backend.service;

import com.optician.backend.dto.ProductAuditLogDto;
import com.optician.backend.model.ProductAuditLog;
import com.optician.backend.model.enums.AuditAction;
import com.optician.backend.repository.ProductAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductAuditService {

    private final ProductAuditLogRepository auditLogRepository;

    @Transactional
    public void logAudit(AuditAction action, String entityName, Long entityId, String oldValue, String newValue) {
        String username = getCurrentUsername();

        ProductAuditLog log = ProductAuditLog.builder()
                .user(username)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .timestamp(LocalDateTime.now())
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ProductAuditLogDto> getAuditLogsForEntity(String entityName, Long entityId) {
        return auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc(entityName, entityId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "SYSTEM_OPTIVISION";
    }

    private ProductAuditLogDto toDto(ProductAuditLog entity) {
        return ProductAuditLogDto.builder()
                .id(entity.getId())
                .user(entity.getUser())
                .action(entity.getAction())
                .entityName(entity.getEntityName())
                .entityId(entity.getEntityId())
                .timestamp(entity.getTimestamp())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .build();
    }
}
