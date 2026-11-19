package com.example.app.service.impl;

import com.example.app.constant.SecurityConstants;
import com.example.app.dto.request.LoginRequest;
import com.example.app.dto.request.RegisterRequest;
import com.example.app.dto.response.AuthResponse;
import com.example.app.dto.response.UserResponse;
import com.example.app.entity.User;
import com.example.app.enums.Role;
import com.example.app.exception.ConflictException;
import com.example.app.exception.UnauthorizedException;
import com.example.app.mapper.UserMapper;
import com.example.app.repository.UserRepository;
import com.example.app.security.CustomUserDetails;
import com.example.app.security.JwtService;
import com.example.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(Role.USER); // public registration can never create an ADMIN
        User saved = userRepository.save(user);

        log.info("Registered user id={}", saved.getId());
        return buildAuthResponse(new CustomUserDetails(saved));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalizeEmail(request.email()), request.password()));
            return buildAuthResponse((CustomUserDetails) authentication.getPrincipal());
        } catch (AuthenticationException ex) {
            // Same message for unknown email, wrong password and disabled account: no user enumeration.
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Override
    public UserResponse getCurrentUser(CustomUserDetails principal) {
        return userMapper.toResponse(principal.getUser());
    }

    private AuthResponse buildAuthResponse(CustomUserDetails principal) {
        return new AuthResponse(
                jwtService.generateToken(principal),
                SecurityConstants.TOKEN_TYPE,
                jwtService.getExpiresInSeconds(),
                userMapper.toResponse(principal.getUser()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
