package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.JwtAuthenticationResponse;
import com.ap101gamestudio.timetracker.dto.LoginRequest;
import com.ap101gamestudio.timetracker.dto.RegisterRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkPolicyRepository;
import com.ap101gamestudio.timetracker.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final WorkPolicyRepository workPolicyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            WorkPolicyRepository workPolicyRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.workPolicyRepository = workPolicyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public JwtAuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DomainException("Email already in use");
        }

        WorkPolicy policy = workPolicyRepository.findById(request.workPolicyId())
                .orElseThrow(() -> new DomainException("Work Policy not found"));

        var user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                UserRole.EMPLOYEE,
                null,
                policy
        );

        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return new JwtAuthenticationResponse(jwtToken, 86400000L);
    }

    public JwtAuthenticationResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new DomainException("User not found"));

        var jwtToken = jwtService.generateToken(user);
        return new JwtAuthenticationResponse(jwtToken, 86400000L);
    }
}