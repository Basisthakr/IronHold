package com.Basisttha.IronHold.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.Basisttha.IronHold.Model.AuthChallenges;
import com.Basisttha.IronHold.Model.User;

@Repository
public interface AuthChallengesRepository extends JpaRepository<AuthChallenges, UUID>{
    Optional<AuthChallenges> findByUserAndUsedFalse(User currentUser);
}
