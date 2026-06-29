package com.optigridza.authservice.service;

import com.optigridza.authservice.dto.AuthResponse;
import com.optigridza.authservice.dto.LoginRequest;
import com.optigridza.authservice.dto.RegisterRequest;
import com.optigridza.authservice.model.User;
import com.optigridza.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .companyId(request.getCompanyId())
                .active(true)
                .build();

        userRepository.save(user);

        String token = jwtService.generatedToken(
          user.getEmail(),
          user.getRole().name(),
          user.getCompanyId()
        );

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .companyId(user.getCompanyId())
                .message("Registration successful")
                .build();
    }
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generatedToken(
                user.getEmail(),
                user.getRole().name(),
                user.getCompanyId()
        );

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .companyId(user.getCompanyId())
                .message("Login successful")
                .build();
    }
}
