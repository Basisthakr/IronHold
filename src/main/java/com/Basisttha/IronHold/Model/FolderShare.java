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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderShare {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID folderShareId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_owner", nullable=false)
    private User owner;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_recipient", nullable=false)
    private User recipient;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_share", nullable = false)
    private Folder folder;
    @Enumerated(EnumType.STRING)//Every file in that folder will get the same permission level
    private PermissionLevel permissionLevel;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
    private LocalDateTime expiresAt;
}