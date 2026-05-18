package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.JwtAuthenticationResponse;
import com.ap101gamestudio.timetracker.dto.LoginRequest;
import com.ap101gamestudio.timetracker.dto.RegisterRequest;
import com.ap101gamestudio.timetracker.dto.GenerateLinkCodeResponse;
import com.ap101gamestudio.timetracker.dto.LinkAccountRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.LinkCode;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.repository.LinkCodeRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final LinkCodeRepository linkCodeRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            LinkCodeRepository linkCodeRepository,
            WorkspaceMembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.linkCodeRepository = linkCodeRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public JwtAuthenticationResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DomainException("error.email.already.in.use");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
        );

        userRepository.save(user);
        String jwtToken = jwtService.generateStandardUserToken(user, null);
        return new JwtAuthenticationResponse(jwtToken, 86400000L);
    }

    public JwtAuthenticationResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        return generateTokenForUser(user);
    }

    private JwtAuthenticationResponse generateTokenForUser(User user) {
        List<WorkspaceMembership> memberships = membershipRepository.findByUserId(user.getId());

        if (user.isSystemAdmin()) {
            UUID defaultWorkspaceId = memberships.stream()
                    .filter(m -> m.getWorkspace().isActive())
                    .findFirst()
                    .map(membership -> membership.getWorkspace().getId())
                    .orElse(null);

            String jwtToken = jwtService.generateStandardUserToken(user, defaultWorkspaceId);
            return new JwtAuthenticationResponse(jwtToken, 86400000L);
        }

        WorkspaceMembership activeMembership = memberships.stream()
                .filter(m -> m.getWorkspace().isActive())
                .findFirst()
                .orElseThrow(() -> new DomainException("error.workspace.inactive"));

        String jwtToken = jwtService.generateStandardUserToken(user, activeMembership.getWorkspace().getId());
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
                .orElseThrow(() -> new DomainException("error.linking.code.invalid"));

        if (linkCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            linkCodeRepository.delete(linkCode);
            throw new DomainException("error.linking.code.expired");
        }

        User user = linkCode.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        linkCodeRepository.delete(linkCode);

        return generateTokenForUser(user);
    }
}