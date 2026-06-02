package com.Basisttha.IronHold.Service;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
import com.Basisttha.IronHold.Exception.InvalidSignatureException;
import com.Basisttha.IronHold.Exception.NoRecoveryKeysException;
import com.Basisttha.IronHold.Exception.UserNotFoundException;
import com.Basisttha.IronHold.Exception.UsernameAlreadyExists;
import com.Basisttha.IronHold.Model.AuthChallenges;
import com.Basisttha.IronHold.Model.RecoveryKey;
import com.Basisttha.IronHold.Model.RevokedToken;
import com.Basisttha.IronHold.Model.User;
import com.Basisttha.IronHold.Repository.AuthChallengesRepository;
import com.Basisttha.IronHold.Repository.RecoveryKeyRepository;
import com.Basisttha.IronHold.Repository.RevokedTokenRepository;
import com.Basisttha.IronHold.Repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final AuthChallengesRepository authRepo;
    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepo;
    private final RecoveryKeyRepository recoveryKeyRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public RegisterResponse RegisterUser(RegisterRequest req) throws UsernameAlreadyExists {
        //User user = userRepo.findByUsername(req.getUsername()).orElseThrow(() -> new UsernameAlreadyExists("Not Available")); wrong line
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new UsernameAlreadyExists("Username Taken");
        }
        System.out.println("Username *********************(*)(())(() : "+ req.getUsername());
        User newUser = User.builder()
                .username(req.getUsername())
                .publicKey(req.getPublicKey())
                .build();

        newUser = userRepo.save(newUser);
        return RegisterResponse.builder().userId(newUser.getUserId()).build();
    }

    public ChallengeResponse challengeRequest(ChallengeRequest req) throws UserNotFoundException {
        //verify if the user exists, if no, exception, if yes, make a nonce, set an expiry of 120 seconds, save it and send it back
        User user = userRepo.findById(req.getUserId()).orElseThrow(() -> new UserNotFoundException("This user does not exist"));

        String nonce = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(120);

        //find if there are any currently open and unused challenges, and if so, close them.
        authRepo.findByUserAndUsedFalse(user).ifPresent(existing -> {
            existing.setUsed(true);
            authRepo.save(existing);
        });

        AuthChallenges challenge = AuthChallenges.builder()
                .user(user)
                .nonce(nonce)
                .expiry(expiry)
                .build();

        authRepo.save(challenge);
        return ChallengeResponse.builder()
                .nonce(nonce)
                .expiry(expiry)
                .build();
    }

    @Transactional// crash between generating token and then not sending token. User wont be able to log in
    public AuthResponse verify(VerifyRequest req) throws UserNotFoundException, InvalidSignatureException {
        //check if user exists, check if a challenge is active and unused, check if signautre valid, if yes send token
        User currentUser = userRepo.findById(req.getUserId()).orElseThrow(() -> new UserNotFoundException("This user does not exist"));

        AuthChallenges auth = authRepo.findByUserAndUsedFalse(currentUser).orElseThrow(() -> new RuntimeException("No authentication challenge exists"));

        boolean valid = verifySignature(auth.getNonce(), req.getSignature(), currentUser.getPublicKey());

        if (valid) {
            //generate Token and send to user
            String token = jwtService.generateToken(currentUser);
            auth.setUsed(true);
            authRepo.save(auth);
            return new AuthResponse(token);

        } else {
            //invalid signature, 
            throw new InvalidSignatureException("Invalid Signature");
        }
    }

    @Transactional//Transactional here because if current token is revoked and then the function crashes before sending the new token, the user will not be able to log in
    public LogoutResponse logout(LogoutRequest req) throws UserNotFoundException, RuntimeException{
        //verify if the user has a valid JWT, then revoke that JWT and send uuid back to user
        User user = userRepo.findById(req.getUserId()).orElseThrow(() -> new UserNotFoundException("This user does not exist"));
        String token = req.getToken();
        boolean valid = jwtService.isTokenValid(token);
        if(!valid){
            throw new RuntimeException("Invalid Request");
        }
        RevokedToken revokedToken = RevokedToken.builder().token(token).expiresAt(jwtService.extractExpiry(token)).revokedAt(LocalDateTime.now()).build();
        revokedTokenRepo.save(revokedToken);
        return new LogoutResponse(user.getUserId());
    }

    @Transactional
    public AuthResponse recoverAccount(RecoverAccountRequest req) throws UserNotFoundException, NoRecoveryKeysException{
        //if user exists, get all active recovery keys(not used and not invalidated), filter them out using stream to find the matchingKey
        // use passwordEncoder to match, if no key, then error, else, put that key as used, usedOn, and save. For user, update with newPublicKey, put keyRotatedAt date and save.
        //Generate new JWT Token and send
        User user = userRepo.findById(req.getUserId()).orElseThrow(() -> new UserNotFoundException("This user does not exist"));

        List<RecoveryKey> activeKeys = recoveryKeyRepository.findByUserAndInvalidatedFalseAndUsedFalse(user);

        if(activeKeys.isEmpty()){
            throw new NoRecoveryKeysException("No recovery keys left. Please generate new ones.");
        }
        RecoveryKey matchedKey= activeKeys.stream().filter(k -> passwordEncoder.matches(req.getRecoverykey(), k.getRecoveryKeyHash())).findFirst().orElseThrow(() -> new RuntimeException("Invalid Recovery key"));

        matchedKey.setUsed(true);
        matchedKey.setUsedOn(LocalDateTime.now());
        recoveryKeyRepository.save(matchedKey);
        
        user.setPublicKey(req.getNewPublicKey());
        user.setKeyRotatedAt(LocalDateTime.now());
        userRepo.save(user);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token);
    }

    @Transactional
    public RecoveryKeyResponse rotateRecoveryKeys(RecoveryKeyRequest req) throws UserNotFoundException{
        //find previous keys that are not invalidated. Use stream, map to invalidate them. The generate 8 new keys and send back
        User user = userRepo.findById(req.getUserId()).orElseThrow(() -> new UserNotFoundException("This user does not exist"));
        List<RecoveryKey> activeKeys = recoveryKeyRepository.findByUserAndInvalidatedFalse(user);
        //activeKeys.stream().map(key -> key.setInvalidated(true)).collect(collector.toList());
        activeKeys.forEach(key -> key.setInvalidated(true));//No working keys left now
        recoveryKeyRepository.saveAll(activeKeys);
        List<String> newRecoveryKeys = generateRecoveryKeys(user);//Should I revoke the JWT if new recovery keys are generated?

        return RecoveryKeyResponse.builder().userId(user.getUserId()).recoveryKeys(newRecoveryKeys).build();
    }

    private List<String> generateRecoveryKeys(User user){
        List<String> keys = new ArrayList<>();
        for(int i=0;i<8;i++){
            String recoveryKey = UUID.randomUUID().toString();
            String recoveryKeyHash = passwordEncoder.encode(recoveryKey);//An implementation earlier generated by AI used .replace("-", ""); too here
            RecoveryKey key = RecoveryKey.builder().user(user).recoveryKeyHash(recoveryKeyHash).createdOn(LocalDateTime.now()).build();
            recoveryKeyRepository.save(key);
            keys.add(recoveryKey);
        }
        return keys;
    }

    //No @Transactional here because if crash happens, then the client will simply send another request with the new key.
    public void rotateKeyRequest(RotateKeyRequest req) throws UserNotFoundException{
        User user = userRepo.findById(req.getUserId()).orElseThrow(() -> new UserNotFoundException("This user does not exist"));
        user.setPublicKey(req.getNewPublicKey());
        user.setKeyRotatedAt(LocalDateTime.now());
        userRepo.save(user);
    }

    private boolean verifySignature(String nonce, String signatureB64, String publicKeyB64) {
        return true;

        // try {
        //     byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyB64);
        //     X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        //     KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
        //     PublicKey publicKey = keyFactory.generatePublic(keySpec);

        //     Signature sig = Signature.getInstance("Ed25519");
        //     sig.initVerify(publicKey);
        //     sig.update(nonce.getBytes());

        //     byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
        //     return sig.verify(signatureBytes);
        // } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidKeySpecException e) {
        //     return false;
        // }
    }
}
