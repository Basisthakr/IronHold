package com.Basisttha.IronHold.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Basisttha.IronHold.Model.Folder;
import com.Basisttha.IronHold.Model.User;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID>{
    List<Folder> findByName(String name);//Multiple folders can exist with the same name inside different folders
    List<Folder> findByOwner(User owner);

    //Boolean existsByFolderId(UUID folderId);

    List<Folder> findByParentFolder(Folder parentFolder);
    List<Folder> findByParentFolderAndOwner(Folder parentFolder, User owner);
}
