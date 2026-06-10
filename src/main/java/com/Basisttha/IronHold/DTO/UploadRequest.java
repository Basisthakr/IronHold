package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UploadRequest {
    @NotNull(message = "sizeBytes is required")
    private Long sizeBytes;
    @NotNull(message = "name is required")
    private String name;
    private String mimeType;
    private UUID parentFolderId;
}
