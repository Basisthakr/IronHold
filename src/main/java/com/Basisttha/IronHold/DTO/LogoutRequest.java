package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import lombok.Data;

@Data
public class LogoutRequest {
    private UUID userId;
    private String token;
}
