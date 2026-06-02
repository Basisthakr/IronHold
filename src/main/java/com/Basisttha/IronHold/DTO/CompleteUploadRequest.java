package com.Basisttha.IronHold.DTO;

import java.util.Map;
import java.util.UUID;

import lombok.Data;

@Data
public class CompleteUploadRequest {
    private UUID fileId;
    private Map<UUID, String> encryptionKeyForRecipients;
}
