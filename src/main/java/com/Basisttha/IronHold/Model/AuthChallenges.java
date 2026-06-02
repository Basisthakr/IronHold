package com.Basisttha.IronHold.Model;

import java.time.LocalDateTime;
import java.util.UUID;

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
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuthChallenges {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    private UUID authId;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "auth_challenge", nullable=false)
    private User user;
    @Column(columnDefinition="TEXT")
    private String nonce;
    private LocalDateTime expiry;
    private Boolean used;

    @PrePersist
    @SuppressWarnings("unused")
    void setStuff(){
        this.used = false;
    }
}
