package com.optigridza.authservice.service;

import com.optigridza.authservice.dto.RegisterRequest;
import com.optigridza.authservice.enums.Role;
import com.optigridza.authservice.model.User;
import com.optigridza.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceRoleTests {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @InjectMocks private AuthService authService;

    private RegisterRequest request(Role role) {
        RegisterRequest r = new RegisterRequest();
        r.setFirstName("Test");
        r.setLastName("User");
        r.setEmail("user@test.local");
        r.setPassword("Passw0rd!Passw0rd");
        r.setRole(role);
        return r;
    }

    private Role savedRole() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue().getRole();
    }

    @Test
    void adminCanAssignTechnician() {
        authService.register(request(Role.TECHNICIAN), true);
        assertEquals(Role.TECHNICIAN, savedRole());
    }

    @Test
    void adminCannotAssignSystemAdmin() {
        authService.register(request(Role.SYSTEM_ADMIN), true);
        assertEquals(Role.ENERGY_MANAGER, savedRole());
    }

    @Test
    void anonymousCallerAlwaysGetsEnergyManager() {
        authService.register(request(Role.VIEWER), false);
        assertEquals(Role.ENERGY_MANAGER, savedRole());
    }
}