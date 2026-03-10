package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.dto.WidgetLoginRequest;
import com.ap101gamestudio.timetracker.repository.ApiKeyRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkPolicyRepository;
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
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String authenticateFromWidget(WidgetLoginRequest request) {
        var apiKey = apiKeyRepository.findByKeyAndActiveTrue(request.apiKey())
                .orElseThrow(() -> new BadCredentialsException("API Key inválida ou inativa"));

        var user = userRepository.findByEmail(request.email())
                .orElseGet(() -> createSilentUser(request));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("workspaceId", apiKey.getWorkspace().getId().toString());

        return jwtService.generateToken(extraClaims, user);
    }

    private User createSilentUser(WidgetLoginRequest request) {
        WorkPolicy defaultPolicy = workPolicyRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Nenhuma WorkPolicy encontrada no banco de dados para vincular ao novo usuário."));
        String email = request.email();
        String name = request.name() != null ? request.name() : request.email().split("@")[0];
        String password = passwordEncoder.encode(UUID.randomUUID().toString());
        var user = new User(email, name, password, UserRole.EMPLOYEE, null, defaultPolicy);
        return userRepository.save(user);
    }
}