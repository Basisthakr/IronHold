package com.Basisttha.IronHold.DTO;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecoveryKeyResponse {
    private UUID userId;
    private List<String> recoveryKeys;
}
