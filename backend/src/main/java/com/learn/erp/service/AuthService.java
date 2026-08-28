package com.learn.erp.service;

import com.learn.erp.dto.AuthRequest;
import com.learn.erp.dto.AuthResponse;
import com.learn.erp.model.User;
import com.learn.erp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;

    @Autowired
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthResponse login(AuthRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email dan password wajib diisi.");
        }

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email atau password salah."));

        if (!user.getPassword().equals(request.getPassword().trim())) {
            throw new IllegalArgumentException("Email atau password salah.");
        }

        // Generate session token (e.g. token containing role and uuid)
        String rawToken = user.getId() + ":" + user.getEmail() + ":" + user.getRole() + ":" + UUID.randomUUID();
        String token = Base64.getEncoder().encodeToString(rawToken.getBytes());

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan dengan email: " + email));
    }
}
