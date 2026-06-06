package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import lombok.Data;

//This exists for updating the status of the file upload. Need to implement that.
@Data
public class CheckFileRequest {
    private UUID userId;
    private String filePath;
}
