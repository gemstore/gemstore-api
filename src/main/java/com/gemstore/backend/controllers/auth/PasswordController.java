package com.gemstore.backend.controllers.auth;

import com.gemstore.backend.dtos.auth.ChangePasswordRequest;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.user.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Dedicated password operations.
 */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordController {

    private final UserService userService;

    /**
     * Change current user's password.
     */
    @PostMapping("/change")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }


    // ---  FORGOT PASSWORD (Sends Email) ---
    public record ForgotPasswordRequest(String email) {}

    @PostMapping("/forgot")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        userService.processForgotPassword(request.email());
        return ResponseEntity.ok("If an account exists, a reset link was sent.");
    }

    // ---  RESET PASSWORD (Uses Token) ---
    public record ResetPasswordRequest(String token, String newPassword) {}

    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPasswordWithToken(request.token(), request.newPassword());
        return ResponseEntity.ok("Password reset successfully");
    }
}
