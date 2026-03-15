package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.EmployeeLeaveRequest;
import com.ap101gamestudio.timetracker.dto.EmployeeLeaveResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmployeeLeaveService {
    private final EmployeeLeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public EmployeeLeaveService(EmployeeLeaveRepository leaveRepository, UserRepository userRepository, WorkspaceRepository workspaceRepository, WorkspaceMembershipRepository membershipRepository) {
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
    }

    private void validateManagerAccess(String email, UUID workspaceId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId).orElseThrow(() -> new DomainException("error.permission.denied"));
        if (membership.getRole() == UserRole.EMPLOYEE) throw new DomainException("error.permission.denied");
    }

    public EmployeeLeaveResponse create(String email, UUID workspaceId, UUID employeeId, EmployeeLeaveRequest request) {
        validateManagerAccess(email, workspaceId);
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(() -> new DomainException("error.workspace.not_found"));
        User employee = userRepository.findById(employeeId).orElseThrow(() -> new DomainException("error.user.not_found"));

        EmployeeLeave leave = new EmployeeLeave(workspace, employee, request.startDate(), request.endDate(), request.reason());
        EmployeeLeave saved = leaveRepository.save(leave);
        return new EmployeeLeaveResponse(saved.getId(), saved.getStartDate(), saved.getEndDate(), saved.getReason());
    }

    public EmployeeLeaveResponse update(String email, UUID workspaceId, UUID leaveId, EmployeeLeaveRequest request) {
        validateManagerAccess(email, workspaceId);
        EmployeeLeave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new DomainException("error.record.not_found"));

        if (!leave.getWorkspace().getId().equals(workspaceId)) {
            throw new DomainException("error.permission.denied");
        }

        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setReason(request.reason());

        EmployeeLeave saved = leaveRepository.save(leave);
        return new EmployeeLeaveResponse(saved.getId(), saved.getStartDate(), saved.getEndDate(), saved.getReason());
    }

    public List<EmployeeLeaveResponse> getByEmployee(String email, UUID workspaceId, UUID employeeId) {
        validateManagerAccess(email, workspaceId);
        return leaveRepository.findByWorkspaceIdAndUserIdOrderByStartDateDesc(workspaceId, employeeId).stream()
                .map(l -> new EmployeeLeaveResponse(l.getId(), l.getStartDate(), l.getEndDate(), l.getReason())).toList();
    }

    public void delete(String email, UUID workspaceId, UUID leaveId) {
        validateManagerAccess(email, workspaceId);
        EmployeeLeave leave = leaveRepository.findById(leaveId).orElseThrow(() -> new DomainException("error.record.not_found"));
        if (!leave.getWorkspace().getId().equals(workspaceId)) throw new DomainException("error.permission.denied");
        leaveRepository.delete(leave);
    }
}