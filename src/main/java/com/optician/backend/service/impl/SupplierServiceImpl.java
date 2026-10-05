package com.optician.backend.service.impl;

import com.optician.backend.dto.SupplierRequestDto;
import com.optician.backend.dto.SupplierResponseDto;
import com.optician.backend.exception.DuplicateResourceException;
import com.optician.backend.exception.ResourceNotFoundException;
import com.optician.backend.model.Supplier;
import com.optician.backend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl {

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public Page<SupplierResponseDto> getAll(Pageable pageable) {
        return supplierRepository.findAll(pageable).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public SupplierResponseDto getById(Long id) {
        return mapToDto(findOrThrow(id));
    }

    @Transactional
    public SupplierResponseDto create(SupplierRequestDto request) {
        if (supplierRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Un fournisseur avec ce nom existe déjà : " + request.getName());
        }
        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .contactName(request.getContactName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .country(request.getCountry())
                .taxIdentifier(request.getTaxIdentifier())
                .notes(request.getNotes())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        return mapToDto(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponseDto update(Long id, SupplierRequestDto request) {
        Supplier supplier = findOrThrow(id);
        // Vérifier unicité uniquement si le nom change
        if (!supplier.getName().equalsIgnoreCase(request.getName())
                && supplierRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Un fournisseur avec ce nom existe déjà : " + request.getName());
        }
        supplier.setName(request.getName());
        supplier.setContactName(request.getContactName());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setCountry(request.getCountry());
        supplier.setTaxIdentifier(request.getTaxIdentifier());
        supplier.setNotes(request.getNotes());
        if (request.getActive() != null) supplier.setActive(request.getActive());
        return mapToDto(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponseDto toggleActive(Long id, boolean active) {
        Supplier supplier = findOrThrow(id);
        supplier.setActive(active);
        return mapToDto(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = findOrThrow(id);
        // Soft delete : désactiver plutôt que supprimer
        supplier.setActive(false);
        supplierRepository.save(supplier);
        log.info("Fournisseur désactivé (soft delete): id={} name={}", id, supplier.getName());
    }

    private Supplier findOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    private SupplierResponseDto mapToDto(Supplier s) {
        return SupplierResponseDto.builder()
                .id(s.getId())
                .name(s.getName())
                .contactName(s.getContactName())
                .email(s.getEmail())
                .phone(s.getPhone())
                .address(s.getAddress())
                .city(s.getCity())
                .country(s.getCountry())
                .taxIdentifier(s.getTaxIdentifier())
                .notes(s.getNotes())
                .active(s.getActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
