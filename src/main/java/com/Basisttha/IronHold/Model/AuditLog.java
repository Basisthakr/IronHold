package com.Basisttha.IronHold.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AuditLog{
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID auditId;
    private UUID actorId;
    private String auditMessage;
    @Enumerated(EnumType.STRING)
    private AuditAction what;
    private UUID targetId;//which folder was the target?
    @CreationTimestamp
    private LocalDateTime createdAt;
}
