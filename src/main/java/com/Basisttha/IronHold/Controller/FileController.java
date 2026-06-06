package com.Basisttha.IronHold.Controller;

import java.io.FileNotFoundException;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Basisttha.IronHold.DTO.CompleteUploadRequest;
import com.Basisttha.IronHold.DTO.DownloadResponse;
import com.Basisttha.IronHold.DTO.FileShareRequest;
import com.Basisttha.IronHold.DTO.HardDeleteRequest;
import com.Basisttha.IronHold.DTO.PageResponse;
import com.Basisttha.IronHold.DTO.UploadRequest;
import com.Basisttha.IronHold.DTO.UploadResponse;
import com.Basisttha.IronHold.Exception.UnauthorizedException;
import com.Basisttha.IronHold.Model.StoredFile;
import com.Basisttha.IronHold.Model.User;
import com.Basisttha.IronHold.Service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> initiateUpload(@RequestBody UploadRequest req) throws FileNotFoundException{
        User currentUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(fileService.initiateUpload(req, currentUser));
    }

    @PostMapping("/upload/complete")
    public ResponseEntity<String> completeUpload(@RequestBody CompleteUploadRequest req) throws FileNotFoundException, UnauthorizedException{
        User currentUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        fileService.completeUpload(req.getFileId(), currentUser, req.getEncryptionKeyForRecipients());
        return ResponseEntity.ok("File has been uploaded");
    }

    @GetMapping("/download")
    public ResponseEntity<DownloadResponse> initiateDownload(@RequestParam UUID fileId) throws FileNotFoundException{
        User currentUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(fileService.initiateDownload(fileId, currentUser));
    }

    @PostMapping("/share")
    public ResponseEntity<String> shareFile(@RequestBody FileShareRequest req) throws FileNotFoundException{
        User currentUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        fileService.shareFile(req, currentUser);
        return ResponseEntity.ok("File Shared Successfully");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam UUID fileId) throws FileNotFoundException{
        User currentUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        fileService.deleteFile(fileId, currentUser);
        return ResponseEntity.ok("The file has been successfully deleted");
    }

    @GetMapping("/list")
    public ResponseEntity<PageResponse<StoredFile>> listAllFiles(@RequestParam(required = false) UUID parentFolderId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size){
        User currentUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(fileService.listAllFilesInFolder(parentFolderId, currentUser, page, size));
    }

    @PostMapping("/harddelete")
    public ResponseEntity<String> hardDelete(@RequestBody HardDeleteRequest req) throws FileNotFoundException{
        User currentUser = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(fileService.hardDelete(req, currentUser));
    }
}
