package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.PageResponse;
import com.ap101gamestudio.timetracker.dto.BrasilApiHolidayResponse;
import com.ap101gamestudio.timetracker.dto.SpecialDateRequest;
import com.ap101gamestudio.timetracker.dto.SpecialDateResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.SpecialDate;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.SpecialDateRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SpecialDateService {

    private final SpecialDateRepository specialDateRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public SpecialDateService(SpecialDateRepository specialDateRepository, WorkspaceRepository workspaceRepository, UserRepository userRepository, WorkspaceMembershipRepository membershipRepository) {
        this.specialDateRepository = specialDateRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    private void validateManagerAccess(String email, UUID workspaceId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new DomainException("error.user.not_found"));
        WorkspaceMembership membership = membershipRepository.findByUserIdAndWorkspaceId(user.getId(), workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));
        if (membership.getRole() == UserRole.EMPLOYEE) {
            throw new DomainException("error.permission.denied");
        }
    }

    public SpecialDateResponse create(String email, UUID workspaceId, SpecialDateRequest request) {
        validateManagerAccess(email, workspaceId);
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(() -> new DomainException("error.workspace.not_found"));
        SpecialDate specialDate = new SpecialDate(workspace, request.date(), request.description(), request.workloadMultiplier(), request.isRecurring());
        SpecialDate saved = specialDateRepository.save(specialDate);
        return new SpecialDateResponse(saved.getId(), saved.getDate(), saved.getDescription(), saved.getWorkloadMultiplier(), saved.isRecurring());
    }

    public SpecialDateResponse update(String email, UUID workspaceId, UUID id, SpecialDateRequest request) {
        validateManagerAccess(email, workspaceId);
        SpecialDate specialDate = specialDateRepository.findById(id).orElseThrow(() -> new DomainException("error.record.not_found"));
        if (!specialDate.getWorkspace().getId().equals(workspaceId)) throw new DomainException("error.permission.denied");

        specialDate.setDate(request.date());
        specialDate.setDescription(request.description());
        specialDate.setWorkloadMultiplier(request.workloadMultiplier());
        specialDate.setRecurring(request.isRecurring());

        SpecialDate saved = specialDateRepository.save(specialDate);
        return new SpecialDateResponse(saved.getId(), saved.getDate(), saved.getDescription(), saved.getWorkloadMultiplier(), saved.isRecurring());
    }

    public PageResponse<SpecialDateResponse> getByYear(UUID workspaceId, int year, String search, int page, int size) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").ascending());

        Page<SpecialDate> result = specialDateRepository.findRelevantDatesWithSearch(workspaceId, start, end, search, pageable);

        List<SpecialDateResponse> content = result.getContent().stream()
                .map(sd -> new SpecialDateResponse(sd.getId(), sd.getDate(), sd.getDescription(), sd.getWorkloadMultiplier(), sd.isRecurring()))
                .toList();

        return new PageResponse<>(content, result.getTotalPages(), result.getTotalElements(), result.getNumber());
    }

    public void delete(String email, UUID workspaceId, UUID id) {
        validateManagerAccess(email, workspaceId);
        SpecialDate specialDate = specialDateRepository.findById(id).orElseThrow(() -> new DomainException("error.record.not_found"));
        if (!specialDate.getWorkspace().getId().equals(workspaceId)) throw new DomainException("error.permission.denied");
        specialDateRepository.delete(specialDate);
    }

    public void importNationalHolidays(String email, UUID workspaceId, int year) {
        validateManagerAccess(email, workspaceId);
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow(() -> new DomainException("error.workspace.not_found"));

        String url = "https://brasilapi.com.br/api/feriados/v1/" + year;
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

        try {
            BrasilApiHolidayResponse[] holidays = restTemplate.getForObject(url, BrasilApiHolidayResponse[].class);
            if (holidays != null) {
                LocalDate start = LocalDate.of(year, 1, 1);
                LocalDate end = LocalDate.of(year, 12, 31);
                List<SpecialDate> existingDates = specialDateRepository.findRelevantDates(workspaceId, start, end);

                for (BrasilApiHolidayResponse h : holidays) {
                    LocalDate hDate = LocalDate.parse(h.date());
                    boolean exists = existingDates.stream().anyMatch(sd ->
                            sd.getDate().equals(hDate) ||
                                    (sd.isRecurring() && sd.getDate().getMonth() == hDate.getMonth() && sd.getDate().getDayOfMonth() == hDate.getDayOfMonth())
                    );
                    if (!exists) {
                        SpecialDate sd = new SpecialDate(workspace, hDate, h.name(), 0.0, false);
                        specialDateRepository.save(sd);
                    }
                }
            }
        } catch (Exception e) {
            throw new DomainException("error.import.failed");
        }
    }
}