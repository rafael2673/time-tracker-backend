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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public String getActiveKey(String email, UUID workspaceId) {
        validateManagerAccess(email, workspaceId);

        return apiKeyRepository.findByWorkspaceIdAndActiveTrue(workspaceId)
                .map(ApiKey::getKey)
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

        String newKeyValue = "tt_live_" + UUID.randomUUID().toString().replace("-", "");
        
        ApiKey newKey = new ApiKey();
        newKey.setKey(newKeyValue);
        newKey.setWorkspace(workspace);
        newKey.setActive(true);

        apiKeyRepository.save(newKey);

        return newKeyValue;
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
}