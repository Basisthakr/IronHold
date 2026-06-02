package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import lombok.Data;

@Data
public class RotateKeyRequest {
    private UUID userId;
    private String newPublicKey;
}
