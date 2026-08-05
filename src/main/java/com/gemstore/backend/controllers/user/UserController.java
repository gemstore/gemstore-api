package com.gemstore.backend.controllers.user;

import com.gemstore.backend.dtos.user.UpdateProfileRequest;
import com.gemstore.backend.dtos.user.UserResponse;
import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.user.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // ==================== GENERAL ====================

    @GetMapping
    public ResponseEntity<List<UserResponse>> listAll() {
        return ResponseEntity.ok(
                userService.findAll()
                        .stream()
                        .map(userMapper::toUserResponse)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
//                userMapper.toUserResponse(userService.getById(id))
                userService.getUserResponseById(id)
        );
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getByUsername(@PathVariable String username) {
        return ResponseEntity.ok(
//                userMapper.toUserResponse(userService.findActiveByUsername(username))
                userService.getUserResponseByUsername(username)
        );
    }

    // ==================== /me ====================

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
//                userMapper.toUserResponse(userService.getById(principal.getId()))
                userService.getUserResponseById(principal.getId())
        );
    }

    @PatchMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> patchProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                userMapper.toUserResponse(
                        userService.updateProfile(principal.getId(), request)
                )
        );
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                userMapper.toUserResponse(
                        userService.updateProfile(principal.getId(), request)
                )
        );
    }

    @PostMapping("/me/avatar")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        String avatarUrl = userService.uploadAvatar(principal.getId(), file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    @DeleteMapping("/me/avatar")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        userService.deleteAvatar(principal.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteSelf(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        userService.softDelete(principal.getId());
        return ResponseEntity.noContent().build();
    }

    // ==================== ADMIN ====================

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        userService.deleteHard(id);
        return ResponseEntity.noContent().build();
    }
}
