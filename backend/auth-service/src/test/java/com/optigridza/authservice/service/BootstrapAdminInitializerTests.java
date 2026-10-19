package com.optigridza.authservice.service;

import com.optigridza.authservice.enums.Role;
import com.optigridza.authservice.model.User;
import com.optigridza.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminInitializerTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsConfiguredSystemAdmin() throws Exception {
        BootstrapAdminInitializer initializer = initializer("admin@test.local", "TestAdmin123!");
        when(userRepository.findByEmail("admin@test.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("TestAdmin123!")).thenReturn("encoded-password");

        initializer.run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.SYSTEM_ADMIN, userCaptor.getValue().getRole());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
    }

    @Test
    void refusesToPromoteExistingNonAdminAccount() {
        BootstrapAdminInitializer initializer = initializer("user@test.local", "TestAdmin123!");
        when(userRepository.findByEmail("user@test.local")).thenReturn(Optional.of(
                User.builder().email("user@test.local").role(Role.ENERGY_MANAGER).build()
        ));

        assertThrows(IllegalStateException.class, () -> initializer.run(null));
        verify(userRepository, never()).save(any(User.class));
    }

    private BootstrapAdminInitializer initializer(String email, String password) {
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "email", email);
        ReflectionTestUtils.setField(initializer, "password", password);
        return initializer;
    }
}