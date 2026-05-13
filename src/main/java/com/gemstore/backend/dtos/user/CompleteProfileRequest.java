package com.gemstore.backend.dtos.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CompleteProfileRequest {
    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;
}