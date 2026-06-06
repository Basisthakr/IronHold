package com.Basisttha.IronHold.DTO;

import java.util.UUID;

import com.Basisttha.IronHold.Model.UploadStatus;

import lombok.Data;
//This exists for updating the status of the file upload. Need to implement that.
@Data
public class CheckFileResponse {
    private UUID userId;
    private String filePath;
    private UploadStatus status;    
}
