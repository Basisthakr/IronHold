package com.Basisttha.IronHold.Model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RecoveryKey {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID recoveryKeyId;
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name = "recovery_key_user", nullable = false)
    private User user;
    @Column(nullable=false, columnDefinition="TEXT")
    private String recoveryKeyHash;
    private Boolean invalidated;
    private Boolean used;
    private LocalDateTime usedOn;
    @CreationTimestamp
    private LocalDateTime createdOn;

    @PrePersist
    @SuppressWarnings("unused")
    void setStuff(){
        this.invalidated = false;
        this.used = false;
    }
}
