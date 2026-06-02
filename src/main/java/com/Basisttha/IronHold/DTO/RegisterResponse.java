package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private UUID userId;
}
