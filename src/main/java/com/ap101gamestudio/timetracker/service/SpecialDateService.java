package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.dto.PageResponse;
import com.ap101gamestudio.timetracker.dto.FeriadosApiResponse;
import com.ap101gamestudio.timetracker.dto.SpecialDateRequest;
import com.ap101gamestudio.timetracker.dto.SpecialDateResponse;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.SpecialDate;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.model.Workspace;
import com.ap101gamestudio.timetracker.model.WorkspaceHolidaySync;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.SpecialDateRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceHolidaySyncRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceRepository;
import com.ap101gamestudio.timetracker.utils.DateFilterUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class SpecialDateService {

    private final SpecialDateRepository specialDateRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final WorkspaceHolidaySyncRepository syncRepository;
    private final RestTemplate restTemplate;

    @Value("${app.feriados-api.token}")
    private String apiToken;

    public SpecialDateService(SpecialDateRepository specialDateRepository,
                              WorkspaceRepository workspaceRepository,
                              UserRepository userRepository,
                              WorkspaceMembershipRepository membershipRepository,
                              WorkspaceHolidaySyncRepository syncRepository) {
        this.specialDateRepository = specialDateRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.syncRepository = syncRepository;
        this.restTemplate = new RestTemplate();
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

    @Transactional
    public PageResponse<SpecialDateResponse> getByYear(UUID workspaceId, int year, String search, String exactDate, int page, int size) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new DomainException("error.workspace.not_found"));

        garantirSincronizacaoAnual(workspace, year);

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").ascending());

        Integer[] parsed = DateFilterUtils.parseDateFilter(exactDate);
        Page<SpecialDate> result = specialDateRepository.findRelevantDatesWithSearch(workspaceId, start, end, search, parsed[0], parsed[1], parsed[2], pageable);

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

    private void garantirSincronizacaoAnual(Workspace workspace, int year) {
        if (syncRepository.findByWorkspaceIdAndYear(workspace.getId(), year).isPresent()) {
            return;
        }

        List<FeriadosApiResponse> feriadosEncontrados = buscarFeriadosNaApiExterna(workspace, year);
        mesclarESalvarNovasDatas(workspace, feriadosEncontrados, year);
        syncRepository.save(new WorkspaceHolidaySync(workspace, year));
    }

    private List<FeriadosApiResponse> buscarFeriadosNaApiExterna(Workspace workspace, int year) {
        List<FeriadosApiResponse> todosFeriados = new ArrayList<>();

        todosFeriados.addAll(requisitarApi("/nacionais", year));

        if (workspace.getStateUf() != null && !workspace.getStateUf().isBlank()) {
            todosFeriados.addAll(requisitarApi("/estado/" + workspace.getStateUf(), year));
        }

        if (workspace.getIbgeCode() != null && !workspace.getIbgeCode().isBlank()) {
            todosFeriados.addAll(requisitarApi("/cidade/" + workspace.getIbgeCode(), year));
        }

        return todosFeriados;
    }

    private List<FeriadosApiResponse> requisitarApi(String endpoint, int year) {
        String url = "https://feriadosapi.com/api/v1/feriados" + endpoint + "?ano=" + year;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<FeriadosApiResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<FeriadosApiResponse>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private void mesclarESalvarNovasDatas(Workspace workspace, List<FeriadosApiResponse> feriadosApi, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<SpecialDate> existentes = specialDateRepository.findRelevantDates(workspace.getId(), start, end);

        for (FeriadosApiResponse f : feriadosApi) {
            LocalDate dataFeriado = LocalDate.parse(f.date());

            boolean jaExiste = existentes.stream().anyMatch(sd ->
                    sd.getDate().equals(dataFeriado) ||
                            (sd.isRecurring() && sd.getDate().getMonth() == dataFeriado.getMonth() && sd.getDate().getDayOfMonth() == dataFeriado.getDayOfMonth())
            );

            if (!jaExiste) {
                specialDateRepository.save(new SpecialDate(workspace, dataFeriado, f.name(), 0.0, false));
            }
        }
    }
}