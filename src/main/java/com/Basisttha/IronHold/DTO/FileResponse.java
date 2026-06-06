package com.Basisttha.IronHold.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import com.Basisttha.IronHold.Model.UploadStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileResponse {
    private UUID fileId;
    private String mimeType;
    private Long sizeBytes;
    private String originalName;
    private UploadStatus uploadStatus;
    private LocalDateTime uploadedAt;
    private UUID folderId;
}
