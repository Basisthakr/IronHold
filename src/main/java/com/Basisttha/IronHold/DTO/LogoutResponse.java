package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogoutResponse {
    private UUID userId;
}
