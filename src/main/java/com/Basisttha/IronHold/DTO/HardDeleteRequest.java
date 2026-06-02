package com.Basisttha.IronHold.DTO;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class HardDeleteRequest {
    private List<UUID> fileIds;
}
