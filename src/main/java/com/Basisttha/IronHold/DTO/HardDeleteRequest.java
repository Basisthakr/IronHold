package com.Basisttha.IronHold.DTO;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HardDeleteRequest {
    @NotNull(message = "fileIds is required")
    private List<UUID> fileIds;
}
