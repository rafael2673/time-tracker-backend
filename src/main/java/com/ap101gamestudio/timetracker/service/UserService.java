package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.GenerateLinkCodeResponse;
import com.ap101gamestudio.timetracker.dto.UpdateProfileRequest;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.LinkCode;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.repository.LinkCodeRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LinkCodeRepository linkCodeRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkspaceService workspaceService;

    public UserService(
            UserRepository userRepository,
            LinkCodeRepository linkCodeRepository,
            WorkspaceMembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            WorkspaceService workspaceService
    ) {
        this.userRepository = userRepository;
        this.linkCodeRepository = linkCodeRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.workspaceService = workspaceService;
    }

    @Transactional
    public void updateProfile(String authenticatedEmail, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        if (request.recoveryEmail() != null && !request.recoveryEmail().isBlank()) {
            userRepository.findByRecoveryEmail(request.recoveryEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(user.getId())) {
                            throw new DomainException("error.email.already.in.use");
                        }
                    });
            user.setRecoveryEmail(request.recoveryEmail());
        }

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }

        userRepository.save(user);
    }

    @Transactional
    public void updatePassword(String authenticatedEmail, String newPassword) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        if (user.isHasWebPassword()) {
            throw new DomainException("error.password.already_set");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setHasWebPassword(true);

        userRepository.save(user);
    }

    @Transactional
    public GenerateLinkCodeResponse generateCodeForEmployee(String managerEmail, UUID employeeId, UUID workspaceId) {
        workspaceService.validateManagerOrAdmin(managerEmail, workspaceId);

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new DomainException("error.user.not_found"));

        membershipRepository.findByUserIdAndWorkspaceId(employee.getId(), workspaceId)
                .orElseThrow(employeeNotInWorkspaceException::new);

        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        LinkCode linkCode = new LinkCode(code, employee, expiresAt);
        linkCodeRepository.save(linkCode);

        return new GenerateLinkCodeResponse(code, 86400);
    }

    private static class employeeNotInWorkspaceException extends DomainException {
        public employeeNotInWorkspaceException() {
            super("error.employee.not_in_workspace");
        }
    }
}