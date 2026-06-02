package com.Basisttha.IronHold.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StoredFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID fileId;
    @ManyToOne(fetch = FetchType.LAZY)//WHy manytoone, a file cannot belong to many folders, also, whats fetchtypelazy
    @JoinColumn(name = "folder_id", nullable = true)
    private Folder folder;
    @ManyToOne(fetch=FetchType.LAZY)//From a file's perspective, many of them can belong to one owner/folder
    @JoinColumn(name = "owner_uuid", nullable=false)
    private User owner;
    private String name;//name of the file
    private String mimeType;//image/jpeg, text/plain, application/pdf is mimeType
    private Long sizeBytes;
    private String s3ObjectKey;
    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;
    @CreationTimestamp
    private LocalDateTime uploadedAt;
    private LocalDateTime lastModifiedAt;
    private Boolean isDeleted;//soft delete
    private LocalDateTime deletedAt;

    @PrePersist
    @SuppressWarnings("unused")
    void setStuff(){
        this.isDeleted = false;
    }
}
