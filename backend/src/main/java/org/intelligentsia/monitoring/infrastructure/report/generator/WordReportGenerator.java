package org.intelligentsia.monitoring.infrastructure.report.generator;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.intelligentsia.monitoring.infrastructure.report.model.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class WordReportGenerator implements ReportGenerator {

    private static final String NAVY = "1F3045";
    private static final String GOLD = "C9A84C";
    private static final String MUTED = "667383";
    private static final String PALE = "F5F7FA";
    private static final String PALE_BLUE = "E6EEF6";
    private static final String BORDER = "DDE2E7";

    @Override
    public String format() {
        return "WORD";
    }

    @Override
    public void generate(ReportDocument report, Path target) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            configurePage(document);
            document.getProperties().getCoreProperties().setTitle(report.getMetadata().getReportTitle());
            document.getProperties().getCoreProperties().setCreator(report.getMetadata().getApplicationName());
            addHeader(document, report);

            heading(document, "Synthèse", 14);
            addKpis(document, report.getKpis());

            boolean commentWritten = false;
            for (ReportSection section : report.getSections()) {
                if (!commentWritten && "Validation".equalsIgnoreCase(section.getTitle())) {
                    addComment(document, report.getMetadata().getComment());
                    commentWritten = true;
                }
                heading(document, section.getTitle(), 12);
                if (notBlank(section.getDescription())) {
                    paragraph(document, section.getDescription(), 9, MUTED, false);
                }
                if (section.getItems() != null && !section.getItems().isEmpty()) {
                    addItems(document, section.getItems());
                }
                if (section.getTable() != null) addReportTable(document, section.getTable());
            }
            if (!commentWritten) addComment(document, report.getMetadata().getComment());

            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            XWPFParagraph footerText = footer.createParagraph();
            footerText.setAlignment(ParagraphAlignment.CENTER);
            run(footerText, "Intelligentsia Monitoring  ·  Généré le "
                    + report.getMetadata().getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), 8, MUTED, false);

            Files.createDirectories(target.toAbsolutePath().getParent());
            try (var output = Files.newOutputStream(target)) {
                document.write(output);
            }
        }
    }

    private void configurePage(XWPFDocument document) {
        var section = document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = section.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(11906));
        pageSize.setH(BigInteger.valueOf(16838));
        pageSize.setOrient(STPageOrientation.PORTRAIT);
        CTPageMar margins = section.addNewPgMar();
        margins.setTop(BigInteger.valueOf(680));
        margins.setBottom(BigInteger.valueOf(680));
        margins.setLeft(BigInteger.valueOf(720));
        margins.setRight(BigInteger.valueOf(720));
    }

    private void addHeader(XWPFDocument document, ReportDocument report) {
        XWPFTable banner = document.createTable(1, 1);
        banner.setWidth("100%");
        XWPFTableCell bannerCell = banner.getRow(0).getCell(0);
        bannerCell.setColor(PALE_BLUE);
        bannerCell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        String bannerText = report.getMetadata().getApplicationName();
        if (notBlank(report.getMetadata().getInstitutionName())) {
            bannerText += "  ·  Institution : " + report.getMetadata().getInstitutionName();
        }
        setCellText(bannerCell, bannerText, 13, NAVY, true);

        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        title.setSpacingBefore(140);
        title.setSpacingAfter(40);
        run(title, report.getMetadata().getReportTitle(), 20, NAVY, true);
        if (notBlank(report.getMetadata().getSubtitle())) {
            XWPFParagraph subtitle = document.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            subtitle.setSpacingAfter(120);
            run(subtitle, report.getMetadata().getSubtitle(), 10, MUTED, false);
        }

        XWPFTable metadata = document.createTable(2, 2);
        metadata.setWidth("100%");
        metadata.setCellMargins(90, 140, 90, 140);
        String[][] lines = {
                {"Période", value(report.getMetadata().getPeriod()), "Source", value(report.getMetadata().getSource())},
                {"Généré le", value(report.getMetadata().getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))), "", ""},
        };
        for (int row = 0; row < lines.length; row++) {
            for (int pair = 0; pair < 2; pair++) {
                XWPFTableCell cell = metadata.getRow(row).getCell(pair);
                cell.setColor(PALE);
                if (!lines[row][pair * 2].isBlank()) {
                    setCellText(cell, lines[row][pair * 2] + "  ·  " + lines[row][pair * 2 + 1], 9, NAVY, false);
                }
            }
        }
    }

    private void addComment(XWPFDocument document, String text) {
        if (!notBlank(text)) return;
        heading(document, "Commentaire de la journée", 11);
        XWPFTable comment = document.createTable(1, 1);
        comment.setWidth("100%");
        comment.setCellMargins(110, 150, 110, 150);
        comment.getRow(0).getCell(0).setColor("FFF9E9");
        setCellText(comment.getRow(0).getCell(0), text, 9, NAVY, false);
    }

    private void addKpis(XWPFDocument document, List<ReportKpi> kpis) {
        if (kpis == null || kpis.isEmpty()) return;
        XWPFTable table = document.createTable(kpis.size() + 1, 3);
        table.setWidth("100%");
        table.setCellMargins(90, 140, 90, 140);
        String[] headers = {"Indicateur", "Valeur", "Description"};
        for (int column = 0; column < headers.length; column++) {
            styleCell(table.getRow(0).getCell(column), headers[column], 9, NAVY, true, PALE_BLUE);
        }
        for (int row = 0; row < kpis.size(); row++) {
            ReportKpi kpi = kpis.get(row);
            String fill = row % 2 == 0 ? "FFFFFF" : PALE;
            styleCell(table.getRow(row + 1).getCell(0), kpi.getLabel(), 9, NAVY, true, fill);
            styleCell(table.getRow(row + 1).getCell(1), kpi.getValue(), 10, NAVY, true, fill);
            styleCell(table.getRow(row + 1).getCell(2), kpi.getDescription(), 9, MUTED, false, fill);
        }
    }

    private void addItems(XWPFDocument document, List<Map<String, Object>> items) {
        XWPFTable table = document.createTable(items.size() + 1, 2);
        table.setWidth("100%");
        table.setCellMargins(75, 120, 75, 120);
        styleCell(table.getRow(0).getCell(0), "Indicateur", 9, NAVY, true, PALE_BLUE);
        styleCell(table.getRow(0).getCell(1), "Valeur", 9, NAVY, true, PALE_BLUE);
        for (int row = 0; row < items.size(); row++) {
            Map<String, Object> item = items.get(row);
            String fill = row % 2 == 0 ? "FFFFFF" : PALE;
            styleCell(table.getRow(row + 1).getCell(0), value(item.get("label")), 9, NAVY, false, fill);
            styleCell(table.getRow(row + 1).getCell(1), value(item.get("total")), 9, NAVY, true, fill);
        }
    }

    private void addReportTable(XWPFDocument document, ReportTable table) {
        if (notBlank(table.getTitle())) heading(document, table.getTitle(), 10);
        List<ReportColumn> columns = table.getColumns();
        if (columns == null || columns.isEmpty()) return;
        if ((table.isRecordLayout() || columns.size() > 8) && table.getRows() != null && !table.getRows().isEmpty()) {
            int index = 1;
            for (var row : table.getRows()) {
                XWPFTable record = document.createTable(columns.size() + 1, 2);
                record.setWidth("100%");
                record.setCellMargins(75, 120, 75, 120);
                for (var line : record.getRows()) line.setCantSplitRow(true);
                String color = ReportDisplay.riskColor(row);
                styleCell(record.getRow(0).getCell(0), ReportDisplay.recordTitle(row, index++), 10, "FFFFFF", true, color);
                styleCell(record.getRow(0).getCell(1), "Détail de l’alerte", 9, "FFFFFF", true, color);
                for (int c = 0; c < columns.size(); c++) {
                    styleCell(record.getRow(c + 1).getCell(0), columns.get(c).getLabel(), 9, MUTED, true, PALE_BLUE);
                    styleCell(record.getRow(c + 1).getCell(1), value(row.get(columns.get(c).getKey())), 9, NAVY, false, c % 2 == 0 ? "FFFFFF" : PALE);
                }
                paragraph(document, " ", 3, MUTED, false);
            }
            return;
        }
        int rowCount = Math.max(2, (table.getRows() == null ? 0 : table.getRows().size()) + 1);
        XWPFTable wordTable = document.createTable(rowCount, columns.size());
        wordTable.setWidth("100%");
        wordTable.getRow(0).setRepeatHeader(true);
        for (var row : wordTable.getRows()) row.setCantSplitRow(true);
        wordTable.setCellMargins(70, 90, 70, 90);
        for (int column = 0; column < columns.size(); column++) {
            styleCell(wordTable.getRow(0).getCell(column), columns.get(column).getLabel(), 8, "FFFFFF", true, NAVY);
        }
        List<Map<String, Object>> rows = table.getRows();
        if (rows == null || rows.isEmpty()) {
            styleCell(wordTable.getRow(1).getCell(0), "Aucune donnée", 8, MUTED, false, PALE);
            for (int column = 1; column < columns.size(); column++) {
                styleCell(wordTable.getRow(1).getCell(column), "", 8, NAVY, false, PALE);
            }
            return;
        }
        for (int row = 0; row < rows.size(); row++) {
            String fill = row % 2 == 0 ? "FFFFFF" : PALE;
            for (int column = 0; column < columns.size(); column++) {
                String key = columns.get(column).getKey();
                Object value = rows.get(row).get(key);
                String cellText = value(value);
                styleCell(wordTable.getRow(row + 1).getCell(column), cellText, 8, NAVY, false, fill);
            }
        }
    }

    private void heading(XWPFDocument document, String text, int size) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.getCTP().addNewPPr();
        paragraph.setKeepNext(true);
        paragraph.getCTP().getPPr().addNewShd().setFill("F6EED4");
        paragraph.setSpacingBefore(180);
        paragraph.setSpacingAfter(70);
        run(paragraph, text, size, NAVY, true);
    }

    private void paragraph(XWPFDocument document, String text, int size, String color, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(70);
        run(paragraph, text, size, color, bold);
    }

    private void styleCell(XWPFTableCell cell, String text, int size, String color, boolean bold, String fill) {
        cell.setColor(fill);
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        setCellText(cell, text, size, color, bold);
    }

    private void setCellText(XWPFTableCell cell, String text, int size, String color, boolean bold) {
        if (!cell.getParagraphs().isEmpty()) cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingAfter(0);
        paragraph.setSpacingBefore(0);
        run(paragraph, value(text), size, color, bold);
    }

    private void run(XWPFParagraph paragraph, String text, int size, String color, boolean bold) {
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Aptos");
        run.setFontSize(size);
        run.setColor(color);
        run.setBold(bold);
        run.setText(value(text));
    }

    private String value(Object value) {
        return ReportDisplay.value(value);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
