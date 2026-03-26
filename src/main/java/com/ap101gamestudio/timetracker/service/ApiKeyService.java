package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.ApiKey;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.ApiKeyRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import com.ap101gamestudio.timetracker.utils.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    @Value("${api.key.secret}")
    private String apiSecret;
    private final ApiKeyRepository apiKeyRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public String getActiveKeyMasked(String email, UUID workspaceId) {
        validateManagerAccess(email, workspaceId);

        return apiKeyRepository.findByWorkspaceIdAndActiveTrue(workspaceId)
                .map(ApiKey::getMaskedKey)
                .orElse(null);
    }

    @Transactional
    public String generateNewKey(String email, UUID workspaceId) {
        validateManagerAccess(email, workspaceId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new DomainException("error.workspace.not_found"));

        List<ApiKey> activeKeys = apiKeyRepository.findAllByWorkspaceIdAndActiveTrue(workspaceId);
        for (ApiKey key : activeKeys) {
            key.setActive(false);
        }
        apiKeyRepository.saveAll(activeKeys);

        String rawKey = "tt_live_" + UUID.randomUUID().toString().replace("-", "");
        String sha256HexKey = HashUtils.hashWithHmacSha256(rawKey, apiSecret);
        String maskedKey = maskApiKey(rawKey);

        ApiKey newKey = new ApiKey();
        newKey.setKey(sha256HexKey);
        newKey.setMaskedKey(maskedKey);
        newKey.setWorkspace(workspace);
        newKey.setActive(true);

        apiKeyRepository.save(newKey);

        return rawKey;
    }

    private void validateManagerAccess(String email, UUID workspaceId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));

        if (membership.getRole() == UserRole.EMPLOYEE) {
            throw new DomainException("error.permission.denied");
        }
    }

    private String maskApiKey(String fullKey) {
        if (fullKey == null || fullKey.length() <= 12) return fullKey;
        String suffix = fullKey.substring(fullKey.length() - 4);
        return "tt_live_****************" + suffix;
    }
}