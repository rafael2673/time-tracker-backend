package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.RecordType;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final TimeRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final SpecialDateRepository specialDateRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = new Locale("pt", "BR");

    public ReportService(
            TimeRecordRepository recordRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            SpecialDateRepository specialDateRepository
    ) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.specialDateRepository = specialDateRepository;
    }

    public byte[] generateMonthlyTimesheet(String authenticatedEmail, UUID userId, UUID workspaceId, int year, int month) {
        validateManagerAccess(authenticatedEmail, workspaceId);

        User user = getUser(userId);
        Workspace workspace = getWorkspace(workspaceId);
        WorkspaceMembership membership = getMembership(userId, workspaceId);
        YearMonth yearMonth = YearMonth.of(year, month);

        List<TimeRecord> validRecords = getValidRecords(userId, workspaceId, yearMonth);
        Map<LocalDate, List<TimeRecord>> recordsByDay = groupRecordsByDay(validRecords);
        List<SpecialDate> monthHolidays = getHolidays(workspaceId, yearMonth);

        return buildExcelWorkbook(user, workspace, membership, yearMonth, recordsByDay, monthHolidays);
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

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("error.user.not_found"));
    }

    private Workspace getWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new DomainException("error.workspace.not_found"));
    }

    private WorkspaceMembership getMembership(UUID userId, UUID workspaceId) {
        return membershipRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new DomainException("error.permission.denied"));
    }

    private List<TimeRecord> getValidRecords(UUID userId, UUID workspaceId, YearMonth yearMonth) {
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<TimeRecord> allRecords = recordRepository.findByUserIdAndWorkspaceIdAndRegisteredAtBetween(userId, workspaceId, start, end);

        Set<UUID> supersededIds = allRecords.stream()
                .filter(r -> r.getEditedFrom() != null)
                .map(r -> r.getEditedFrom().getId())
                .collect(Collectors.toSet());

        return allRecords.stream()
                .filter(r -> !r.isPendingApprovation())
                .filter(r -> !supersededIds.contains(r.getId()))
                .toList();
    }

    private Map<LocalDate, List<TimeRecord>> groupRecordsByDay(List<TimeRecord> records) {
        return records.stream().collect(Collectors.groupingBy(r -> r.getRegisteredAt().toLocalDate()));
    }

    private List<SpecialDate> getHolidays(UUID workspaceId, YearMonth yearMonth) {
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return specialDateRepository.findRelevantDates(workspaceId, start, end);
    }

    private byte[] buildExcelWorkbook(User user, Workspace workspace, WorkspaceMembership membership, YearMonth yearMonth, Map<LocalDate, List<TimeRecord>> recordsByDay, List<SpecialDate> monthHolidays) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(yearMonth.format(DateTimeFormatter.ofPattern("MM-yyyy")));
            ReportStyles styles = createStyles(workbook);

            createDocumentHeader(sheet, user, workspace, yearMonth, styles);
            createTableHeader(sheet, styles);

            long totalWorkedMinutesMonth = fillMonthRows(sheet, yearMonth, membership, recordsByDay, monthHolidays, styles);

            createDocumentFooter(sheet, sheet.getLastRowNum(), totalWorkedMinutesMonth, user.getFullName(), styles);
            adjustColumnWidths(sheet);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new DomainException("error.generating.excel");
        }
    }

    private long fillMonthRows(Sheet sheet, YearMonth yearMonth, WorkspaceMembership membership, Map<LocalDate, List<TimeRecord>> recordsByDay, List<SpecialDate> monthHolidays, ReportStyles styles) {
        int rowIndex = 5;
        long totalWorkedMinutesMonth = 0;

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate currentDate = yearMonth.atDay(day);
            Row row = sheet.createRow(rowIndex++);
            totalWorkedMinutesMonth += processDailyRow(row, currentDate, membership, recordsByDay, monthHolidays, styles);
        }

        return totalWorkedMinutesMonth;
    }

    private long processDailyRow(Row row, LocalDate currentDate, WorkspaceMembership membership, Map<LocalDate, List<TimeRecord>> recordsByDay, List<SpecialDate> monthHolidays, ReportStyles styles) {
        String dayName = currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);

        Optional<SpecialDate> holiday = monthHolidays.stream()
                .filter(h -> h.getDate().equals(currentDate))
                .findFirst();

        boolean isWorkingDay = isWorkingDay(currentDate.getDayOfWeek(), membership.getWorkPolicy());

        if (holiday.isPresent()) {
            fillExceptionRow(row, currentDate, dayName, "FERIADO: " + holiday.get().getDescription().toUpperCase(), styles.weekendStyle());
            return 0;
        }

        if (!isWorkingDay) {
            fillExceptionRow(row, currentDate, dayName, "FINAL DE SEMANA", styles.weekendStyle());
            return 0;
        }

        List<TimeRecord> dailyRecords = recordsByDay.getOrDefault(currentDate, List.of());
        return fillWorkingDayRow(row, currentDate, dayName, dailyRecords, styles.dataStyle());
    }

    private void createDocumentHeader(Sheet sheet, User user, Workspace workspace, YearMonth yearMonth, ReportStyles styles) {
        Row row0 = sheet.createRow(0);
        Cell titleCell = row0.createCell(0);
        titleCell.setCellValue("FOLHA DE PONTO INDIVIDUAL");
        titleCell.setCellStyle(styles.titleStyle());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

        Row row2 = sheet.createRow(2);
        createCell(row2, 0, "Colaborador:", styles.boldStyle());
        createCell(row2, 1, user.getFullName(), styles.normalStyle());
        createCell(row2, 6, "Mês/Ano:", styles.boldStyle());
        createCell(row2, 7, yearMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")), styles.normalStyle());

        Row row3 = sheet.createRow(3);
        createCell(row3, 0, "Empresa:", styles.boldStyle());
        createCell(row3, 1, workspace.getName(), styles.normalStyle());
    }

    private void createTableHeader(Sheet sheet, ReportStyles styles) {
        Row header = sheet.createRow(4);
        String[] columns = {"Data", "Dia da Semana", "Entrada", "Saída Almoço", "Retorno Almoço", "Duração Almoço", "Saída", "Total Trabalhado", "Observação"};

        for (int i = 0; i < columns.length; i++) {
            createCell(header, i, columns[i], styles.headerStyle());
        }
    }

    private void fillExceptionRow(Row row, LocalDate date, String dayName, String justification, CellStyle style) {
        createCell(row, 0, date.format(DATE_FORMATTER), style);
        createCell(row, 1, dayName, style);

        for (int i = 2; i <= 7; i++) {
            createCell(row, i, "-", style);
        }
        createCell(row, 8, justification, style);
    }

    private long fillWorkingDayRow(Row row, LocalDate date, String dayName, List<TimeRecord> records, CellStyle style) {
        createCell(row, 0, date.format(DATE_FORMATTER), style);
        createCell(row, 1, dayName, style);

        Optional<TimeRecord> entry = records.stream().filter(r -> r.getRecordType() == RecordType.ENTRY).findFirst();
        Optional<TimeRecord> pauseStart = records.stream().filter(r -> r.getRecordType() == RecordType.PAUSE_START).findFirst();
        Optional<TimeRecord> pauseEnd = records.stream().filter(r -> r.getRecordType() == RecordType.PAUSE_END).reduce((first, second) -> second);
        Optional<TimeRecord> exit = records.stream().filter(r -> r.getRecordType() == RecordType.EXIT).reduce((first, second) -> second);

        createCell(row, 2, entry.map(r -> r.getRegisteredAt().format(TIME_FORMATTER)).orElse(""), style);
        createCell(row, 3, pauseStart.map(r -> r.getRegisteredAt().format(TIME_FORMATTER)).orElse(""), style);
        createCell(row, 4, pauseEnd.map(r -> r.getRegisteredAt().format(TIME_FORMATTER)).orElse(""), style);

        long pauseMinutes = 0;
        if (pauseStart.isPresent() && pauseEnd.isPresent()) {
            pauseMinutes = Duration.between(pauseStart.get().getRegisteredAt(), pauseEnd.get().getRegisteredAt()).toMinutes();
            createCell(row, 5, formatMinutesToHHMM(pauseMinutes), style);
        } else {
            createCell(row, 5, "", style);
        }

        createCell(row, 6, exit.map(r -> r.getRegisteredAt().format(TIME_FORMATTER)).orElse(""), style);

        long workedMinutes = 0;
        if (entry.isPresent() && exit.isPresent()) {
            long totalShift = Duration.between(entry.get().getRegisteredAt(), exit.get().getRegisteredAt()).toMinutes();
            workedMinutes = totalShift - pauseMinutes;
            createCell(row, 7, formatMinutesToHHMM(workedMinutes), style);
        } else {
            createCell(row, 7, "", style);
        }

        String observation = records.isEmpty() ? "FALTA" : "";
        createCell(row, 8, observation, style);

        return workedMinutes;
    }

    private void createDocumentFooter(Sheet sheet, int lastRowIndex, long totalMinutes, String userName, ReportStyles styles) {
        int rowIndex = lastRowIndex + 2;

        Row totalRow = sheet.createRow(rowIndex++);
        createCell(totalRow, 6, "Total do Mês:", styles.boldStyle());
        createCell(totalRow, 7, formatMinutesToHHMM(totalMinutes), styles.boldStyle());

        rowIndex += 3;

        Row signatureRow = sheet.createRow(rowIndex++);
        createCell(signatureRow, 1, "________________________________________________", styles.normalStyle());
        createCell(signatureRow, 5, "________________________________________________", styles.normalStyle());

        Row labelRow = sheet.createRow(rowIndex);
        createCell(labelRow, 1, "Assinatura do Colaborador: " + userName, styles.normalStyle());
        createCell(labelRow, 5, "Assinatura do Responsável", styles.normalStyle());
    }

    private void adjustColumnWidths(Sheet sheet) {
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.setColumnWidth(8, 256 * 25);
    }

    private boolean isWorkingDay(DayOfWeek dayOfWeek, WorkPolicy policy) {
        if (policy == null || policy.getWorkingDays() == null) return true;
        return policy.getWorkingDays().contains(dayOfWeek.name());
    }

    private String formatMinutesToHHMM(long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private ReportStyles createStyles(Workbook workbook) {
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        CellStyle normalStyle = workbook.createCellStyle();

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(boldFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle weekendStyle = workbook.createCellStyle();
        weekendStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        weekendStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        weekendStyle.setBorderBottom(BorderStyle.THIN);
        weekendStyle.setBorderTop(BorderStyle.THIN);
        weekendStyle.setBorderLeft(BorderStyle.THIN);
        weekendStyle.setBorderRight(BorderStyle.THIN);
        weekendStyle.setAlignment(HorizontalAlignment.CENTER);

        return new ReportStyles(titleStyle, boldStyle, normalStyle, headerStyle, dataStyle, weekendStyle);
    }

    private record ReportStyles(
            CellStyle titleStyle,
            CellStyle boldStyle,
            CellStyle normalStyle,
            CellStyle headerStyle,
            CellStyle dataStyle,
            CellStyle weekendStyle
    ) {}
}