package com.optician.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipient;
    private String channel; // EMAIL, WHATSAPP, SMS
    private String subject;
    
    @Column(length = 2000)
    private String message;
    
    private LocalDateTime sentAt;
    private String status; // SENT, DELIVERED, FAILED
}
