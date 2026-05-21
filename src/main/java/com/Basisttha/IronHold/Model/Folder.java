package com.Basisttha.IronHold.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "folder")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID folderId;
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id", nullable = true)
    private Folder parentFolder;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Boolean isShared;

    @PrePersist
    @SuppressWarnings("unused")
    void SetStuff() {
        this.isShared = false;
    }
}
