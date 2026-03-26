package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.model.ExportJob;
import com.ap101gamestudio.timetracker.model.WorkspaceMembership;
import com.ap101gamestudio.timetracker.model.enums.ExportJobStatus;
import com.ap101gamestudio.timetracker.repository.ExportJobRepository;
import com.ap101gamestudio.timetracker.repository.WorkspaceMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class ReportWorkerService {

    private final ExportJobRepository jobRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final ReportService reportService;

    public void processBulkExport(ExportJob job, int year, int month, String locale) {
        iniciarProcessamento(job);

        try {
            List<WorkspaceMembership> members = membershipRepository.findByWorkspaceId(job.getWorkspaceId());

            if (members.isEmpty()) {
                finalizarVazio(job);
                return;
            }

            Path tempZipFile = Files.createTempFile("timetracker_export_" + job.getId().toString(), ".zip");
            processarMembrosEGerarZip(tempZipFile, job, members, year, month, locale);
            finalizarComSucesso(job, tempZipFile);

        } catch (Exception e) {
            finalizarComErro(job);
        }
    }

    private void iniciarProcessamento(ExportJob job) {
        job.setStatus(ExportJobStatus.PROCESSING);
        jobRepository.save(job);
    }

    private void finalizarVazio(ExportJob job) {
        job.setStatus(ExportJobStatus.COMPLETED);
        job.setProgress(100);
        jobRepository.save(job);
    }

    private void processarMembrosEGerarZip(Path tempZipFile, ExportJob job, List<WorkspaceMembership> members, int year, int month, String locale) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            int total = members.size();
            int current = 0;

            for (WorkspaceMembership member : members) {
                adicionarRelatorioAoZip(zos, job, member, year, month, locale);
                current++;
                atualizarProgresso(job, current, total);
            }
        }
    }

    private void adicionarRelatorioAoZip(ZipOutputStream zos, ExportJob job, WorkspaceMembership member, int year, int month, String locale) throws IOException {
        byte[] excelBytes = reportService.generateMonthlyTimesheet(
                job.getRequesterEmail(),
                member.getUser().getId(),
                job.getWorkspaceId(),
                year,
                month,
                locale
        );

        String safeName = limparNomeArquivo(member.getUser().getFullName());
        ZipEntry entry = new ZipEntry("Folha_Ponto_" + safeName + "_" + month + "_" + year + ".xlsx");
        zos.putNextEntry(entry);
        zos.write(excelBytes);
        zos.closeEntry();
    }

    private String limparNomeArquivo(String nome) {
        if (nome == null) return "Usuario";
        return nome.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private void atualizarProgresso(ExportJob job, int current, int total) {
        job.setProgress((int) ((current * 100.0) / total));
        jobRepository.save(job);
    }

    private void finalizarComSucesso(ExportJob job, Path tempZipFile) {
        job.setFilePath(tempZipFile.toAbsolutePath().toString());
        job.setStatus(ExportJobStatus.COMPLETED);
        job.setProgress(100);
        jobRepository.save(job);
    }

    private void finalizarComErro(ExportJob job) {
        job.setStatus(ExportJobStatus.FAILED);
        jobRepository.save(job);
    }
}