package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import lombok.Data;

@Data
public class CheckFileRequest {
    private UUID userId;
    private String filePath;
}
