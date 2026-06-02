package com.Basisttha.IronHold.DTO;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChallengeResponse {
    private String nonce;
    private LocalDateTime expiry;
}
