package org.intelligentsia.monitoring.infrastructure.report.generator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.intelligentsia.monitoring.infrastructure.report.model.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class ExcelReportGenerator implements ReportGenerator {

    @Override
    public String format() {
        return "EXCEL";
    }

    @Override
    public void generate(ReportDocument report, Path target) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);
            createSummarySheet(workbook, report, styles);
            if (report.isGroupedDetails()) createGroupedDetailSheet(workbook, report, styles);
            else createDetailsSheets(workbook, report, styles);
            Files.createDirectories(target.toAbsolutePath().getParent());
            try (var output = Files.newOutputStream(target)) {
                workbook.write(output);
            }
        }
    }

    private void createSummarySheet(XSSFWorkbook workbook, ReportDocument report, Styles styles) {
        Sheet sheet = workbook.createSheet("Synthèse");
        sheet.setColumnWidth(0, 34 * 256);
        sheet.setColumnWidth(1, 24 * 256);
        sheet.setColumnWidth(2, 76 * 256);
        sheet.setPrintGridlines(false);
        configurePrint(sheet);

        int rowIndex = 0;
        mergeRow(sheet, rowIndex++, 0, 2, value(report.getMetadata().getApplicationName()), styles.banner, 34);
        mergeRow(sheet, rowIndex++, 0, 2, "Institution  ·  " + value(report.getMetadata().getInstitutionName()), styles.metaValue, 25);
        mergeRow(sheet, rowIndex++, 0, 2, value(report.getMetadata().getReportTitle()), styles.title, 30);
        mergeRow(sheet, rowIndex++, 0, 2, value(report.getMetadata().getSubtitle()), styles.subtitle, 22);
        addMetadataRow(sheet, rowIndex++, "Période", report.getMetadata().getPeriod(), "Source", report.getMetadata().getSource(), styles);
        addMetadataRow(sheet, rowIndex++, "Généré le", report.getMetadata().getGeneratedAt(), "", "", styles);
        rowIndex++;
        Row kpiHeader = sheet.createRow(rowIndex++);
        writeCell(kpiHeader, 0, "Indicateur", styles.tableHeader);
        writeCell(kpiHeader, 1, "Valeur", styles.tableHeader);
        writeCell(kpiHeader, 2, "Description", styles.tableHeader);
        List<ReportKpi> kpis = report.getKpis() == null ? List.of() : report.getKpis();
        for (int index = 0; index < kpis.size(); index++) {
            ReportKpi kpi = kpis.get(index);
            Row row = sheet.createRow(rowIndex++);
            CellStyle style = index % 2 == 0 ? styles.body : styles.alternate;
            writeCell(row, 0, kpi.getLabel(), style);
            writeCell(row, 1, kpi.getValue(), styles.value);
            writeCell(row, 2, kpi.getDescription(), style);
            row.setHeight((short) 360);
        }

        for (ReportSection section : report.isGroupedDetails() ? List.<ReportSection>of() : report.getSections()) {
            if ("Validation".equalsIgnoreCase(section.getTitle()) && notBlank(report.getMetadata().getComment())) {
                rowIndex++;
                mergeRow(sheet, rowIndex++, 0, 2, "Commentaire de la journée", styles.section, 24);
                mergeRow(sheet, rowIndex++, 0, 2, report.getMetadata().getComment(), styles.comment, 48);
            }
            rowIndex++;
            mergeRow(sheet, rowIndex++, 0, 2, value(section.getTitle()), styles.section, 24);
            if (notBlank(section.getDescription())) {
                mergeRow(sheet, rowIndex++, 0, 2, section.getDescription(), styles.description, 30);
            }
            if (section.getItems() != null) {
                for (Map<String, Object> item : section.getItems()) {
                    Row row = sheet.createRow(rowIndex++);
                    writeCell(row, 0, value(item.get("label")), styles.body);
                    writeCell(row, 1, value(item.get("total")), styles.value);
                    writeCell(row, 2, "", styles.body);
                }
            }
            if (section.getTable() != null && "Validation".equalsIgnoreCase(section.getTitle())) {
                ReportTable validation = section.getTable();
                List<ReportColumn> columns = validation.getColumns() == null ? List.of() : validation.getColumns();
                if (!columns.isEmpty()) {
                    Row header = sheet.createRow(rowIndex++);
                    for (int column = 0; column < columns.size(); column++) {
                        writeCell(header, column, columns.get(column).getLabel(), styles.tableHeader);
                    }
                    List<Map<String, Object>> rows = validation.getRows() == null ? List.of() : validation.getRows();
                    for (Map<String, Object> data : rows) {
                        Row row = sheet.createRow(rowIndex++);
                        row.setHeight((short) 900);
                        for (int column = 0; column < columns.size(); column++) {
                            Object cell = data.get(columns.get(column).getKey());
                            writeCell(row, column, cell == null ? "" : cell.toString(), styles.body);
                        }
                    }
                }
            }
        }

        rowIndex++;
        addMetadataRow(sheet, rowIndex, "Généré le", report.getMetadata().getGeneratedAt(), "Source", report.getMetadata().getSource(), styles);
        sheet.createFreezePane(0, 8);
    }

    private void addMetadataRow(Sheet sheet, int index, String leftLabel, Object leftValue,
                                String rightLabel, Object rightValue, Styles styles) {
        Row row = sheet.createRow(index);
        writeCell(row, 0, leftLabel, styles.metaLabel);
        writeCell(row, 1, value(leftValue), styles.metaValue);
        String rightMetadata = rightLabel == null || rightLabel.isBlank()
                ? "" : rightLabel + "  ·  " + value(rightValue);
        writeCell(row, 2, rightMetadata, styles.metaValue);
        row.setHeight((short) 330);
    }

    private void createDetailsSheets(XSSFWorkbook workbook, ReportDocument report, Styles styles) {
        List<ReportSection> detailSections = new ArrayList<>(report.getSections());
        detailSections.sort(Comparator.comparingInt(this::detailSheetOrder));
        for (ReportSection section : detailSections) {
            ReportTable table = section.getTable();
            if (table == null || table.getColumns() == null || table.getColumns().isEmpty()) continue;
            if ("Validation".equalsIgnoreCase(section.getTitle())) continue;

            if (table.isRecordLayout() || table.getColumns().size() > 8) {
                createRecordSheet(workbook, section, styles);
                continue;
            }
            Sheet sheet = workbook.createSheet(sanitizeSheetName(section.getTitle(), workbook));
            sheet.setPrintGridlines(false);
            configurePrint(sheet);
            List<ReportColumn> columns = table.getColumns();
            List<Map<String, Object>> rows = table.getRows() == null ? List.of() : table.getRows();
            int headerIndex = 1;
            mergeRow(sheet, 0, 0, columns.size() - 1, value(section.getTitle()), styles.banner, 30);
            if (section.getItems() != null) {
                for (Map<String, Object> item : section.getItems()) {
                    mergeRow(sheet, headerIndex++, 0, columns.size() - 1,
                            value(item.get("label")) + " : " + value(item.get("total")), styles.metaValue, 25);
                }
            }
            mergeRow(sheet, headerIndex++, 0, columns.size() - 1, value(table.getTitle()), styles.section, 25);
            Row header = sheet.createRow(headerIndex);
            for (int column = 0; column < columns.size(); column++) {
                writeCell(header, column, columns.get(column).getLabel(), styles.tableHeader);
                sheet.setColumnWidth(column, estimateWidth(columns.get(column), rows));
            }
            header.setHeight((short) 420);

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Map<String, Object> data = rows.get(rowIndex);
                Row row = sheet.createRow(rowIndex + headerIndex + 1);
                CellStyle style = rowIndex % 2 == 0 ? styles.body : styles.alternate;
                int visualLines = 1;
                for (int column = 0; column < columns.size(); column++) {
                    String key = columns.get(column).getKey();
                    String cellValue = value(data.get(key));
                    writeCell(row, column, cellValue, style);
                    int width = Math.max(12, sheet.getColumnWidth(column) / 256);
                    visualLines = Math.max(visualLines, Math.min(5, (cellValue.length() + width - 1) / width));
                }
                row.setHeight((short) Math.min(1350, Math.max(330, visualLines * 280)));
            }

            if (!rows.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(headerIndex, rows.size() + headerIndex, 0, columns.size() - 1));
            }
            sheet.createFreezePane(0, headerIndex + 1);
            sheet.setRepeatingRows(new CellRangeAddress(headerIndex, headerIndex, -1, -1));
            sheet.getFooter().setCenter("&A   ·   Page &P / &N");
        }
    }

    private void createRecordSheet(XSSFWorkbook workbook, ReportSection section, Styles styles) {
        Sheet sheet = workbook.createSheet(sanitizeSheetName(section.getTitle(), workbook));
        configurePrint(sheet);
        sheet.setPrintGridlines(false);
        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 70 * 256);
        mergeRow(sheet, 0, 0, 1, section.getTitle(), styles.banner, 30);
        int rowIndex = 1;
        List<Map<String, Object>> records = section.getTable().getRows();
        if (records == null || records.isEmpty()) {
            mergeRow(sheet, rowIndex, 0, 1, "Aucune donnée", styles.body, 24);
        } else {
            for (int index = 0; index < records.size(); index++) {
                mergeRow(sheet, rowIndex++, 0, 1, ReportDisplay.recordTitle(records.get(index), index + 1), styles.recordHeader(workbook, ReportDisplay.riskColor(records.get(index))), 26);
                for (var column : section.getTable().getColumns()) {
                    String content = value(records.get(index).get(column.getKey()));
                    // Excel limits a row's height; split long values into printable continuations.
                    for (int offset = 0; offset < content.length(); offset += 1400) {
                        String part = content.substring(offset, Math.min(offset + 1400, content.length()));
                        Row row = sheet.createRow(rowIndex++);
                        writeCell(row, 0, column.getLabel() + (offset == 0 ? "" : " (suite)"), styles.metaLabel);
                        writeCell(row, 1, part, index % 2 == 0 ? styles.body : styles.alternate);
                        row.setHeightInPoints(Math.max(24, ((part.length() + 65) / 66) * 14));
                    }
                }
                rowIndex++;
            }
        }
        sheet.createFreezePane(0, 1);
        sheet.setRepeatingRows(new CellRangeAddress(0, 0, -1, -1));
        sheet.getFooter().setCenter("&A · Page &P / &N");
    }

    private void createGroupedDetailSheet(XSSFWorkbook workbook, ReportDocument report, Styles styles) {
        Sheet sheet = workbook.createSheet("GABs et mouvements");
        configurePrint(sheet);
        sheet.setPrintGridlines(false);
        int rowIndex = 0;
        mergeRow(sheet, rowIndex++, 0, 4, value(report.getMetadata().getReportTitle()), styles.banner, 34);
        mergeRow(sheet, rowIndex++, 0, 4, value(report.getMetadata().getPeriod()), styles.metaValue, 25);
        int[] widths = {22, 12, 14, 21, 34};
        for (int column = 0; column < widths.length; column++) sheet.setColumnWidth(column, widths[column] * 256);
        for (ReportSection section : report.getSections()) {
            rowIndex++;
            mergeRow(sheet, rowIndex++, 0, 4, section.getTitle(), styles.section, 28);
            for (Map<String, Object> item : section.getItems()) {
                mergeRow(sheet, rowIndex++, 0, 4, value(item.get("label")) + " : " + value(item.get("total")), styles.metaValue, 26);
            }
            var table = section.getTable();
            mergeRow(sheet, rowIndex++, 0, 4, table.getTitle(), styles.description, 24);
            Row header = sheet.createRow(rowIndex++);
            header.setHeightInPoints(30);
            for (int column = 0; column < table.getColumns().size(); column++) {
                writeCell(header, column, table.getColumns().get(column).getLabel(), styles.tableHeader);
            }
            if (table.getRows().isEmpty()) {
                mergeRow(sheet, rowIndex++, 0, 4, "Aucun mouvement correspondant aux filtres", styles.body, 24);
            } else {
                for (int index = 0; index < table.getRows().size(); index++) {
                    Row row = sheet.createRow(rowIndex++);
                    int lines = 1;
                    for (int column = 0; column < table.getColumns().size(); column++) {
                        String content = value(table.getRows().get(index).get(table.getColumns().get(column).getKey()));
                        writeCell(row, column, content, index % 2 == 0 ? styles.body : styles.alternate);
                        lines = Math.max(lines, (content.length() + widths[column] - 1) / widths[column]);
                    }
                    row.setHeightInPoints(Math.max(24, Math.min(409, lines * 14)));
                }
            }
        }
        sheet.createFreezePane(0, 2);
        sheet.setRepeatingRows(new CellRangeAddress(0, 1, -1, -1));
        sheet.getFooter().setCenter("Page &P / &N");
    }

    private int detailSheetOrder(ReportSection section) {
        if (section.getTable() == null || section.getTable().getTitle() == null) return 100;
        return switch (section.getTable().getTitle()) {
            case "Alertes enregistrées" -> 0;
            case "Règles" -> 1;
            case "Cartes et transactions" -> 2;
            case "Traitements" -> 3;
            default -> 10;
        };
    }

    private int estimateWidth(ReportColumn column, List<Map<String, Object>> rows) {
        int width = column.getLabel() == null ? 12 : column.getLabel().length() + 3;
        for (int row = 0; row < Math.min(rows.size(), 100); row++) {
            Object value = rows.get(row).get(column.getKey());
            if (value != null) width = Math.max(width, Math.min(value.toString().length(), 42));
        }
        return Math.min(46, Math.max(14, width)) * 256;
    }

    private void configurePrint(Sheet sheet) {
        PrintSetup setup = sheet.getPrintSetup();
        setup.setLandscape(false);
        setup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        setup.setFitWidth((short) 1);
        setup.setFitHeight((short) 0);
        sheet.setFitToPage(true);
        sheet.setMargin(Sheet.LeftMargin, 0.3);
        sheet.setMargin(Sheet.RightMargin, 0.3);
        sheet.setMargin(Sheet.TopMargin, 0.5);
        sheet.setMargin(Sheet.BottomMargin, 0.5);
    }

    private void mergeRow(Sheet sheet, int rowIndex, int firstColumn, int lastColumn, String text,
                          CellStyle style, int height) {
        Row row = sheet.createRow(rowIndex);
        for (int column = firstColumn; column <= lastColumn; column++) {
            writeCell(row, column, column == firstColumn ? text : "", style);
        }
        if (lastColumn > firstColumn) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
        }
        row.setHeight((short) (height * 20));
    }

    private void writeCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private String sanitizeSheetName(String name, Workbook workbook) {
        String value = name == null || name.isBlank() ? "Détails" : name.replaceAll("[\\\\/*?:\\[\\]]", "_");
        value = value.length() > 31 ? value.substring(0, 31) : value;
        String candidate = value;
        int suffix = 2;
        while (workbook.getSheet(candidate) != null) {
            String postfix = " (" + suffix++ + ")";
            candidate = value.substring(0, Math.min(value.length(), 31 - postfix.length())) + postfix;
        }
        return candidate;
    }

    private String value(Object value) {
        return ReportDisplay.value(value);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static final class Styles {
        private final CellStyle banner;
        private final CellStyle title;
        private final CellStyle subtitle;
        private final CellStyle metaLabel;
        private final CellStyle metaValue;
        private final CellStyle section;
        private final CellStyle description;
        private final CellStyle comment;
        private final CellStyle tableHeader;
        private final CellStyle body;
        private final CellStyle alternate;
        private final CellStyle value;

        private final Map<String, CellStyle> recordHeaders = new java.util.HashMap<>();

        private CellStyle recordHeader(XSSFWorkbook workbook, String color) {
            return recordHeaders.computeIfAbsent(color, key -> style(workbook, key, "FFFFFF", true, 11, true));
        }

        private Styles(XSSFWorkbook workbook) {
            banner = style(workbook, "E6EEF6", "1F3045", true, 16, true);
            title = style(workbook, "EAF0F6", "1F3045", true, 14, true);
            subtitle = style(workbook, "F5F7FA", "667383", false, 10, true);
            metaLabel = style(workbook, "F5F7FA", "667383", true, 9, true);
            metaValue = style(workbook, "FFFFFF", "1F3045", false, 9, true);
            section = style(workbook, "F6EED4", "1F3045", true, 10, true);
            description = style(workbook, "FFF9E9", "536171", false, 9, true);
            comment = style(workbook, "FFF9E9", "1F3045", false, 10, true);
            tableHeader = style(workbook, "1F3045", "FFFFFF", true, 10, true);
            body = style(workbook, "FFFFFF", "1F3045", false, 9, true);
            alternate = style(workbook, "F5F7FA", "1F3045", false, 9, true);
            value = style(workbook, "EAF0F6", "1F3045", true, 9, true);
        }

        private CellStyle style(XSSFWorkbook workbook, String fill, String color, boolean bold, int size, boolean wrap) {
            CellStyle style = workbook.createCellStyle();
            ((org.apache.poi.xssf.usermodel.XSSFCellStyle) style).setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(java.awt.Color.decode("#" + fill), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(wrap);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBorderTop(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBorderLeft(BorderStyle.THIN);
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBorderRight(BorderStyle.THIN);
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            Font font = workbook.createFont();
            font.setFontName("Aptos");
            font.setFontHeightInPoints((short) size);
            font.setBold(bold);
            ((org.apache.poi.xssf.usermodel.XSSFFont) font).setColor(new org.apache.poi.xssf.usermodel.XSSFColor(java.awt.Color.decode("#" + color), null));
            style.setFont(font);
            return style;
        }

    }
}
