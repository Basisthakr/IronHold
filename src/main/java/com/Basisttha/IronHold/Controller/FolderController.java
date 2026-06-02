package com.Basisttha.IronHold.Controller;

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

import com.Basisttha.IronHold.DTO.FolderShareRequestList;
import com.Basisttha.IronHold.DTO.ListFoldersResponse;
import com.Basisttha.IronHold.Model.User;
import com.Basisttha.IronHold.Service.FolderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/folder")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping("/create")
    public ResponseEntity<String> createFolder(@RequestParam String name, @RequestParam(required = false) UUID parentFolderId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        folderService.createFolder(name, parentFolderId, currentUser);
        return ResponseEntity.ok("Folder successfully created");
    }

    @GetMapping("/list")
    public ResponseEntity<ListFoldersResponse> listFolders(@RequestParam(required = false) UUID parentFolderId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(folderService.listFolders(parentFolderId, currentUser));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFolder(@RequestParam UUID folderId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        folderService.deleteFolder(folderId, currentUser);
        return ResponseEntity.ok("The folder has been deleted");
    }

    @PostMapping("/share")
    public ResponseEntity<String> shareFolder(@RequestBody FolderShareRequestList req) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        folderService.shareFolder(currentUser, req);
        return ResponseEntity.ok("The Folder and all of its contents have been shared");
    }
}
