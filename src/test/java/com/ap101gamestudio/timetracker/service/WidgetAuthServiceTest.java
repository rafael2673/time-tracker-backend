package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.WidgetLoginRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.ApiKey;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.WorkPolicy;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.repository.ApiKeyRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkPolicyRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.security.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

@ExtendWith(MockitoExtension.class)
class WidgetAuthServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkPolicyRepository workPolicyRepository;

    @Mock
    private WorkspaceMembershipRepository membershipRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private WidgetAuthService widgetAuthService;

    @Test
    void shouldThrowExceptionForInvalidApiKey() {
        WidgetLoginRequest request = new WidgetLoginRequest("invalid_key", "test@test.com", "Name");
        Mockito.when(apiKeyRepository.findByKeyAndActiveTrue("invalid_key")).thenReturn(Optional.empty());

        Assertions.assertThrows(BadCredentialsException.class, () -> widgetAuthService.authenticateFromWidget(request));
    }

    @Test
    void shouldThrowExceptionIfApiKeyHasNoWorkspace() {
        ApiKey apiKey = new ApiKey();
        apiKey.setKey("valid_key");
        apiKey.setWorkspace(null);

        WidgetLoginRequest request = new WidgetLoginRequest("valid_key", "test@test.com", "Name");
        Mockito.when(apiKeyRepository.findByKeyAndActiveTrue("valid_key")).thenReturn(Optional.of(apiKey));

        Assertions.assertThrows(DomainException.class, () -> widgetAuthService.authenticateFromWidget(request));
    }

    @Test
    void shouldAuthenticateAndReturnTokenForValidRequest() {
        Workspace workspace = new Workspace("Office", -5.0, -35.0, 100);
        ReflectionTestUtils.setField(workspace, "id", UUID.randomUUID());

        ApiKey apiKey = new ApiKey();
        apiKey.setKey("valid_key");
        apiKey.setWorkspace(workspace);

        User user = new User("test@test.com", "hash", "Name");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

        WorkPolicy defaultPolicy = new WorkPolicy(workspace,"Default", 480, 10, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY");

        WidgetLoginRequest request = new WidgetLoginRequest("valid_key", "test@test.com", "Name");

        Mockito.when(apiKeyRepository.findByKeyAndActiveTrue("valid_key")).thenReturn(Optional.of(apiKey));
        Mockito.when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        Mockito.when(membershipRepository.findByUserIdAndWorkspaceId(any(), any())).thenReturn(Optional.empty());
        Mockito.when(workPolicyRepository.findByWorkspaceId(workspace.getId())).thenReturn(List.of(defaultPolicy));
        Mockito.when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("valid_jwt_token");

        String token = widgetAuthService.authenticateFromWidget(request);

        Assertions.assertEquals("valid_jwt_token", token);
        Mockito.verify(membershipRepository, Mockito.times(1)).save(any());
    }
}