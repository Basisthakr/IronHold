package com.Basisttha.IronHold.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Basisttha.IronHold.Model.Folder;
import com.Basisttha.IronHold.Model.FolderShare;
import com.Basisttha.IronHold.Model.PermissionLevel;
import com.Basisttha.IronHold.Model.User;

@Repository
public interface  FolderShareRepository extends JpaRepository<FolderShare, UUID>{
    List<FolderShare> findByFolder(Folder folder);
    List<FolderShare> findByRecipient(User recipient);
    Optional<FolderShare> findByFolderAndRecipient(Folder folder, User recipient);
    Optional<FolderShare> findByFolderAndRecipientAndPermissionLevelIn(Folder folder, User recipient, Collection<PermissionLevel> c);
}
