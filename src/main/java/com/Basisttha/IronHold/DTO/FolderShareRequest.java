package com.Basisttha.IronHold.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import com.Basisttha.IronHold.Model.PermissionLevel;

import lombok.Data;

@Data
public class FolderShareRequest {
    private UUID recipientId;
    private PermissionLevel permissionLevel;
    private LocalDateTime expiresAt;
}