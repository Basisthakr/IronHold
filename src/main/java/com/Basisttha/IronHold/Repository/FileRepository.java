package com.Basisttha.IronHold.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Basisttha.IronHold.Model.Folder;
import com.Basisttha.IronHold.Model.StoredFile;
import com.Basisttha.IronHold.Model.UploadStatus;
import com.Basisttha.IronHold.Model.User;


@Repository
public interface FileRepository extends JpaRepository<StoredFile, UUID>{
    
    Page<StoredFile> findByOwner(User owner, Pageable pageable);
    List<StoredFile> findByOwner(User owner);
    Page<StoredFile> findByFolder(Folder folder, Pageable pageable);
    List<StoredFile> findByFolder(Folder folder);
    List<StoredFile> findByFolderAndOwner(Folder folder, User owner);
    Page<StoredFile> findByUploadStatus(UploadStatus uploadStatus);
    Page<StoredFile> findByUploadedAtBeforeAndIsDeleted(LocalDateTime cutoff, Boolean isDeleted);

    @Query("SELECT f FROM StoredFile f WHERE f.owner.username LIKE 'demo_%' AND f.uploadedAt < :cutoff AND f.isDeleted =false")
    List<StoredFile> findDemoFilesOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
