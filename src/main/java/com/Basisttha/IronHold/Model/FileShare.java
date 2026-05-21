package com.Basisttha.IronHold.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class FileShare {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID fileShareId;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="shared_file", nullable = false)
    private StoredFile file;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "recipient", nullable=false)
    private User recipient;
    private String encryptedFileKey;
    @Enumerated(EnumType.STRING)
    private PermissionLevel permissionLevel;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
}
