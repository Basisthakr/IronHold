package com.Basisttha.IronHold.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Basisttha.IronHold.Model.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, UUID >{
    boolean existsByToken(String token);

    void deleteAllByExpiresAtBefore(LocalDateTime dateTime);

    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);
}
