package com.Basisttha.IronHold.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import com.Basisttha.IronHold.Model.PermissionLevel;

import lombok.Data;

@Data
public class FileShareRequest {
    private UUID fileId;
    private UUID recipientId;
    private LocalDateTime expiresAt;
    private PermissionLevel permissionLevel;
    private String encryptedFileKey;
}