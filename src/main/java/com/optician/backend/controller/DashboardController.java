package com.optician.backend.controller;

import com.optician.backend.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ProductRepository productRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        long totalProducts = productRepository.count();
        long totalPatients = patientRepository.count();
        long totalAppointments = appointmentRepository.count();
        long totalOrders = orderRepository.count();

        double totalRevenue = orderRepository.findAll().stream()
                .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0.0)
                .sum();

        long lowStockCount = productRepository.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() < 10)
                .count();

        DashboardStats stats = DashboardStats.builder()
                .totalProducts(totalProducts)
                .totalPatients(totalPatients)
                .totalAppointments(totalAppointments)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .lowStockCount(lowStockCount)
                .activeStores(storeRepository.findByActiveTrue().size())
                .build();

        return ResponseEntity.ok(stats);
    }

    @Data
    @Builder
    public static class DashboardStats {
        private long totalProducts;
        private long totalPatients;
        private long totalAppointments;
        private long totalOrders;
        private double totalRevenue;
        private long lowStockCount;
        private int activeStores;
    }
}
