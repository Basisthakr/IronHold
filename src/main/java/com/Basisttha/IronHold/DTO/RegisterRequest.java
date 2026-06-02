package com.Basisttha.IronHold.DTO;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotEmpty(message = "Username cannot be empty")
    @Size(min = 2, max = 50, message = "Between 2 and 50 characters only")
    private String username;
    private String publicKey;
}