package com.temacart.service;

import com.temacart.dto.request.LoginRequest;
import com.temacart.dto.request.RegisterRequest;
import com.temacart.dto.response.AuthResponse;
import com.temacart.entity.Role;
import com.temacart.entity.User;
import com.temacart.repository.UserRepository;
import com.temacart.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    // ---------- REGISTER ----------
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRoles(Set.of(Role.CUSTOMER));

        User saved = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(saved.getEmail())
                .password(saved.getPassword())
                .authorities(saved.getRoles().stream()
                        .map(r -> "ROLE_" + r.name())
                        .toArray(String[]::new))
                .build();

        String token = jwtService.generateToken(userDetails);

        Set<String> roles = saved.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new AuthResponse(token, saved.getEmail(), saved.getFullName(), roles);
    }

    // ---------- LOGIN ----------
    public AuthResponse login(LoginRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(userDetails);

        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new AuthResponse(token, user.getEmail(), user.getFullName(), roles);
    }
}
