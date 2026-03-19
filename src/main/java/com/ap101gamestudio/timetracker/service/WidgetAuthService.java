package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.WidgetLoginRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.ApiKeyRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkPolicyRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WidgetAuthService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final WorkPolicyRepository workPolicyRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String authenticateFromWidget(WidgetLoginRequest request) {
        var apiKey = apiKeyRepository.findByKeyAndActiveTrue(request.apiKey())
                .orElseThrow(() -> new BadCredentialsException("error.api_key.invalid"));

        if (apiKey.getWorkspace() == null) {
            throw new DomainException("error.api_key.no_workspace");
        }

        Workspace workspace = apiKey.getWorkspace();

        var user = userRepository.findByEmail(request.email())
                .orElseGet(() -> createSilentUser(request));

        if (membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspace.getId()).isEmpty()) {

            WorkPolicy defaultPolicy = workPolicyRepository.findByWorkspaceId(workspace.getId())
                    .stream()
                    .findFirst()
                    .orElse(null);

            membershipRepository.save(new WorkspaceMembership(user, workspace, UserRole.EMPLOYEE, defaultPolicy));
        }

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("workspaceId", workspace.getId().toString());

        return jwtService.generateToken(extraClaims, user);
    }

    private User createSilentUser(WidgetLoginRequest request) {
        String email = request.email();
        String name = (request.name() != null && !request.name().isBlank()) ? request.name() : request.email().split("@")[0];
        String password = passwordEncoder.encode(UUID.randomUUID().toString());

        return userRepository.save(new User(email, password, name));
    }
}