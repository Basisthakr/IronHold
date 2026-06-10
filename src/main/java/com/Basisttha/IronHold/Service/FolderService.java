package com.Basisttha.IronHold.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.Basisttha.IronHold.DTO.FolderResponse;
import com.Basisttha.IronHold.DTO.FolderShareRequest;
import com.Basisttha.IronHold.DTO.FolderShareRequestList;
import com.Basisttha.IronHold.DTO.PageResponse;
import com.Basisttha.IronHold.Exception.DuplicateFolderException;
import com.Basisttha.IronHold.Exception.FolderNotFoundException;
import com.Basisttha.IronHold.Exception.UnauthorizedException;
import com.Basisttha.IronHold.Exception.UserNotFoundException;
import com.Basisttha.IronHold.Model.FileShare;
import com.Basisttha.IronHold.Model.Folder;
import com.Basisttha.IronHold.Model.FolderShare;
import com.Basisttha.IronHold.Model.PermissionLevel;
import com.Basisttha.IronHold.Model.StoredFile;
import com.Basisttha.IronHold.Model.User;
import com.Basisttha.IronHold.Repository.FileRepository;
import com.Basisttha.IronHold.Repository.FileShareRepository;
import com.Basisttha.IronHold.Repository.FolderRepository;
import com.Basisttha.IronHold.Repository.FolderShareRepository;
import com.Basisttha.IronHold.Repository.UserRepository;

