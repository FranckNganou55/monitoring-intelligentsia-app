package org.intelligentsia.monitoring.infrastructure.report.generator;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.intelligentsia.monitoring.infrastructure.report.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PortraitReportLayoutTest {
    @TempDir Path directory;
    private ReportDocument report(List<ReportColumn> columns, List<Map<String, Object>> rows) {
        return ReportDocument.builder().metadata(ReportMetadata.builder().applicationName("MonitoringApp")
                .reportTitle("Vérification portrait").generatedAt(LocalDateTime.of(2026,10,12,9,0)).build())
                .kpis(List.of()).sections(List.of(ReportSection.builder().title("Détails")
                        .table(ReportTable.builder().title("Mouvements").columns(columns).rows(rows).build()).build())).build();
    }
    @Test
    void splitsAnOversizedMessageAcrossPagesWithoutClippingText() throws Exception {
        var columns = List.of(ReportColumn.builder().key("date").label("Date").build(),
                ReportColumn.builder().key("previous").label("Statut précédent").build(),
                ReportColumn.builder().key("message").label("Message").build());
        var target = directory.resolve("long.pdf");
        new PdfReportGenerator().generate(report(columns, List.of(Map.of("date", "12/10/2026", "previous", "UP",
                "message", "DEBUT " + "ABCDEFGHIJ".repeat(1200) + " FIN-MOUVEMENT"))), target);
        try (var document = Loader.loadPDF(target.toFile())) {
            assertTrue(document.getNumberOfPages() > 1);
            var stripper = new PDFTextStripper() {
                @Override protected void processTextPosition(TextPosition text) {
                    assertTrue(text.getXDirAdj() >= 35, "Left margin");
                    assertTrue(text.getXDirAdj() + text.getWidthDirAdj() <= 560, "Right margin");
                    assertTrue(text.getYDirAdj() < 809, "Footer boundary");
                    super.processTextPosition(text);
                }
            };
            String content = stripper.getText(document);
            assertTrue(content.contains("DEBUT"));
            assertTrue(content.contains("FIN-MOUVEMENT"));
            for (var page : document.getPages()) assertTrue(page.getMediaBox().getHeight() > page.getMediaBox().getWidth());
        }
    }
    @Test
    void wideTablesBecomeReadablePortraitRecordsWithoutDroppingFields() throws Exception {
        List<ReportColumn> columns = new ArrayList<>();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int field = 0; field < 15; field++) {
            columns.add(ReportColumn.builder().key("field" + field).label("Champ " + field).build());
            row.put("field" + field, "ValeurDistincte" + field);
        }
        var target = directory.resolve("wide.pdf");
        new PdfReportGenerator().generate(report(columns, List.of(row)), target);
        try (var document = Loader.loadPDF(target.toFile())) {
            String content = new PDFTextStripper().getText(document);
            for (Object value : row.values()) assertTrue(content.contains(value.toString()));
            assertTrue(content.contains("Enregistrement 1"));
        }
        var word = directory.resolve("wide.docx");
        new WordReportGenerator().generate(report(columns, List.of(row)), word);
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(java.nio.file.Files.newInputStream(word))) {
            assertEquals(org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation.PORTRAIT,
                    document.getDocument().getBody().getSectPr().getPgSz().getOrient());
            String content = document.getTables().stream().map(table -> table.getText()).reduce("", (a,b) -> a+b);
            for (Object value : row.values()) assertTrue(content.contains(value.toString()));
        }
        var excel = directory.resolve("wide.xlsx");
        new ExcelReportGenerator().generate(report(columns, List.of(row)), excel);
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(java.nio.file.Files.newInputStream(excel))) {
            var sheet = workbook.getSheet("Détails");
            assertNotNull(sheet);
            assertFalse(sheet.getPrintSetup().getLandscape());
            StringBuilder content = new StringBuilder();
            sheet.forEach(record -> record.forEach(cell -> content.append(cell.toString())));
            for (Object value : row.values()) assertTrue(content.toString().contains(value.toString()));
            assertEquals(2, sheet.getRow(2).getPhysicalNumberOfCells());
        }
    }
}
