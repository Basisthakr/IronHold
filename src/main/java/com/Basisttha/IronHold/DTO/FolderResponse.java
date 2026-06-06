package com.Basisttha.IronHold.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FolderResponse {
    private UUID folderId;
    private String name;
    private UUID parentFolderId;
    private LocalDateTime createdAt;
    private Boolean isShared;
}
