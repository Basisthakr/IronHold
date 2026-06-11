package com.Basisttha.IronHold.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Basisttha.IronHold.Model.AuthChallenges;
import com.Basisttha.IronHold.Model.User;

public interface AuthChallengesRepository extends JpaRepository<AuthChallenges, UUID>{
    Optional<AuthChallenges> findByUserAndUsedFalse(User currentUser);
    List<AuthChallenges> findByUser(User user);
}
