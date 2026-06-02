package com.Basisttha.IronHold.Service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.Basisttha.IronHold.Model.AuditAction;
import com.Basisttha.IronHold.Model.AuditLog;
import com.Basisttha.IronHold.Repository.AuditRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    public void logAction(UUID actorId, AuditAction action, UUID targetId, String message){
        AuditLog audit = AuditLog.builder().actorId(actorId).auditMessage(message).what(action).targetId(targetId).build();
        auditRepository.save(audit);
    }
}
