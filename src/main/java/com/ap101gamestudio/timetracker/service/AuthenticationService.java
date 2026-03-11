package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.JwtAuthenticationResponse;
import com.ap101gamestudio.timetracker.dto.LoginRequest;
import com.ap101gamestudio.timetracker.dto.RegisterRequest;
import com.ap101gamestudio.timetracker.dto.GenerateLinkCodeResponse;
import com.ap101gamestudio.timetracker.dto.LinkAccountRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.LinkCode;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.repository.LinkCodeRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final LinkCodeRepository linkCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            LinkCodeRepository linkCodeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.linkCodeRepository = linkCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public JwtAuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DomainException("error.email.already.in.use");
        }

        var user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
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
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        var jwtToken = jwtService.generateToken(user);
        return new JwtAuthenticationResponse(jwtToken, 86400000L);
    }

    @Transactional
    public GenerateLinkCodeResponse generateLinkCode(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        LinkCode linkCode = new LinkCode(code, user, expiresAt);
        linkCodeRepository.save(linkCode);

        return new GenerateLinkCodeResponse(code, 600);
    }

    @Transactional
    public JwtAuthenticationResponse linkAccount(LinkAccountRequest request) {
        LinkCode linkCode = linkCodeRepository.findByCode(request.code())
                .orElseThrow(() -> new DomainException("Invalid linking code"));

        if (linkCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            linkCodeRepository.delete(linkCode);
            throw new DomainException("error.expired.linking.code");
        }

        User user = linkCode.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        linkCodeRepository.delete(linkCode);

        var jwtToken = jwtService.generateToken(user);
        return new JwtAuthenticationResponse(jwtToken, 86400000L);
    }
}