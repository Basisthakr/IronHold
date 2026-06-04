package com.Basisttha.IronHold.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;
    private String username;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @CreationTimestamp
    private LocalDateTime lastAccessedAt;
    @Enumerated(EnumType.STRING)
    private Status status;
    private Long quotaAllowed;
    private Long quotaUsed;
    private String publicKey;
    private LocalDateTime keyRotatedAt;

    @PrePersist
    @SuppressWarnings("unused")
    void setCreation(){
        this.quotaUsed = 0L;
        this.quotaAllowed = 5000000000L;//aka 5GB(Giga not Gibi) allowed at first. SHould be in application.properties!
        this.status = Status.ACTIVE;
    }
}
