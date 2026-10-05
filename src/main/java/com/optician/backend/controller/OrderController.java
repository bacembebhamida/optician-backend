package com.optician.backend.controller;

import com.optician.backend.model.Order;
import com.optician.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        order.setOrderDate(LocalDateTime.now());
        if (order.getOrderReference() == null || order.getOrderReference().isEmpty()) {
            order.setOrderReference("OPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (order.getStatus() == null) {
            order.setStatus("PAYEE");
        }
        Order saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Change the production/delivery status from the optician dashboard.
     * PUT is kept for compatibility with the Angular client; PATCH is also
     * accepted for partial REST updates.
     */
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return orderRepository.findById(id)
                .map(existing -> {
                    existing.setStatus(status);
                    return ResponseEntity.ok(orderRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
