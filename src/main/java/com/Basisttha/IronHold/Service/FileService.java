package com.Basisttha.IronHold.Service;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.Basisttha.IronHold.DTO.DownloadResponse;
import com.Basisttha.IronHold.DTO.FileShareRequest;
import com.Basisttha.IronHold.DTO.HardDeleteRequest;
import com.Basisttha.IronHold.DTO.ListFileResponse;
import com.Basisttha.IronHold.DTO.UploadRequest;
import com.Basisttha.IronHold.DTO.UploadResponse;
import com.Basisttha.IronHold.Exception.FolderNotFoundException;
import com.Basisttha.IronHold.Exception.NotEnoughStorageException;
import com.Basisttha.IronHold.Exception.UnauthorizedException;
import com.Basisttha.IronHold.Exception.UserNotFoundException;
import com.Basisttha.IronHold.Model.AuditAction;
import com.Basisttha.IronHold.Model.FileShare;
import com.Basisttha.IronHold.Model.Folder;
import com.Basisttha.IronHold.Model.FolderShare;
import com.Basisttha.IronHold.Model.PermissionLevel;
import com.Basisttha.IronHold.Model.StoredFile;
import com.Basisttha.IronHold.Model.UploadStatus;
import com.Basisttha.IronHold.Model.User;
import com.Basisttha.IronHold.Repository.FileRepository;
import com.Basisttha.IronHold.Repository.FileShareRepository;
import com.Basisttha.IronHold.Repository.FolderRepository;
import com.Basisttha.IronHold.Repository.FolderShareRepository;
import com.Basisttha.IronHold.Repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileShareRepository fileShareRepository;
    private final FolderShareRepository folderShareRepository;
    private final S3StorageService storageService;
    private final FolderRepository folderRepository;
    private final AuditService auditService;

    public UploadResponse initiateUpload(UploadRequest req, User currentUser) throws NotEnoughStorageException, FileNotFoundException {
        //check quota, generate s3Key, make a record in StoredFile, generate presigned url, return it
        if (req.getSizeBytes() + currentUser.getQuotaUsed() > currentUser.getQuotaAllowed()) {
            throw new NotEnoughStorageException("Storage quota exceeded");
        }

        Folder parentFolder = req.getParentFolderId() == null ? null : folderRepository.findById(req.getParentFolderId()).orElse(null);
        if(parentFolder!=null && Boolean.TRUE.equals(parentFolder.getIsDeleted())){
            throw new FileNotFoundException("File does not exist");
        }

        StoredFile newFile = StoredFile.builder()
                .folder(parentFolder)
                .owner(currentUser)
                .name(req.getName())
                .mimeType(req.getMimeType())
                .sizeBytes(req.getSizeBytes())
                .isDeleted(false)
                .uploadStatus(UploadStatus.WAITING)
                .build();
        fileRepository.save(newFile);//Need to save the file to getFileId()
        String s3ObjectKey = currentUser.getUserId() + "/" + newFile.getFileId() + ".enc";
        newFile.setS3ObjectKey(s3ObjectKey);
        fileRepository.save(newFile);

        String presignedUrl = storageService.initiateUpload(s3ObjectKey);
        System.out.println("PRESIGNED URL: " + presignedUrl);
        return UploadResponse.builder().fileId(newFile.getFileId()).preSignedUrl(presignedUrl).build();
    }

    @Transactional
    public void completeUpload(UUID fileId, User currentUser, Map<UUID, String> encryptionKeyForRecipients) throws FileNotFoundException, UnauthorizedException {
        //steps : Check if the repo has the file, check if user is owner of file, check if file is in s3, update status, lastmodifiedat for file, save, update quota for user, save, log it, done
        StoredFile file = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File Not Found"));

        if (!file.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("You do not own this file");
        }

        String s3ObjectKey = file.getS3ObjectKey();
        if (!storageService.objectExists(s3ObjectKey)) {
            throw new FileNotFoundException("This file has not been uploaded");
        }

        file.setUploadStatus(UploadStatus.DONE);
        file.setLastModifiedAt(LocalDateTime.now());
        fileRepository.save(file);

        currentUser.setQuotaUsed(currentUser.getQuotaUsed() + file.getSizeBytes());
        userRepository.save(currentUser);

        auditService.logAction(currentUser.getUserId(), AuditAction.FILE_UPLOADED, fileId, "File upload completed");
        //If here that means upload is completed. call a function to check if the upload was in a shared folder, if yes, make a fileshare
        isUploadInSharedFolder(file, currentUser, encryptionKeyForRecipients);
    }

    @Transactional
    public void isUploadInSharedFolder(StoredFile file, User currentUser, Map<UUID, String> encryptionKeyForRecipients) {
        List<FolderShare> sharedFolder = folderShareRepository.findByFolder(file.getFolder());
        if (sharedFolder.isEmpty()) {
            return;//not a shared folder
        }        //Here I need to find the recipient(s), the permission and the duration of the share, and then apply that to the file.
        sharedFolder.forEach(share -> {
            String encKey = encryptionKeyForRecipients.get(share.getRecipient().getUserId());
            FileShare temp = FileShare.builder().file(file).recipient(share.getRecipient()).permissionLevel(share.getPermissionLevel()).expiresAt(share.getExpiresAt()).encryptedFileKey(encKey).build();
            FileShare duplicate = fileShareRepository.findByFileAndRecipientAndPermissionLevel(file, share.getRecipient(), share.getPermissionLevel()).orElse(null);
            if (duplicate != null) {
                fileShareRepository.delete(duplicate);
            }
            fileShareRepository.save(temp);
        });
    }

    public DownloadResponse initiateDownload(UUID fileId, User currentUser) throws FileNotFoundException, UnauthorizedException {
        StoredFile file = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("File does not exist"));

        if (Boolean.TRUE.equals(file.getIsDeleted())) {
            throw new FileNotFoundException("File does not exist");
        }

        boolean owner = file.getOwner().getUserId().equals(currentUser.getUserId());
        boolean hasShare = fileShareRepository.findByFileAndRecipient(file, currentUser).map(share -> share.getRevokedAt() == null && (share.getExpiresAt() == null || share.getExpiresAt().isAfter(LocalDateTime.now()))).orElse(false);
        if (!owner && !hasShare) {
            throw new UnauthorizedException("You do not own this file");
        }

        String s3ObjectKey = file.getS3ObjectKey();
        if (!storageService.objectExists(s3ObjectKey)) {
            throw new FileNotFoundException("File Not Found");
        }

        String downloadUrl = storageService.initiateDownload(s3ObjectKey);

        auditService.logAction(currentUser.getUserId(), AuditAction.FILE_DOWNLOADED, fileId, "File download initiated");

        return DownloadResponse.builder().fileId(fileId).preSignedUrl(downloadUrl).build();
    }

    public void shareFile(FileShareRequest req, User currentUser) throws FileNotFoundException, UnauthorizedException {
        //check if file exists, if the current user is owner, or has rights to share it, check if recipient doesnt already have same permissions to same file, then share and save
        StoredFile file = fileRepository.findById(req.getFileId()).orElseThrow(() -> new FileNotFoundException("This file does not exist."));
        boolean isOwner = file.getOwner().getUserId().equals(currentUser.getUserId());
        boolean isRecipient = fileShareRepository.findByFileAndRecipientAndPermissionLevelIn(file, currentUser, List.of(PermissionLevel.READ_WRITE, PermissionLevel.SHARED_OWNER)).map(f -> f.getRevokedAt() == null && (f.getExpiresAt() == null || f.getExpiresAt().isAfter(LocalDateTime.now()))).orElse(false);
        if (!isOwner && !isRecipient) {
            throw new UnauthorizedException("You do not own this file");
        }
        User recipient = userRepository.findById(req.getRecipientId()).orElseThrow(() -> new UserNotFoundException("This user does not exist"));
        Optional<FileShare> sharedAlready = fileShareRepository.findByFileAndRecipient(file, recipient);
        if (sharedAlready.isPresent()) {
            sharedAlready.get().setPermissionLevel(req.getPermissionLevel());
            fileShareRepository.save(sharedAlready.get());
            return;
        }
        FileShare newShare = FileShare.builder().file(file).recipient(recipient).permissionLevel(req.getPermissionLevel()).expiresAt(req.getExpiresAt()).encryptedFileKey(req.getEncryptedFileKey()).build();
        fileShareRepository.save(newShare);
    }

    public void deleteFile(UUID fileId, User currentUser) throws FileNotFoundException, UnauthorizedException {
        //check if user owns the file, or has share request, then soft delete
        StoredFile file = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("This file does not exist"));
        boolean isOwner = file.getOwner().getUserId().equals(currentUser.getUserId());
        boolean isRecipient = fileShareRepository.findByFileAndRecipient(file, currentUser).map(share -> (share.getPermissionLevel() == PermissionLevel.READ_WRITE || share.getPermissionLevel() == PermissionLevel.SHARED_OWNER) && share.getRevokedAt() == null && (share.getExpiresAt() == null || share.getExpiresAt().isAfter(LocalDateTime.now()))).orElse(false);
        if (!isOwner && !isRecipient) {
            throw new UnauthorizedException("You do not own this file");
        }

        file.setDeletedAt(LocalDateTime.now());
        file.setIsDeleted(true);
        fileRepository.save(file);
    }

    public ListFileResponse listAllFilesInFolder(UUID parentFolderId, User currentUser) throws FolderNotFoundException {
        //do you think I should check if this person owns this folder? I should, right? Here is my logic:
        //There are only two permissions right now. Read and Read write. If this isnt your folder, you WONT be able to even see it. SO why bother to check, you obviously can see it.
        //and both read and read_write can download. But, I should check because I shouldnt trust the client

        List<StoredFile> files;
        Set<UUID> set = new HashSet<>();
        if (parentFolderId == null) {
            files = fileRepository.findByFolder(null);
        } else {
            Folder parentFolder = folderRepository.findById(parentFolderId).orElseThrow(() -> new FolderNotFoundException("This folder does not exist"));
            files = fileRepository.findByFolder(parentFolder);
        }
        files.forEach(file -> {
            set.add(file.getFileId());
        });
        List<FileShare> temp2 = fileShareRepository.findByRecipient(currentUser);
        temp2.forEach(fileshare -> {
            if (parentFolderId == null) {//comparing with null means ==
                if (fileshare.getFile().getFolder()==null && !set.contains(fileshare.getFile().getFileId())){
                    set.add(fileshare.getFile().getFileId());
                    files.add(fileshare.getFile());
                }
            }else{// we are not in the root folder hence we need to find fileshares that are in the current folder
                //comparing UUID with UUID, need equals
                if(fileshare.getFile().getFolder()!=null && fileshare.getFile().getFolder().getFolderId().equals(parentFolderId) && !set.contains(fileshare.getFile().getFileId())){
                    set.add(fileshare.getFile().getFileId());
                    files.add(fileshare.getFile());
                }
            }
        });
        files.removeIf(file -> {
            boolean isOwner = file.getOwner().getUserId()
                    .equals(currentUser.getUserId());

            List<FileShare> shares = fileShareRepository.findByFile(file);

            boolean isRecipient = shares.stream()
                    .anyMatch(share
                            -> share.getRecipient().getUserId()
                            .equals(currentUser.getUserId())
                    && share.getRevokedAt() == null
                    && (share.getExpiresAt() == null
                    || share.getExpiresAt().isAfter(LocalDateTime.now()))
                    && (share.getPermissionLevel() == PermissionLevel.READ
                    || share.getPermissionLevel() == PermissionLevel.READ_WRITE)
                    );

            return !isOwner && !isRecipient;
        });
        //Now files should only have the files that the person actually owns.
        return new ListFileResponse(files);
    }

    @Transactional
    public String hardDelete(HardDeleteRequest req, User currentUser) throws FileNotFoundException {
        //since hard delete can only be performed from the trash, it is safe toassume that the person in whose trash these files exist in has permission to delete them.
        //check if person has shared_owner access, only then he can delete the shared file

        List<StoredFile> files = new ArrayList<>();
        for (UUID fileId : req.getFileIds()) {
            StoredFile file = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException("This file does not exist"));
            if (Boolean.TRUE.equals(file.getIsDeleted())) {
                files.add(file);
            } else {
                throw new UnauthorizedException("File is not in trash");
            }
        }

        //1. check if owner -> delete
        //2. check if recipient is shared_owner, and their ownership hasnt been revoked or hasnt expired -> delete
        //else, exception -> UnauthorizedException("You do not have permission to permanently delete this file. Worried about storage space?...")
        files.removeIf(file -> {//owner
            FileShare shared = fileShareRepository.findByFileAndRecipientAndPermissionLevelIn(file, currentUser, List.of(PermissionLevel.READ_WRITE, PermissionLevel.SHARED_OWNER)).orElse(null);
            boolean hasPermission = shared != null;//if no fileshare, then you do not have permission to delete this file
            boolean isOwner = file.getOwner().getUserId().equals(currentUser.getUserId());
            return !hasPermission && !isOwner;//if owner, then return false aka not delete
        });

        //Now files only has those files which are truly removable by the currentUser;
        if (!files.isEmpty()) {
            //find out total size of all files
            files.forEach(file -> {
                //now find the owner of each file and change their usedQuota
                Long sizeBytes = file.getSizeBytes();
                User ownerOfFile = file.getOwner();
                ownerOfFile.setQuotaUsed(ownerOfFile.getQuotaUsed() - sizeBytes);
                fileShareRepository.deleteAll(fileShareRepository.findByFile(file));//deleting all fileshares 
                userRepository.save(ownerOfFile);
                storageService.deleteObject(file.getS3ObjectKey());
                fileRepository.delete(file);
            });
            return "Files in which you were owner or had access were deleted.";
        }
        return "You do not have access to delete any of the file(s) selected.";
    }
}
