package com.Basisttha.IronHold.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Basisttha.IronHold.Model.RecoveryKey;
import com.Basisttha.IronHold.Model.User;


@Repository
public interface RecoveryKeyRepository extends JpaRepository<RecoveryKey, UUID>{
    List<RecoveryKey> findByRecoveryKeyHashAndInvalidatedFalseAndUsedFalse(String recoveryKeyHash);
    List<RecoveryKey> findByUserAndInvalidatedFalse(User user);
    List<RecoveryKey> findByUserAndInvalidatedFalseAndUsedFalse(User user);
    List<RecoveryKey> findByUser(User user);
}