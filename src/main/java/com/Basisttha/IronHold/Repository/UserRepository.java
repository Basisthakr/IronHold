package com.Basisttha.IronHold.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Basisttha.IronHold.Model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.username LIKE 'demo_%' AND u.createdAt < :cutoff")
    List<User> findDemoUsersCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
