package com.Basisttha.IronHold.DTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FolderShareRequestList {
    private List<FolderShareRequest> recipients;
    private Map<UUID, String> encryptionKeys;
    private UUID folderId;
}
