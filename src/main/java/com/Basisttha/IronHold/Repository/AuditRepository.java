package com.Basisttha.IronHold.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.Basisttha.IronHold.Model.AuditLog;
import java.util.List;


public interface AuditRepository extends JpaRepository<AuditLog, UUID>{
    List<AuditLog> findByActorId(UUID actorId);
    List<AuditLog> findByTargetId(UUID targetId);
}
