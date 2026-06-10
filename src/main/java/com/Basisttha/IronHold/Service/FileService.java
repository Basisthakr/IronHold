package com.Basisttha.IronHold.Service;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.Basisttha.IronHold.DTO.CompleteUploadRequest;
import com.Basisttha.IronHold.DTO.DownloadResponse;
import com.Basisttha.IronHold.DTO.FileResponse;
import com.Basisttha.IronHold.DTO.FileShareRequest;
import com.Basisttha.IronHold.DTO.HardDeleteRequest;
import com.Basisttha.IronHold.DTO.PageResponse;
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

    public UploadResponse initiateUpload(UploadRequest req, User currentUser)
            throws NotEnoughStorageException, FileNotFoundException {
        // check quota, generate s3Key, make a record in StoredFile, generate presigned url, return it
        if (req.getSizeBytes() + currentUser.getQuotaUsed() > currentUser.getQuotaAllowed()) {
            throw new NotEnoughStorageException("Storage quota exceeded");
        }

        Folder parentFolder = req.getParentFolderId() == null ? null
                : folderRepository.findById(req.getParentFolderId())
                        .orElseThrow(() -> new FolderNotFoundException("This parent folder does not exist"));
        if (parentFolder != null && Boolean.TRUE.equals(parentFolder.getIsDeleted())) {
            throw new FileNotFoundException("File does not exist");
        }
        if (parentFolder != null) {
            boolean isOwner = parentFolder.getOwner().getUserId().equals(currentUser.getUserId());
            Optional<FolderShare> folderShare = folderShareRepository.findByFolderAndRecipientAndPermissionLevelIn(
                    parentFolder, currentUser, List.of(PermissionLevel.READ_WRITE, PermissionLevel.SHARED_OWNER));
            boolean hasWriteAccess = folderShare
                    .map(s -> s.getRevokedAt() == null
                            && (s.getExpiresAt() == null || s.getExpiresAt().isAfter(LocalDateTime.now())))
                    .orElse(false);
            if (!isOwner && !hasWriteAccess)
                throw new UnauthorizedException("You do not have permission to upload files in this folder");
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
        fileRepository.save(newFile);// Need to save the file to getFileId()
        String s3ObjectKey = currentUser.getUserId() + "/" + newFile.getFileId() + ".enc";
        newFile.setS3ObjectKey(s3ObjectKey);
        fileRepository.save(newFile);

        String presignedUrl = storageService.initiateUpload(s3ObjectKey);
        System.out.println("PRESIGNED URL: " + presignedUrl);
        return UploadResponse.builder().fileId(newFile.getFileId()).preSignedUrl(presignedUrl).build();
    }

    @Transactional
    public void completeUpload(CompleteUploadRequest req, User currentUser)
            throws FileNotFoundException, UnauthorizedException {
        // steps : Check if the repo has the file, check if user is owner of file, check if file is in s3, update status, lastmodifiedat for file, save, update quota
        // for user, save, log it, done
        StoredFile file = fileRepository.findById(req.getFileId())
                .orElseThrow(() -> new FileNotFoundException("File Not Found"));

        if (!file.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("You do not own this file");
        }

        String s3ObjectKey = file.getS3ObjectKey();
        if (!storageService.objectExists(s3ObjectKey)) {
            throw new FileNotFoundException("This file has not been uploaded");
        }

        if (file.getUploadStatus().equals(UploadStatus.DONE)) {
            throw new UnauthorizedException("The file has already been uploaded");
        }

        file.setUploadStatus(UploadStatus.DONE);
        file.setLastModifiedAt(LocalDateTime.now());
        fileRepository.save(file);

        currentUser.setQuotaUsed(currentUser.getQuotaUsed() + file.getSizeBytes());
        userRepository.save(currentUser);

        auditService.logAction(currentUser.getUserId(), AuditAction.FILE_UPLOADED, req.getFileId(),
                "File upload completed");
        // If here that means upload is completed. call a function to check if the upload was in a shared folder, if yes, make a fileshare.
        // encryptionKeyForRecipients is optional in the request (null is valid for files not in shared folders), so default to empty map to avoid NPE inside isUploadInSharedFolder
        Map<UUID, String> encKeys = req.getEncryptionKeyForRecipients() != null
                ? req.getEncryptionKeyForRecipients()
                : new java.util.HashMap<>();
        isUploadInSharedFolder(file, currentUser, encKeys);
    }

    @Transactional
    public void isUploadInSharedFolder(StoredFile file, User currentUser, Map<UUID, String> encryptionKeyForRecipients)
            throws UnauthorizedException {
        if (file.getFolder() == null)
            return;
        List<FolderShare> sharedFolder = folderShareRepository.findByFolder(file.getFolder());
        sharedFolder.removeIf(share -> {
            boolean isNotRevoked = share.getRevokedAt() == null;// if true then not revoked
            boolean isNotExpired = share.getExpiresAt() == null || share.getExpiresAt().isAfter(LocalDateTime.now());
            return !isNotRevoked || !isNotExpired;
        });
        if (sharedFolder.isEmpty()) {
            return;// not a shared folder
        } // Here I need to find the recipients, the permission and the duration of the share, and then apply that to the file.
        sharedFolder.forEach(share -> {
            String encKey = encryptionKeyForRecipients.get(share.getRecipient().getUserId());
            if (encKey == null) {
                return;
            }
            FileShare temp = FileShare.builder().file(file).recipient(share.getRecipient())
                    .permissionLevel(share.getPermissionLevel()).expiresAt(share.getExpiresAt())
                    .encryptedFileKey(encKey).build();
            FileShare duplicate = fileShareRepository
                    .findByFileAndRecipientAndPermissionLevel(file, share.getRecipient(), share.getPermissionLevel())
                    .orElse(null);
            if (duplicate != null) {
                fileShareRepository.delete(duplicate);
            }
            fileShareRepository.save(temp);
        });
        // If Alice is owner, and Bob uploads a file in Alice's folder, then Alice
        // should get a fileshare for Bob's file
        Folder folder = file.getFolder();
        if (!folder.getOwner().getUserId().equals(currentUser.getUserId())) {
            // current user is NOT the owner of the file
            FolderShare temp = folderShareRepository.findByFolderAndRecipient(folder, currentUser).orElseThrow(
                    () -> new UnauthorizedException("You do not have permission to upload in this folder"));// to get
                                                                                                            // the
                                                                                                            // permission
                                                                                                            // that
                                                                                                            // current
                                                                                                            // user has
                                                                                                            // in the
                                                                                                            // shared
                                                                                                            // folder,
                                                                                                            // so we can
                                                                                                            // replicate
                                                                                                            // the same
                                                                                                            // permission
                                                                                                            // for
                                                                                                            // currentUser's
                                                                                                            // file to
                                                                                                            // folder
                                                                                                            // owner
            String encKey = encryptionKeyForRecipients.get(folder.getOwner().getUserId());
            FileShare ownerShare = FileShare.builder().file(file).recipient(folder.getOwner())
                    .permissionLevel(temp.getPermissionLevel()).expiresAt(temp.getExpiresAt()).encryptedFileKey(encKey)
                    .build();
            fileShareRepository.save(ownerShare);
        }
    }

    public DownloadResponse initiateDownload(UUID fileId, User currentUser)
            throws FileNotFoundException, UnauthorizedException {
        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File does not exist"));

        if (Boolean.TRUE.equals(file.getIsDeleted())) {
            throw new FileNotFoundException("File does not exist");
        }

        boolean owner = file.getOwner().getUserId().equals(currentUser.getUserId());
        boolean hasShare = fileShareRepository.findByFileAndRecipient(file, currentUser)
                .map(share -> share.getRevokedAt() == null
                        && (share.getExpiresAt() == null || share.getExpiresAt().isAfter(LocalDateTime.now())))
                .orElse(false);
        if (!owner && !hasShare) {
            throw new UnauthorizedException("You do not own this file");
        }

        String s3ObjectKey = file.getS3ObjectKey();
        if (!storageService.objectExists(s3ObjectKey)) {
            throw new FileNotFoundException("File Not Found");
        }

        String downloadUrl = storageService.initiateDownload(s3ObjectKey, file.getName());

        auditService.logAction(currentUser.getUserId(), AuditAction.FILE_DOWNLOADED, fileId, "File download initiated");

        return DownloadResponse.builder().fileId(fileId).preSignedUrl(downloadUrl).build();
    }

    public void shareFile(FileShareRequest req, User currentUser) throws FileNotFoundException, UnauthorizedException {
        // check if file exists, if the current user is owner, or has rights to share it, check if recipient doesnt already have same permissions to same file,
        // then share and save
        StoredFile file = fileRepository.findById(req.getFileId())
                .orElseThrow(() -> new FileNotFoundException("This file does not exist."));
        boolean isOwner = file.getOwner().getUserId().equals(currentUser.getUserId());
        boolean isRecipient = fileShareRepository
                .findByFileAndRecipientAndPermissionLevelIn(file, currentUser,
                        List.of(PermissionLevel.READ_WRITE, PermissionLevel.SHARED_OWNER))
                .map(f -> f.getRevokedAt() == null
                        && (f.getExpiresAt() == null || f.getExpiresAt().isAfter(LocalDateTime.now())))
                .orElse(false);
        if (!isOwner && !isRecipient) {
            throw new UnauthorizedException("You do not own this file");
        }
        User recipient = userRepository.findById(req.getRecipientId())
                .orElseThrow(() -> new UserNotFoundException("This user does not exist"));
        Optional<FileShare> sharedAlready = fileShareRepository.findByFileAndRecipient(file, recipient);
        if (sharedAlready.isPresent()) {
            fileShareRepository.delete(sharedAlready.get());
        }
        FileShare newShare = FileShare.builder().file(file).recipient(recipient)
                .permissionLevel(req.getPermissionLevel()).expiresAt(req.getExpiresAt())
                .encryptedFileKey(req.getEncryptedFileKey()).build();
        fileShareRepository.save(newShare);
    }

    public void deleteFile(UUID fileId, User currentUser) throws FileNotFoundException, UnauthorizedException {
        // check if user owns the file, or has share request, then soft delete
        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("This file does not exist"));
        if(file.getIsDeleted()){
            return;
        }
        boolean isOwner = file.getOwner().getUserId().equals(currentUser.getUserId());
        boolean isRecipient = fileShareRepository.findByFileAndRecipient(file, currentUser)
                .map(share -> (share.getPermissionLevel() == PermissionLevel.READ_WRITE
                        || share.getPermissionLevel() == PermissionLevel.SHARED_OWNER) && share.getRevokedAt() == null
                        && (share.getExpiresAt() == null || share.getExpiresAt().isAfter(LocalDateTime.now())))
                .orElse(false);
        if (!isOwner && !isRecipient) {
            throw new UnauthorizedException("You do not own this file");
        }

        file.setDeletedAt(LocalDateTime.now());
        file.setIsDeleted(true);
        fileRepository.save(file);
    }

    public PageResponse<FileResponse> listAllFilesInFolder(UUID parentFolderId, User currentUser, int page, int size)
            throws FolderNotFoundException {
        // Steps: 1. Get all files that are in that folder, files in which I am owner 2.
        // Get files in that folder that are shared to me 3. Remove all files that I
        // dont own or isnt shared to me
        List<StoredFile> filesOutput;// filesOwnedByMe
        Set<UUID> set = new HashSet<>();// having this set helps in de-duplication, when adding in files from fileshares
        if (parentFolderId == null) {
            filesOutput = new ArrayList<>(fileRepository.findByFolderAndOwner(null, currentUser));// only current user's root files shared root 
            // files are added below using everyFileSharedToMe
        } else {
            Folder parentFolder = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new FolderNotFoundException("This folder does not exist"));
            filesOutput = fileRepository.findByFolder(parentFolder);
        }
        filesOutput.forEach(file -> {
            set.add(file.getFileId());
        });
        List<FileShare> everyFileSharedToMe = fileShareRepository.findByRecipient(currentUser);// this contains
                                                                                               // information for all
                                                                                               // the fileshares that I
                                                                                               // own, NOT necessarily
                                                                                               // in the same folder, I
                                                                                               // will figure that out
                                                                                               // later
        everyFileSharedToMe.removeIf(file -> {
            boolean isRevoked = file.getRevokedAt() != null;
            boolean isExpired = file.getExpiresAt() != null && file.getExpiresAt().isBefore(LocalDateTime.now());

            return isRevoked || isExpired;
        });
        everyFileSharedToMe.forEach(fileshare -> {
            if (parentFolderId == null) {// comparing with null means == we are finding files in root.
                if (fileshare.getFile().getFolder() == null && !set.contains(fileshare.getFile().getFileId())) {
                    set.add(fileshare.getFile().getFileId());
                    filesOutput.add(fileshare.getFile());
                }
            } else {// we are not in the root folder hence we need to find fileshares that are in the current folder
                    // comparing UUID with UUID, need equals
                if (fileshare.getFile().getFolder() != null
                        && fileshare.getFile().getFolder().getFolderId().equals(parentFolderId)
                        && !set.contains(fileshare.getFile().getFileId())) {
                    set.add(fileshare.getFile().getFileId());
                    filesOutput.add(fileshare.getFile());
                }
            }
        });

        Map<UUID, List<FileShare>> sharedMap = new HashMap<>();
        everyFileSharedToMe.forEach(fileshare -> {
            UUID fileId = fileshare.getFile().getFileId();
            sharedMap.computeIfAbsent(fileId, k -> new ArrayList<>()).add(fileshare);
        });

        // using the above line, I got all the files
        filesOutput.removeIf(file -> {// removes all shares where I am not owner or the share has expired or revoked
            boolean isOwner = file.getOwner().getUserId()
                    .equals(currentUser.getUserId());

            List<FileShare> shares = sharedMap.get(file.getFileId());

            boolean isRecipient = shares != null && shares.stream()
                    .anyMatch(share -> share.getRecipient().getUserId().equals(currentUser.getUserId())
                            && share.getRevokedAt() == null
                            && (share.getExpiresAt() == null || share.getExpiresAt().isAfter(LocalDateTime.now())));

            return !isOwner && !isRecipient;
        });
        // Now files should only have the files that the person actually owns.
        int totalElements = filesOutput.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<StoredFile> result = fromIndex >= toIndex ? Collections.emptyList()
                : filesOutput.subList(fromIndex, toIndex);
        List<FileResponse> content = result.stream().map(this::toFileResponse).collect(Collectors.toList());
        return PageResponse.<FileResponse>builder().content(content).page(page).size(size).totalElements(totalElements)
                .totalPages(totalPages).last(totalElements == 0 || page == totalPages - 1).build();
    }

    @Transactional
    public String hardDelete(HardDeleteRequest req, User currentUser) throws FileNotFoundException {
        // since hard delete can only be performed from the trash, it is safe toassume
        // that the person in whose trash these files exist in has permission to delete
        // them. check if person has shared_owner access, only then he can delete the shared file

        List<StoredFile> files = new ArrayList<>();
        for (UUID fileId : req.getFileIds()) {
            StoredFile file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new FileNotFoundException("This file does not exist"));
            if (Boolean.TRUE.equals(file.getIsDeleted())) {
                files.add(file);
            } else {
                throw new UnauthorizedException("File is not in trash");
            }
        }

        // 1. check if owner -> delete
        // 2. check if recipient is shared_owner, and their ownership hasnt been revoked
        // or hasnt expired -> delete
        // else, exception -> UnauthorizedException("You do not have permission to
        // permanently delete this file. Worried about storage space?...")
        files.removeIf(file -> {// Removes those files in which currentUser doesnt have permission to delete
            FileShare shared = fileShareRepository.findByFileAndRecipientAndPermissionLevelIn(file, currentUser,
                    List.of(PermissionLevel.SHARED_OWNER)).orElse(null);
            boolean hasPermission = shared != null && shared.getRevokedAt() == null
                    && (shared.getExpiresAt() == null || shared.getExpiresAt().isAfter(LocalDateTime.now()));// if no fileshare,then you do not have permission to delete this file
            boolean isOwner = file.getOwner().getUserId().equals(currentUser.getUserId());
            return !hasPermission && !isOwner;// if owner, then return false aka not delete
        });

        // Now files only has those files which are truly removable by the currentUser;
        if (!files.isEmpty()) {
            // Find all fileshares that have these files, in a single DB query. Why? We need
            // to delete fileShares too.
            List<FileShare> allShares = fileShareRepository.findByFileIn(files);
            fileShareRepository.deleteAllInBatch(allShares);
            // group by fileId per user
            /*Map<UUID, User> ownerMap = new HashMap<>();//Map saves the User object, so that we can update all at once in DB, saving multiple calls
            files.forEach(file -> {
                User owner = file.getOwner();
                ownerMap.putIfAbsent(owner.getUserId(),owner);
                ownerMap.get(owner.getUserId()).setQuotaUsed(ownerMap.get(owner.getUserId()).getQuotaUsed() - file.getSizeBytes());
            });
            userRepository.saveAll(ownerMap.values());//Map's values have the updated User objects, save all at once */
            files.forEach(file -> {
                file.getOwner().setQuotaUsed(file.getOwner().getQuotaUsed() - file.getSizeBytes());
            });
            userRepository.saveAll(
                files.stream().map(StoredFile::getOwner).distinct().collect(Collectors.  toList())
            );
            files.forEach(file -> {
                storageService.deleteObject(file.getS3ObjectKey());
            });
            fileRepository.deleteAllInBatch(files);
        }
        return "Files in which you were owner or had access were deleted";
    }

    public FileResponse toFileResponse(StoredFile file) {
        return FileResponse.builder().fileId(file.getFileId()).mimeType(file.getMimeType())
                .sizeBytes(file.getSizeBytes()).originalName(file.getName()).uploadStatus(file.getUploadStatus())
                .folderId(file.getFolder() == null ? null : file.getFolder().getFolderId())
                .isDeleted(file.getIsDeleted()).build();
    }
}