package com.ap101gamestudio.timetracker.service;

import com.ap101gamestudio.timetracker.model.enums.RecordType;
import com.ap101gamestudio.timetracker.model.TimeRecord;
import com.ap101gamestudio.timetracker.model.User;
import com.ap101gamestudio.timetracker.repository.TimeRecordRepository;
import com.ap101gamestudio.timetracker.repository.UserRepository;
import com.ap101gamestudio.timetracker.exceptions.DomainException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final TimeRecordRepository recordRepository;
    private final UserRepository userRepository;

    public ReportService(TimeRecordRepository recordRepository, UserRepository userRepository) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
    }

    public byte[] generateMonthlyTimesheet(UUID userId, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found"));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<TimeRecord> records = recordRepository.findByUserIdAndRegisteredAtBetween(userId, start, end);
        Map<LocalDate, List<TimeRecord>> recordsByDay = records.stream()
                .collect(Collectors.groupingBy(r -> r.getRegisteredAt().toLocalDate()));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Timesheet - " + month + "-" + year);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle weekendStyle = createWeekendStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            createHeaderRow(sheet, headerStyle);

            int rowIndex = 1;
            for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
                LocalDate currentDate = yearMonth.atDay(day);
                Row row = sheet.createRow(rowIndex++);

                createCell(row, 0, currentDate.toString(), dataStyle);
                createCell(row, 1, currentDate.getDayOfWeek().name(), dataStyle);

                boolean isWeekend = currentDate.getDayOfWeek().getValue() >= 6;
                if (isWeekend) {
                    row.setRowStyle(weekendStyle);
                    for (int i = 0; i < 8; i++) {
                        Cell cell = row.getCell(i);
                        if (cell == null) cell = row.createCell(i);
                        cell.setCellStyle(weekendStyle);
                    }
                    continue;
                }

                List<TimeRecord> dailyRecords = recordsByDay.getOrDefault(currentDate, List.of());
                fillDailyData(row, dailyRecords, dataStyle);
            }

            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new DomainException("Error generating Excel file");
        }
    }

    private void fillDailyData(Row row, List<TimeRecord> records, CellStyle style) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        records.stream().filter(r -> r.getRecordType() == RecordType.ENTRY)
                .findFirst().ifPresent(r -> createCell(row, 2, r.getRegisteredAt().format(timeFormatter), style));

        records.stream().filter(r -> r.getRecordType() == RecordType.LUNCH_START)
                .findFirst().ifPresent(r -> createCell(row, 3, r.getRegisteredAt().format(timeFormatter), style));

        records.stream().filter(r -> r.getRecordType() == RecordType.LUNCH_END)
                .findFirst().ifPresent(r -> createCell(row, 4, r.getRegisteredAt().format(timeFormatter), style));

        records.stream().filter(r -> r.getRecordType() == RecordType.EXIT)
                .reduce((first, second) -> second).ifPresent(r -> createCell(row, 5, r.getRegisteredAt().format(timeFormatter), style));

        // Coluna de Total (simplificada para o exemplo)
        createCell(row, 6, "Calculated via Strategy", style);
    }

    private void createHeaderRow(Sheet sheet, CellStyle style) {
        Row header = sheet.createRow(0);
        String[] columns = {"Data", "Dia da Semana", "Entrada", "Saída Almoço", "Volta Almoço", "Saída", "Total Horas", "Observação"};

        for (int i = 0; i < columns.length; i++) {
            createCell(header, i, columns[i], style);
        }
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createWeekendStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}