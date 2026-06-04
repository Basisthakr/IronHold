package com.Basisttha.IronHold.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Basisttha.IronHold.Model.StoredFile;
import com.Basisttha.IronHold.Model.User;
import com.Basisttha.IronHold.Model.Folder;
import com.Basisttha.IronHold.Model.UploadStatus;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface FileRepository extends JpaRepository<StoredFile, UUID>{
    
    List<StoredFile> findByOwner(User owner);
    List<StoredFile> findByFolder(Folder folder);
    List<StoredFile> findByUploadStatus(UploadStatus uploadStatus);
    List<StoredFile> findByUploadedAtBeforeAndIsDeleted(LocalDateTime cutoff, Boolean isDeleted);
}
