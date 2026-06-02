package com.Basisttha.IronHold.ExceptionHandler;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.Basisttha.IronHold.Exception.DuplicateFolderException;
import com.Basisttha.IronHold.Exception.FolderNotFoundException;
import com.Basisttha.IronHold.Exception.InvalidSignatureException;
import com.Basisttha.IronHold.Exception.NoRecoveryKeysException;
import com.Basisttha.IronHold.Exception.NotEnoughStorageException;
import com.Basisttha.IronHold.Exception.UnauthorizedException;
import com.Basisttha.IronHold.Exception.UserNotFoundException;
import com.Basisttha.IronHold.Exception.UsernameAlreadyExists;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("Error", e.getMessage()));
    }

    @ExceptionHandler(UsernameAlreadyExists.class)
    public ResponseEntity<Map<String, String>> handleUsernameAlreadyExistsException(UsernameAlreadyExists e){
        //return ResponseEntity.status(HttpStatus.IM_USED).body(Map.of("Error", e.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("Error", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("Error:", e.getMessage()));
    }

    @ExceptionHandler(InvalidSignatureException.class)
    public ResponseEntity<Map<String, String>> handleInvalidSignatureException(InvalidSignatureException e){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("Error", e.getMessage()));
    }

    @ExceptionHandler(NoRecoveryKeysException.class)
    public ResponseEntity<Map<String, String>> handleNoRecoveryKeysException(NoRecoveryKeysException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Error", e.getMessage()));
    }

    @ExceptionHandler(NotEnoughStorageException.class)
    public ResponseEntity<Map<String, String>> handleNotEnoughStorageException(NotEnoughStorageException e){
        return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE).body(Map.of("Error", e.getMessage()));
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFileNotFoundException(FileNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Error", e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedException(UnauthorizedException e){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("Error", e.getMessage()));
    }

    @ExceptionHandler(DuplicateFolderException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateFolderException(DuplicateFolderException e){
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("Error", e.getMessage()));
    }

    @ExceptionHandler(FolderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFolderNotFoundException(FolderNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Error", e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error
                -> errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }
}
/**NotEnoughStorageException, FileNotFoundException, UnauthorizedException, DuplicateFolderException, FolderNotFoundException */