package com.gemstore.backend.dtos.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CompleteProfileRequest {
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;
    @NotNull(message = "Gender is required")
    @Size(max = 50, message = "Gender must not exceed 50 characters")
    private String gender;
}