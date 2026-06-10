package com.Basisttha.IronHold.DTO;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class FolderShareRequestList {
    @NotNull(message = "recipients is required")
    private List<FolderShareRequest> recipients;
    @NotNull(message = "encryptionKeys is required")
    private Map<UUID, String> encryptionKeys;
    private UUID folderId;
}
