package com.Basisttha.IronHold.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Basisttha.IronHold.Model.FileShare;
import com.Basisttha.IronHold.Model.PermissionLevel;
import com.Basisttha.IronHold.Model.StoredFile;
import com.Basisttha.IronHold.Model.User;


@Repository
public interface FileShareRepository extends JpaRepository<FileShare, UUID>{
    List<FileShare> findByFile(StoredFile file);
    List<FileShare> findByFileIn(List<StoredFile> file);
    List<FileShare> findByRecipient(User recipient);

    Optional<FileShare> findByFileAndRecipient(StoredFile file, User recipient);
    Optional<FileShare> findByFileAndRecipientAndPermissionLevelIn(StoredFile file, User recipient, Collection<PermissionLevel> permissionLevels);
    Optional<FileShare> findByFileAndRecipientAndPermissionLevel(StoredFile file, User recipient, PermissionLevel p);
}
