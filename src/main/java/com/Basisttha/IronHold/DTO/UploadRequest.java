package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import lombok.Data;

@Data
public class UploadRequest {
    private Long sizeBytes;
    private String name;
    private String mimeType;
    private UUID parentFolderId;
}
