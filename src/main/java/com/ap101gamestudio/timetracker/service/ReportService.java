package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.exceptions.DomainException;
import com.ap101gamestudio.timetracker.model.*;
import com.ap101gamestudio.timetracker.model.enums.RecordType;
import com.ap101gamestudio.timetracker.model.enums.UserRole;
import com.ap101gamestudio.timetracker.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
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
import java.util.Comparator;
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
    private final MessageSource messageSource;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final int COLUMN_COUNT = 12;

    public ReportService(
            TimeRecordRepository recordRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            SpecialDateRepository specialDateRepository,
            MessageSource messageSource
    ) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.specialDateRepository = specialDateRepository;
        this.messageSource = messageSource;
    }

    public byte[] generateMonthlyTimesheet(String authenticatedEmail, UUID userId, UUID workspaceId, int year, int month, String localeString) {
        validateManagerAccess(authenticatedEmail, workspaceId);

        User user = getUser(userId);
        Workspace workspace = getWorkspace(workspaceId);
        WorkspaceMembership membership = getMembership(userId, workspaceId);
        YearMonth yearMonth = YearMonth.of(year, month);
        Locale locale = localeString != null ? Locale.forLanguageTag(localeString) : Locale.forLanguageTag("pt-BR");

        List<TimeRecord> validRecords = getValidRecords(userId, workspaceId, yearMonth);
        Map<LocalDate, List<TimeRecord>> recordsByDay = groupRecordsByDay(validRecords);
        List<SpecialDate> monthHolidays = getHolidays(workspaceId, yearMonth);

        return buildExcelWorkbook(user, workspace, membership, yearMonth, recordsByDay, monthHolidays, locale);
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

    private byte[] buildExcelWorkbook(User user, Workspace workspace, WorkspaceMembership membership, YearMonth yearMonth, Map<LocalDate, List<TimeRecord>> recordsByDay, List<SpecialDate> monthHolidays, Locale locale) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(yearMonth.format(DateTimeFormatter.ofPattern("MM-yyyy")));
            ReportStyles styles = createStyles(workbook);

            configureSheetLayout(sheet);
            createDocumentHeader(sheet, user, workspace, yearMonth, styles, locale);
            createTableHeader(sheet, styles, locale);

            long totalWorkedMinutesMonth = fillMonthRows(sheet, yearMonth, membership, recordsByDay, monthHolidays, styles, locale);

            createDocumentFooter(sheet, sheet.getLastRowNum(), totalWorkedMinutesMonth, user.getFullName(), styles, locale);
            adjustColumnWidths(sheet);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new DomainException("error.generating.excel");
        }
    }

    private void configureSheetLayout(Sheet sheet) {
        sheet.createFreezePane(0, 5);
        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);
        sheet.setAutobreaks(true);

        sheet.setMargin(PageMargin.LEFT, 0.2);
        sheet.setMargin(PageMargin.RIGHT, 0.2);
        sheet.setMargin(PageMargin.TOP, 0.25);
        sheet.setMargin(PageMargin.BOTTOM, 0.25);

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 1);
    }

    private long fillMonthRows(Sheet sheet, YearMonth yearMonth, WorkspaceMembership membership, Map<LocalDate, List<TimeRecord>> recordsByDay, List<SpecialDate> monthHolidays, ReportStyles styles, Locale locale) {
        int rowIndex = 5;
        long totalWorkedMinutesMonth = 0;
        long weeklyWorkedMinutes = 0;
        int weekStartRow = rowIndex;

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate currentDate = yearMonth.atDay(day);
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(16);

            DailyRowResult dailyResult = processDailyRow(row, currentDate, membership, recordsByDay, monthHolidays, styles, locale);
            totalWorkedMinutesMonth += dailyResult.workedMinutes();
            weeklyWorkedMinutes += dailyResult.workedMinutes();

            boolean weekClosed = currentDate.getDayOfWeek() == DayOfWeek.SUNDAY || day == yearMonth.lengthOfMonth();
            if (weekClosed) {
                String weeklyTotal = formatMinutesToHHMM(weeklyWorkedMinutes);

                if (weekStartRow < rowIndex) {
                    sheet.addMergedRegion(new CellRangeAddress(weekStartRow, rowIndex, 9, 9));
                    Row anchorRow = sheet.getRow(weekStartRow);
                    if (anchorRow != null) {
                        createCell(anchorRow, 9, weeklyTotal, styles.weeklyTotalStyle());
                    }

                    for (int i = weekStartRow + 1; i <= rowIndex; i++) {
                        Row fillRow = sheet.getRow(i);
                        if (fillRow != null && fillRow.getCell(9) == null) {
                            createCell(fillRow, 9, "", styles.weeklyTotalStyle());
                        }
                    }
                } else {
                    createCell(row, 9, weeklyTotal, styles.weeklyTotalStyle());
                }

                weeklyWorkedMinutes = 0;
                weekStartRow = rowIndex + 1;
            } else {
                createCell(row, 9, "", styles.dataStyle());
            }

            rowIndex++;
        }

        return totalWorkedMinutesMonth;
    }

    private DailyRowResult processDailyRow(Row row, LocalDate currentDate, WorkspaceMembership membership, Map<LocalDate, List<TimeRecord>> recordsByDay, List<SpecialDate> monthHolidays, ReportStyles styles, Locale locale) {
        String dayName = currentDate.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

        Optional<SpecialDate> specialDate = findMatchingSpecialDate(monthHolidays, currentDate);

        boolean isWorkingDay = isWorkingDay(currentDate.getDayOfWeek(), membership.getWorkPolicy());
        CellStyle baseDataStyle = styles.dataStyle();

        if (specialDate.isPresent() && isFullDayHoliday(specialDate.get())) {
            String holidayLabel = messageSource.getMessage("report.holiday", null, locale);
            String holidayJustification = normalizeDescription(specialDate.get().getDescription());
            fillExceptionRow(row, currentDate, dayName, holidayLabel, holidayJustification, styles.weekendStyle());
            return new DailyRowResult(0, styles.weekendStyle());
        }

        if (!isWorkingDay) {
            String weekendLabel = messageSource.getMessage("report.weekend", null, locale);
            fillExceptionRow(row, currentDate, dayName, weekendLabel, weekendLabel, styles.weekendStyle());
            return new DailyRowResult(0, styles.weekendStyle());
        }

        List<TimeRecord> dailyRecords = recordsByDay.getOrDefault(currentDate, List.of());
        
        String partialHolidayDesc = specialDate
                .filter(sd -> !isFullDayHoliday(sd))
                .map(SpecialDate::getDescription)
                .map(this::normalizeDescription)
                .orElse(null);
        
        long workedMinutes = fillWorkingDayRow(row, currentDate, dayName, dailyRecords, baseDataStyle, styles, partialHolidayDesc, locale);
        return new DailyRowResult(workedMinutes, baseDataStyle);
    }

    private void createDocumentHeader(Sheet sheet, User user, Workspace workspace, YearMonth yearMonth, ReportStyles styles, Locale locale) {
        Row row0 = sheet.createRow(0);
        row0.setHeightInPoints(28);
        Cell titleCell = row0.createCell(0);
        titleCell.setCellValue(messageSource.getMessage("report.title", null, locale));
        titleCell.setCellStyle(styles.titleStyle());
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COLUMN_COUNT - 1));

        Row row1 = sheet.createRow(1);
        row1.setHeightInPoints(20);
        Cell subtitleCell = row1.createCell(0);
        subtitleCell.setCellValue(messageSource.getMessage("report.subtitle", null, locale));
        subtitleCell.setCellStyle(styles.subtitleStyle());
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, COLUMN_COUNT - 1));

        Row row2 = sheet.createRow(2);
        createCell(row2, 0, messageSource.getMessage("report.employee", null, locale), styles.metaLabelStyle());
        createCell(row2, 1, user.getFullName(), styles.metaValueStyle());
        createCell(row2, 6, messageSource.getMessage("report.month_year", null, locale), styles.metaLabelStyle());
        createCell(row2, 7, yearMonth.format(MONTH_YEAR_FORMATTER), styles.metaValueStyle());

        Row row3 = sheet.createRow(3);
        createCell(row3, 0, messageSource.getMessage("report.company", null, locale), styles.metaLabelStyle());
        createCell(row3, 1, workspace.getName(), styles.metaValueStyle());
        createCell(row3, 6, messageSource.getMessage("report.generated_at", null, locale), styles.metaLabelStyle());
        createCell(row3, 7, LocalDate.now().format(DATE_FORMATTER), styles.metaValueStyle());
    }

    private void createTableHeader(Sheet sheet, ReportStyles styles, Locale locale) {
        Row header = sheet.createRow(4);
        header.setHeightInPoints(24);
        String[] columns = {
                messageSource.getMessage("report.header.date", null, locale),
                messageSource.getMessage("report.header.weekday", null, locale),
                messageSource.getMessage("report.header.entry", null, locale),
                messageSource.getMessage("report.header.lunch_start", null, locale),
                messageSource.getMessage("report.header.lunch_end", null, locale),
                messageSource.getMessage("report.header.interval", null, locale),
                messageSource.getMessage("report.header.exit", null, locale),
                messageSource.getMessage("report.header.total_without_interval", null, locale),
                messageSource.getMessage("report.header.worked_hours", null, locale),
                messageSource.getMessage("report.header.weekly_hours", null, locale),
                messageSource.getMessage("report.header.justification", null, locale),
                messageSource.getMessage("report.header.observation", null, locale)
        };

        for (int i = 0; i < columns.length; i++) {
            createCell(header, i, columns[i], styles.headerStyle());
        }
    }

    private void fillExceptionRow(Row row, LocalDate date, String dayName, String observation, String justification, CellStyle style) {
        createCell(row, 0, date.format(DATE_FORMATTER), style);
        createCell(row, 1, dayName, style);

        for (int i = 2; i <= 9; i++) {
            createCell(row, i, "-", style);
        }

        createCell(row, 10, justification, style);
        createCell(row, 11, observation, style);
    }

    private long fillWorkingDayRow(Row row, LocalDate date, String dayName, List<TimeRecord> records, CellStyle style, ReportStyles styles, String partialHolidayDesc, Locale locale) {
        createCell(row, 0, date.format(DATE_FORMATTER), style);
        createCell(row, 1, dayName, style);

        Optional<TimeRecord> entry = records.stream().filter(r -> r.getRecordType() == RecordType.ENTRY).findFirst();
        Optional<TimeRecord> pauseStart = records.stream().filter(r -> r.getRecordType() == RecordType.PAUSE_START).findFirst();
        Optional<TimeRecord> pauseEnd = records.stream()
                .filter(r -> r.getRecordType() == RecordType.PAUSE_END)
                .max(Comparator.comparing(TimeRecord::getRegisteredAt));
        Optional<TimeRecord> exit = records.stream()
                .filter(r -> r.getRecordType() == RecordType.EXIT)
                .max(Comparator.comparing(TimeRecord::getRegisteredAt));

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

        long totalShiftMinutes = 0;
        long workedMinutes = 0;
        if (entry.isPresent() && exit.isPresent()) {
            totalShiftMinutes = Duration.between(entry.get().getRegisteredAt(), exit.get().getRegisteredAt()).toMinutes();
            workedMinutes = totalShiftMinutes - pauseMinutes;
        }

        createCell(row, 7, totalShiftMinutes > 0 ? formatMinutesToHHMM(totalShiftMinutes) : "", style);
        createCell(row, 8, workedMinutes > 0 ? formatMinutesToHHMM(workedMinutes) : "", style);

        String justification;
        String absenceStr = messageSource.getMessage("report.absence", null, locale);
        String partialStr = messageSource.getMessage("report.partial_holiday", null, locale);
        String observation = records.isEmpty() ? absenceStr : "";

        if (partialHolidayDesc != null) {
            justification = partialHolidayDesc;
            observation = observation.isEmpty() ? partialStr : observation + " - " + partialStr;
        } else {
            justification = records.stream()
                    .map(TimeRecord::getJustification)
                    .filter(j -> j != null && !j.isBlank())
                    .findFirst()
                    .orElse("");
        }
        
        createCell(row, 10, justification, styles.justificationStyle());
        createCell(row, 11, observation, style);

        return workedMinutes;
    }

    private Optional<SpecialDate> findMatchingSpecialDate(List<SpecialDate> specialDates, LocalDate date) {
        return specialDates.stream()
                .filter(sd -> sd.getDate().equals(date)
                        || (sd.isRecurring()
                        && sd.getDate().getMonth() == date.getMonth()
                        && sd.getDate().getDayOfMonth() == date.getDayOfMonth()))
                .findFirst();
    }

    private boolean isFullDayHoliday(SpecialDate specialDate) {
        Double multiplier = specialDate.getWorkloadMultiplier();
        return multiplier == null || multiplier <= 0.0;
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim().toUpperCase();
    }

    private void createDocumentFooter(Sheet sheet, int lastRowIndex, long totalMinutes, String userName, ReportStyles styles, Locale locale) {
        int rowIndex = lastRowIndex + 1;

        Row totalRow = sheet.createRow(rowIndex++);
        totalRow.setHeightInPoints(28);

        for (int i = 0; i < 7; i++) {
            createCell(totalRow, i, "", styles.totalBandStyle());
        }
        sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 6));

        createCell(totalRow, 7, messageSource.getMessage("report.monthly_total", null, locale), styles.totalLabelStyle());
        createCell(totalRow, 8, formatMinutesToHHMM(totalMinutes), styles.totalValueStyle());
        createCell(totalRow, 9, "", styles.totalBorderStyle());
        createCell(totalRow, 10, "", styles.totalBorderStyle());
        createCell(totalRow, 11, "", styles.totalBorderStyle());

        rowIndex += 2;

        Row signatureLine = sheet.createRow(rowIndex++);
        signatureLine.setHeightInPoints(14);
        createCell(signatureLine, 1, "____________________________________________________", styles.signatureStyle());
        sheet.addMergedRegion(new CellRangeAddress(signatureLine.getRowNum(), signatureLine.getRowNum(), 1, COLUMN_COUNT - 1));

        Row signatureLabel = sheet.createRow(rowIndex++);
        signatureLabel.setHeightInPoints(13);
        createCell(signatureLabel, 1, messageSource.getMessage("report.employee_signature", null, locale), styles.signatureStyle());
        sheet.addMergedRegion(new CellRangeAddress(signatureLabel.getRowNum(), signatureLabel.getRowNum(), 1, COLUMN_COUNT - 1));

        Row employeeLabel = sheet.createRow(rowIndex);
        employeeLabel.setHeightInPoints(13);
        createCell(employeeLabel, 1, userName, styles.signatureStyle());
        sheet.addMergedRegion(new CellRangeAddress(employeeLabel.getRowNum(), employeeLabel.getRowNum(), 1, COLUMN_COUNT - 1));

        rowIndex += 1;

        Row noteRow = sheet.createRow(rowIndex);
        createCell(noteRow, 0, messageSource.getMessage("report.note", null, locale), styles.noteStyle());
        sheet.addMergedRegion(new CellRangeAddress(noteRow.getRowNum(), noteRow.getRowNum(), 0, 10));
    }

    private void adjustColumnWidths(Sheet sheet) {
        int[] widths = {
                15, 17, 9, 11, 12, 11, 15, 12, 12, 12, 47, 21
        };

        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        
        sheet.getRow(4).setHeightInPoints(28);
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
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleFont.setFontHeightInPoints((short) 16);

        Font subtitleFont = workbook.createFont();
        subtitleFont.setColor(IndexedColors.WHITE.getIndex());
        subtitleFont.setFontHeightInPoints((short) 11);

        Font boldWhiteFont = workbook.createFont();
        boldWhiteFont.setBold(true);
        boldWhiteFont.setColor(IndexedColors.WHITE.getIndex());

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle subtitleStyle = workbook.createCellStyle();
        subtitleStyle.setFont(subtitleFont);
        subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
        subtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        subtitleStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        subtitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle metaLabelStyle = workbook.createCellStyle();
        metaLabelStyle.setFont(boldFont);
        metaLabelStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        metaLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle metaValueStyle = workbook.createCellStyle();
        metaValueStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        metaValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(boldWhiteFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setWrapText(true);

        CellStyle dataStyleEven = workbook.createCellStyle();
        dataStyleEven.setBorderBottom(BorderStyle.THIN);
        dataStyleEven.setBorderTop(BorderStyle.THIN);
        dataStyleEven.setBorderLeft(BorderStyle.THIN);
        dataStyleEven.setBorderRight(BorderStyle.THIN);
        dataStyleEven.setAlignment(HorizontalAlignment.CENTER);
        dataStyleEven.setVerticalAlignment(VerticalAlignment.TOP);
        dataStyleEven.setWrapText(true);

        CellStyle dataStyleOdd = workbook.createCellStyle();
        dataStyleOdd.cloneStyleFrom(dataStyleEven);
        dataStyleOdd.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        dataStyleOdd.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setWrapText(false);

        CellStyle justificationStyle = workbook.createCellStyle();
        justificationStyle.cloneStyleFrom(dataStyle);
        justificationStyle.setAlignment(HorizontalAlignment.LEFT);
        justificationStyle.setIndention((short) 2);
        justificationStyle.setWrapText(true);

        CellStyle weekendStyle = workbook.createCellStyle();
        weekendStyle.cloneStyleFrom(dataStyle);
        weekendStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        weekendStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle weeklyTotalStyle = workbook.createCellStyle();
        weeklyTotalStyle.cloneStyleFrom(dataStyle);
        weeklyTotalStyle.setFont(boldWhiteFont);
        weeklyTotalStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        weeklyTotalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        weeklyTotalStyle.setAlignment(HorizontalAlignment.CENTER);
        weeklyTotalStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle totalBorderStyle = workbook.createCellStyle();
        totalBorderStyle.setBorderBottom(BorderStyle.THIN);
        totalBorderStyle.setBorderTop(BorderStyle.THIN);
        totalBorderStyle.setBorderLeft(BorderStyle.THIN);
        totalBorderStyle.setBorderRight(BorderStyle.THIN);
        totalBorderStyle.setAlignment(HorizontalAlignment.CENTER);
        totalBorderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        totalBorderStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        totalBorderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle totalLabelStyle = workbook.createCellStyle();
        totalLabelStyle.cloneStyleFrom(headerStyle);
        totalLabelStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle totalBandStyle = workbook.createCellStyle();
        totalBandStyle.cloneStyleFrom(totalLabelStyle);

        CellStyle totalValueStyle = workbook.createCellStyle();
        totalValueStyle.cloneStyleFrom(weeklyTotalStyle);
        totalValueStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        totalValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalValueStyle.setFont(boldFont);
        totalValueStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle signatureStyle = workbook.createCellStyle();
        signatureStyle.setAlignment(HorizontalAlignment.CENTER);

        Font noteFont = workbook.createFont();
        noteFont.setFontHeightInPoints((short) 8);

        CellStyle noteStyle = workbook.createCellStyle();
        noteStyle.setFont(noteFont);
        noteStyle.setAlignment(HorizontalAlignment.LEFT);

        return new ReportStyles(
                titleStyle,
                subtitleStyle,
                metaLabelStyle,
                metaValueStyle,
                headerStyle,
                dataStyleEven,
                dataStyleOdd,
                dataStyle,
                justificationStyle,
                weekendStyle,
                weeklyTotalStyle,
                totalBandStyle,
                totalBorderStyle,
                totalLabelStyle,
                totalValueStyle,
                signatureStyle,
                noteStyle
        );
    }

    private record ReportStyles(
            CellStyle titleStyle,
            CellStyle subtitleStyle,
            CellStyle metaLabelStyle,
            CellStyle metaValueStyle,
            CellStyle headerStyle,
            CellStyle dataStyleEven,
            CellStyle dataStyleOdd,
            CellStyle dataStyle,
            CellStyle justificationStyle,
            CellStyle weekendStyle,
            CellStyle weeklyTotalStyle,
            CellStyle totalBandStyle,
            CellStyle totalBorderStyle,
            CellStyle totalLabelStyle,
            CellStyle totalValueStyle,
            CellStyle signatureStyle,
            CellStyle noteStyle
    ) {}

    private record DailyRowResult(long workedMinutes, CellStyle baseStyle) {}
}