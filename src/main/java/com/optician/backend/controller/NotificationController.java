package com.optician.backend.controller;

import com.optician.backend.model.NotificationLog;
import com.optician.backend.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "API de dispatch des notifications (Email, SMS, WhatsApp)")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Operation(summary = "Obtenir l'historique de toutes les notifications transmises")
    public ResponseEntity<List<NotificationLog>> getAllNotifications() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }

    @PostMapping("/send")
    @Operation(summary = "Déclencher une notification Email/SMS/WhatsApp")
    public ResponseEntity<NotificationLog> sendNotification(@RequestBody NotificationLog notification) {
        if (notification.getSentAt() == null) {
            notification.setSentAt(LocalDateTime.now());
        }
        if (notification.getStatus() == null) {
            notification.setStatus("SENT");
        }
        return ResponseEntity.ok(notificationRepository.save(notification));
    }
}
