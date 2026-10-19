package com.optigridza.authservice.service;

import com.optigridza.authservice.enums.Role;
import com.optigridza.authservice.model.User;
import com.optigridza.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.bootstrap-admin.email:}")
    private String email;

    @Value("${auth.bootstrap-admin.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) {
            return;
        }
        if (email.isBlank() || password.length() < 12
                || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).+$")) {
            throw new IllegalStateException(
                    "Bootstrap admin requires an email and a strong password (12+ characters, mixed case, number, symbol)");
        }

        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            if (user.getRole() != Role.SYSTEM_ADMIN) {
                throw new IllegalStateException(
                        "Bootstrap admin email already belongs to a non-admin user");
            }
        }, () -> userRepository.save(User.builder()
                .firstName("System")
                .lastName("Administrator")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.SYSTEM_ADMIN)
                .active(true)
                .build()));
    }
}