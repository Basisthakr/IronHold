package com.Basisttha.IronHold.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Basisttha.IronHold.DTO.AuthResponse;
import com.Basisttha.IronHold.DTO.ChallengeRequest;
import com.Basisttha.IronHold.DTO.ChallengeResponse;
import com.Basisttha.IronHold.DTO.LogoutRequest;
import com.Basisttha.IronHold.DTO.LogoutResponse;
import com.Basisttha.IronHold.DTO.RecoverAccountRequest;
import com.Basisttha.IronHold.DTO.RecoveryKeyRequest;
import com.Basisttha.IronHold.DTO.RecoveryKeyResponse;
import com.Basisttha.IronHold.DTO.RegisterRequest;
import com.Basisttha.IronHold.DTO.RegisterResponse;
import com.Basisttha.IronHold.DTO.RotateKeyRequest;
import com.Basisttha.IronHold.DTO.VerifyRequest;
import com.Basisttha.IronHold.Service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest req){
        System.out.println("Username authcontroller *********************(*)(())(() : "+ req.getUsername());
        return ResponseEntity.ok(userService.RegisterUser(req));
    }

    @PostMapping("/challenge")
    public ResponseEntity<ChallengeResponse> createChallenge(@RequestBody ChallengeRequest req){
        return ResponseEntity.ok(userService.challengeRequest(req));
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyRequest(@RequestBody VerifyRequest req){
        return ResponseEntity.ok(userService.verify(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest req){
        return ResponseEntity.ok(userService.logout(req));
    }

    @PostMapping("/recover")
    public ResponseEntity<AuthResponse> recoverAccount(@RequestBody RecoverAccountRequest req){
        return ResponseEntity.ok(userService.recoverAccount(req));
    }

    @PostMapping("/rotate-recovery-keys")
    public ResponseEntity<RecoveryKeyResponse> rotateRecoveryKeys(@RequestBody RecoveryKeyRequest req){
        return ResponseEntity.ok(userService.rotateRecoveryKeys(req));
    }

    @PostMapping("/rotate-public-key")
    public ResponseEntity<String> rotatePublicKey(@RequestBody RotateKeyRequest req){
        userService.rotateKeyRequest(req);
        return ResponseEntity.ok("Key rotated successfully.");
    }
}
