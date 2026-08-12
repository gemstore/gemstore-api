package com.gemstore.backend.services.user;

import com.gemstore.backend.dtos.auth.ChangePasswordRequest;
import com.gemstore.backend.entities.user.User;
import com.gemstore.backend.exceptions.UserNotFoundException;
import com.gemstore.backend.mappers.user.UserMapper;
import com.gemstore.backend.repositories.user.FollowRepository;
import com.gemstore.backend.repositories.user.PasswordResetTokenRepository;
import com.gemstore.backend.repositories.user.UserRepository;
import com.gemstore.backend.services.auth.EmailService;
import com.gemstore.backend.services.storage.FileStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {

        testUser = new User();

        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@gemstore.com");
        testUser.setDisplayName("Test User");
        testUser.setStatus("ACTIVE");
        testUser.setRole("USER");
        testUser.setPasswordHash("$2a$12$hashedpassword");
    }

    // ============================================================
    // GET BY ID
    // ============================================================

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("TC-USR-001: Should return user when found")
        void shouldReturnUser() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            User result = userService.getById(1L);

            assertThat(result)
                    .isNotNull();

            assertThat(result.getUsername())
                    .isEqualTo("testuser");

            verify(userRepository)
                    .findActiveById(1L);
        }

        @Test
        @DisplayName("TC-USR-002: Should throw when user not found")
        void shouldThrowWhenNotFound() {

            when(userRepository.findActiveById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.getById(99L)
            )
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository)
                    .findActiveById(99L);
        }
    }

    // ============================================================
    // FIND ALL
    // ============================================================

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("TC-USR-003: Should return only active users")
        void shouldExcludeSoftDeleted() {

            when(userRepository.findAllActive())
                    .thenReturn(List.of(testUser));

            List<User> result = userService.findAll();

            assertThat(result)
                    .hasSize(1);

            assertThat(result.get(0).getId())
                    .isEqualTo(1L);

            assertThat(result.get(0).getUsername())
                    .isEqualTo("testuser");

            verify(userRepository)
                    .findAllActive();

            verify(userRepository, never())
                    .findAll();
        }
    }

    // ============================================================
    // FIND ACTIVE BY USERNAME
    // ============================================================

    @Nested
    @DisplayName("findActiveByUsername()")
    class FindActiveByUsername {

        @Test
        @DisplayName("TC-USR-004: Should find active user by username")
        void shouldFindActiveUserByUsername() {

            when(userRepository.findActiveByUsername("testuser"))
                    .thenReturn(Optional.of(testUser));

            User result =
                    userService.findActiveByUsername("testuser");

            assertThat(result)
                    .isNotNull();

            assertThat(result.getUsername())
                    .isEqualTo("testuser");

            assertThat(result.getEmail())
                    .isEqualTo("test@gemstore.com");

            verify(userRepository)
                    .findActiveByUsername("testuser");
        }

        @Test
        @DisplayName(
                "TC-USR-005: Should throw when active username not found"
        )
        void shouldThrowWhenNotFound() {

            when(userRepository.findActiveByUsername("ghost"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.findActiveByUsername("ghost")
            )
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("ghost");

            verify(userRepository)
                    .findActiveByUsername("ghost");
        }
    }

    // ============================================================
    // UPDATE STATUS
    // ============================================================

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("TC-USR-006: Should update status to uppercase")
        void shouldUpdateStatus() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            when(userRepository.save(any(User.class)))
                    .thenReturn(testUser);

            User result =
                    userService.updateStatus(1L, "banned");

            assertThat(testUser.getStatus())
                    .isEqualTo("BANNED");

            assertThat(result)
                    .isSameAs(testUser);

            verify(userRepository)
                    .save(testUser);
        }
    }

    // ============================================================
    // UPDATE ROLE
    // ============================================================

    @Nested
    @DisplayName("updateRole()")
    class UpdateRole {

        @Test
        @DisplayName("TC-USR-007: Should update role to ADMIN")
        void shouldUpdateToAdmin() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            when(userRepository.save(any(User.class)))
                    .thenReturn(testUser);

            User result =
                    userService.updateRole(1L, "ADMIN");

            assertThat(testUser.getRole())
                    .isEqualTo("ADMIN");

            assertThat(result)
                    .isSameAs(testUser);

            verify(userRepository)
                    .save(testUser);
        }

        @Test
        @DisplayName("TC-USR-008: Should strip ROLE_ prefix")
        void shouldStripPrefix() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            when(userRepository.save(any(User.class)))
                    .thenReturn(testUser);

            userService.updateRole(
                    1L,
                    "ROLE_ADMIN"
            );

            assertThat(testUser.getRole())
                    .isEqualTo("ADMIN");

            verify(userRepository)
                    .save(testUser);
        }

        @Test
        @DisplayName("TC-USR-009: Should reject unsupported role")
        void shouldRejectBadRole() {

            assertThatThrownBy(() ->
                    userService.updateRole(
                            1L,
                            "SUPERADMIN"
                    )
            )
                    .isInstanceOf(
                            IllegalArgumentException.class
                    )
                    .hasMessageContaining(
                            "Unsupported role"
                    );

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("TC-USR-010: Should reject blank role")
        void shouldRejectBlank() {

            assertThatThrownBy(() ->
                    userService.updateRole(
                            1L,
                            ""
                    )
            )
                    .isInstanceOf(
                            IllegalArgumentException.class
                    )
                    .hasMessageContaining(
                            "Role must not be blank"
                    );

            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("TC-USR-011: Should not save if same role")
        void shouldSkipIfSameRole() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            User result =
                    userService.updateRole(
                            1L,
                            "USER"
                    );

            assertThat(result)
                    .isSameAs(testUser);

            assertThat(testUser.getRole())
                    .isEqualTo("USER");

            verify(userRepository, never())
                    .save(any(User.class));
        }

        @Test
        @DisplayName(
                "TC-USR-012: Should reject role change for deleted user"
        )
        void shouldRejectForDeletedUser() {

            testUser.setDeletedAt(
                    Instant.now()
            );

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            assertThatThrownBy(() ->
                    userService.updateRole(
                            1L,
                            "ADMIN"
                    )
            )
                    .isInstanceOf(
                            IllegalStateException.class
                    )
                    .hasMessageContaining(
                            "deleted"
                    );

            verify(userRepository, never())
                    .save(any(User.class));
        }
    }

    // ============================================================
    // CHANGE PASSWORD
    // ============================================================

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName(
                "TC-USR-013: Should change password successfully"
        )
        void shouldChangePassword() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            when(passwordEncoder.matches(
                    "oldpass",
                    "$2a$12$hashedpassword"
            ))
                    .thenReturn(true);

            when(passwordEncoder.matches(
                    "newpass123",
                    "$2a$12$hashedpassword"
            ))
                    .thenReturn(false);

            when(passwordEncoder.encode("newpass123"))
                    .thenReturn("$2a$12$newhash");

            ChangePasswordRequest req =
                    new ChangePasswordRequest();

            req.setCurrentPassword("oldpass");
            req.setNewPassword("newpass123");

            userService.changePassword(
                    1L,
                    req
            );

            assertThat(testUser.getPasswordHash())
                    .isEqualTo("$2a$12$newhash");

            verify(passwordEncoder)
                    .encode("newpass123");

            verify(userRepository)
                    .save(testUser);
        }

        @Test
        @DisplayName(
                "TC-USR-014: Should reject wrong current password"
        )
        void shouldRejectWrongPassword() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            when(passwordEncoder.matches(
                    "wrongpass",
                    "$2a$12$hashedpassword"
            ))
                    .thenReturn(false);

            ChangePasswordRequest req =
                    new ChangePasswordRequest();

            req.setCurrentPassword("wrongpass");
            req.setNewPassword("newpass123");

            assertThatThrownBy(() ->
                    userService.changePassword(
                            1L,
                            req
                    )
            )
                    .isInstanceOf(
                            IllegalArgumentException.class
                    )
                    .hasMessageContaining(
                            "incorrect"
                    );

            verify(userRepository, never())
                    .save(any(User.class));
        }

        @Test
        @DisplayName(
                "TC-USR-015: Should reject same password"
        )
        void shouldRejectSamePassword() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            when(passwordEncoder.matches(
                    "samepass",
                    "$2a$12$hashedpassword"
            ))
                    .thenReturn(true);

            ChangePasswordRequest req =
                    new ChangePasswordRequest();

            req.setCurrentPassword("samepass");
            req.setNewPassword("samepass");

            assertThatThrownBy(() ->
                    userService.changePassword(
                            1L,
                            req
                    )
            )
                    .isInstanceOf(
                            IllegalArgumentException.class
                    )
                    .hasMessageContaining(
                            "different"
                    );

            verify(userRepository, never())
                    .save(any(User.class));
        }

        @Test
        @DisplayName(
                "TC-USR-016: Should reject blank new password"
        )
        void shouldRejectBlankNewPassword() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            ChangePasswordRequest req =
                    new ChangePasswordRequest();

            req.setCurrentPassword("oldpass");
            req.setNewPassword("");

            assertThatThrownBy(() ->
                    userService.changePassword(
                            1L,
                            req
                    )
            )
                    .isInstanceOf(
                            IllegalArgumentException.class
                    )
                    .hasMessageContaining(
                            "blank"
                    );

            verifyNoInteractions(passwordEncoder);

            verify(userRepository, never())
                    .save(any(User.class));
        }
    }

    // ============================================================
    // SOFT DELETE
    // ============================================================

    @Nested
    @DisplayName("softDelete()")
    class SoftDelete {

        @Test
        @DisplayName("TC-USR-017: Should soft delete user")
        void shouldSoftDelete() {

            when(userRepository.findActiveById(1L))
                    .thenReturn(Optional.of(testUser));

            userService.softDelete(1L);

            assertThat(testUser.getDeletedAt())
                    .isNotNull();

            verify(userRepository)
                    .save(testUser);
        }
    }

    // ============================================================
    // HARD DELETE
    // ============================================================

    @Nested
    @DisplayName("deleteHard()")
    class DeleteHard {

        @Test
        @DisplayName("TC-USR-018: Should hard delete user")
        void shouldHardDelete() {

            User user = User.builder()
                    .id(1L)
                    .build();

            when(userRepository.findById(1L))
                    .thenReturn(Optional.of(user));

            userService.deleteHard(1L);

            verify(userRepository)
                    .delete(user);
        }

        @Test
        @DisplayName(
                "TC-USR-019: Should throw when user not found for hard delete"
        )
        void shouldThrowWhenNotFound() {

            when(userRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.deleteHard(99L)
            )
                    .isInstanceOf(
                            UserNotFoundException.class
                    );

            verify(userRepository, never())
                    .delete(any(User.class));
        }
    }
}