import io.jsonwebtoken.lang.Collections;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FileShareRepository fileShareRepository;
    private final FolderRepository folderRepository;
    private final FolderShareRepository folderShareRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    //createFolder: validate no duplicate name under same parent for same owner, build and save entity. 
    public void createFolder(String name, UUID parentFolderId, User currentUser) throws DuplicateFolderException {
        Folder parentFolder = parentFolderId==null?null:folderRepository.findById(parentFolderId).orElseThrow(() -> new FolderNotFoundException("This parent folder does not exist"));
        Optional<FolderShare> folderShare = folderShareRepository.findByFolderAndRecipientAndPermissionLevelIn(parentFolder, currentUser, List.of(PermissionLevel.READ_WRITE, PermissionLevel.SHARED_OWNER));
        if (parentFolder != null) {
            if(Boolean.TRUE.equals(parentFolder.getIsDeleted())){
                throw new FolderNotFoundException("This folder no longer exists");
            }
            boolean isOwner = parentFolder.getOwner().getUserId().equals(currentUser.getUserId());
            boolean isRecipient = folderShare.map(f -> f.getRecipient().getUserId().equals(currentUser.getUserId()) && f.getRevokedAt() == null && f.getPermissionLevel().equals(PermissionLevel.READ_WRITE) && (f.getExpiresAt() == null || f.getExpiresAt().isAfter(LocalDateTime.now()))).orElse(false);
            if (!isOwner && !isRecipient) {
                throw new UnauthorizedException("You do not have the permission to create this folder");
            }
        }
        boolean exists = folderRepository
                .findByParentFolderAndOwner(parentFolder, currentUser)
                .stream()
                .anyMatch(f -> f.getName().equals(name));
        if (exists) {
            throw new DuplicateFolderException("This folder already exists");
        }

        Folder newFolder = Folder.builder().name(name).owner(currentUser).parentFolder(parentFolder).build();
        folderRepository.save(newFolder);
    }

    public PageResponse<FolderResponse> listFolders(User currentUser, int page, int size) {
        //list all root folders in which the current user is owner
        List<Folder> temp = folderRepository.findByParentFolderAndOwner(null, currentUser);
        List<FolderShare> temp2 = folderShareRepository.findByRecipient(currentUser);
        temp2.forEach(share -> {
            if(share.getRevokedAt()==null && (share.getExpiresAt()==null ||share.getExpiresAt().isAfter(LocalDateTime.now())) && share.getFolder().getParentFolder() == null){
                temp.add(share.getFolder());
            }
        });
        return pageIt(temp, page, size);
    }

    public PageResponse<FolderResponse> pageIt(List<Folder> temp, int page, int size){
        temp.removeIf(folder -> Boolean.TRUE.equals(folder.getIsDeleted()));
        int totalFolders = temp.size();
        int totalPages = (int) Math.ceil((double) totalFolders/size);

        int from_index = page * size;
        int to_index = Math.min(from_index + size, totalFolders);
        List<Folder> pagedFolders = from_index >= totalFolders ? Collections.emptyList(): temp.subList(from_index, to_index) ;
        List<FolderResponse> content = pagedFolders.stream().map(this::toFolderResponse).collect(Collectors.toList());

        return PageResponse.<FolderResponse>builder().content(content).page(page).size(size).totalElements(totalFolders).totalPages(totalPages).last(totalFolders ==0 || page == totalPages - 1).build();
    }

    public PageResponse<FolderResponse> listFolders(UUID parentFolderId, User currentUser, int page, int size) {
        if (parentFolderId == null) {
            return listFolders(currentUser, page, size);
        }
        //this is not the root folder
        Folder parentFolder = folderRepository.findById(parentFolderId).orElseThrow(() -> new FolderNotFoundException("This folder does not exist"));
        List<Folder> temp = folderRepository.findByParentFolderAndOwner(parentFolder, currentUser);
        List<FolderShare> temp2 = folderShareRepository.findByRecipient(currentUser);
        temp2.forEach(foldershare -> {
            if(foldershare.getRevokedAt()==null && (foldershare.getExpiresAt()==null || foldershare.getExpiresAt().isAfter(LocalDateTime.now()))){
                if(foldershare.getFolder().getParentFolder()!=null && foldershare.getFolder().getParentFolder().getFolderId().equals(parentFolderId)){
                //here, the folder is good parentFolder is not null because if parentFolder is null then the folder is in the root, and since parentFolder!=null, the user does not want files in the root.
                    temp.add(foldershare.getFolder());
                }
            }
        });
        temp.removeIf(folder -> Boolean.TRUE.equals(folder.getIsDeleted()));
        //ListFoldersResponse.builder().folders(temp).build();
        return pageIt(temp, page, size);
    }

    @Transactional
    public void deleteFolder(UUID folderId, User currentUser) throws FolderNotFoundException {
        //returned the folderId of the folder deleted earlier, removed that
        //check if the folder exists, if you are the owner, or you have the shared access, change the isDeleted to yes and set the time when deleted
        Folder deleteFolder = folderRepository.findById(folderId).orElseThrow(() -> new FolderNotFoundException("This folder does not exist"));
        Optional<FolderShare> folderShare = folderShareRepository.findByFolderAndRecipientAndPermissionLevelIn(deleteFolder, currentUser, List.of(PermissionLevel.READ_WRITE, PermissionLevel.SHARED_OWNER));
        boolean isOwner = deleteFolder.getOwner().getUserId().equals(currentUser.getUserId());
        boolean isRecipient = folderShare.map(f -> f.getRecipient().getUserId().equals(currentUser.getUserId()) && f.getRevokedAt() == null && f.getPermissionLevel().equals(PermissionLevel.READ_WRITE) && (f.getExpiresAt() == null || f.getExpiresAt().isAfter(LocalDateTime.now()))).orElse(false);
        if (!isOwner && !isRecipient) {
            throw new UnauthorizedException("You do not have the permission to delete this folder");
        }
        deleteFolder.setIsDeleted(true);
        deleteFolder.setDeletedAt(LocalDateTime.now());//quota not reduced until hard delete
        folderRepository.save(deleteFolder);

        folderShare.ifPresent(fs -> {
            fs.setRevokedAt(LocalDateTime.now());//not deleting, just revoking. Will get deleted after the folder is hard deleted
            folderShareRepository.save(fs);
        });//if owner is present, but no recipient
        deleteFilesInsideDeletedFolder(deleteFolder, currentUser);
    }

    @Transactional
    public void deleteFilesInsideDeletedFolder(Folder folder, User currentUser) {
        List<StoredFile> filesInDeletedFolder = fileRepository.findByFolder(folder);
        filesInDeletedFolder.forEach(file -> {
            file.setIsDeleted(true);
            file.setDeletedAt(LocalDateTime.now());
            List<FileShare> shared = fileShareRepository.findByFile(file);
            shared.forEach(share -> {
                share.setRevokedAt(LocalDateTime.now());
                fileShareRepository.save(share);//not deleting, just revoking. Will get deleted after the folder is hard deleted
            });
            fileRepository.save(file);
        });
        List<Folder> subFolders = folderRepository.findByParentFolder(folder);
        subFolders.forEach(sub -> {
            sub.setIsDeleted(true);
            sub.setDeletedAt(LocalDateTime.now());
            folderRepository.save(sub);
            deleteFilesInsideDeletedFolder(sub, currentUser);
        });
    }

    public void shareFolder(User currentUser, FolderShareRequestList req) {
        //check if current user owns the folder, check if all the users in the recipients list exist, make a folderShare for each user in the list and save
        Folder folder = folderRepository.findById(req.getFolderId()).orElseThrow(() -> new FolderNotFoundException("This folder does not exist"));//which is better, findById or findByFolder
        boolean isRecipient = folderShareRepository.findByFolderAndRecipientAndPermissionLevelIn(folder, currentUser, List.of(PermissionLevel.READ_WRITE, PermissionLevel.SHARED_OWNER)).map(f -> f.getRevokedAt() == null && (f.getExpiresAt() == null || f.getExpiresAt().isAfter(LocalDateTime.now()))).orElse(false);
        if (!folder.getOwner().getUserId().equals(currentUser.getUserId()) && !isRecipient) {
            throw new UnauthorizedException("You do not have the permission to share this folder");
        }
        //for each recipient check if they dont already have the share
        req.getRecipients().forEach(recipient -> {
            User r = userRepository.findById(recipient.getRecipientId()).orElseThrow(() -> new UserNotFoundException("User does not exist"));
            //check if they dont already have the same file with the same permission, if yes, continue
            Optional<FolderShare> f = folderShareRepository.findByFolderAndRecipient(folder, r);
            if (f.isPresent()) {
                f.get().setPermissionLevel(recipient.getPermissionLevel());//Changed the permission level
                f.get().setRevokedAt(null);//clear any prior revocation so re-sharing actually restores access
                folderShareRepository.save(f.get());
                return;//acts like a continue here
            }
            FolderShare newShare = FolderShare.builder().owner(currentUser).folder(folder).recipient(r).permissionLevel(recipient.getPermissionLevel()).expiresAt(recipient.getExpiresAt()).build();
            folderShareRepository.save(newShare);
        });
        allFilesInFolderShare(req.getRecipients(), currentUser, folder, req.getEncryptionKeys());
    }

    public void allFilesInFolderShare(List<FolderShareRequest> recipients, User currentUser, Folder parentFolder, Map<UUID, String> encryptionKeys) {
        //since this is called after foldershare is completed, we can assume three things, the folder exists
        //all recipients in the recipients list exist the user has permission to share the folder
        List<StoredFile> filesInFolder = fileRepository.findByFolder(parentFolder);
        //if a file has already been shared to a recipient, this will create duplicate shares.
        recipients.forEach(recipient -> {
            //orElseThrow instead of get(), if user was deleted in the middle of this function, it will cause NoSuchElementException. edge case
            User r = userRepository.findById(recipient.getRecipientId()).orElseThrow(() -> new UserNotFoundException("User does not exist"));
            filesInFolder.forEach(file -> {
                String encKey = encryptionKeys.get(file.getFileId());
                FileShare newFileShare = FileShare.builder().file(file).recipient(r).permissionLevel(recipient.getPermissionLevel()).expiresAt(recipient.getExpiresAt()).encryptedFileKey(encKey).build();
                //find by file+recipient only,not permission level, so that changing permission level removes the old share rather than leaving both active
                Optional<FileShare> duplicate = fileShareRepository.findByFileAndRecipient(file, r);
                if (duplicate.isPresent()) {
                    fileShareRepository.delete(duplicate.get());
                }
                fileShareRepository.save(newFileShare);
            });
        });
        //now all files are shared, now we share all folders.
        List<Folder> subFolders = folderRepository.findByParentFolder(parentFolder);
        subFolders.forEach(subfolder -> {
            if(!Boolean.TRUE.equals(subfolder.getIsDeleted())){
                FolderShareRequestList temp = FolderShareRequestList.builder().recipients(recipients).encryptionKeys(encryptionKeys).folderId(subfolder.getFolderId()).build();
                shareFolder(currentUser, temp);
            }
        });
    }
    public FolderResponse toFolderResponse(Folder folder){
        return FolderResponse.builder().folderId(folder.getFolderId()).name(folder.getName()).parentFolderId(folder.getParentFolder() == null ? null : folder.getParentFolder().getFolderId()).createdAt(folder.getCreatedAt()).isShared(folder.getIsShared()).build();
    }
}
