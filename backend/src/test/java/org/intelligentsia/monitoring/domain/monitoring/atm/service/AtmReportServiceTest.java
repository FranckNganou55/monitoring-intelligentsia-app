package org.intelligentsia.monitoring.domain.monitoring.atm.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.intelligentsia.monitoring.domain.monitoring.atm.dto.*;
import org.intelligentsia.monitoring.infrastructure.report.generator.*;
import org.intelligentsia.monitoring.infrastructure.report.service.ReportExportService;
import org.intelligentsia.monitoring.shared.dto.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AtmReportServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"PDF", "WORD", "EXCEL"})
    void exportsAllFilteredPagesAndTheirMovements(String format) throws Exception {
        var atms = mock(AtmService.class);
        var movements = mock(AtmMovementService.class);
        var exports = new ReportExportService(List.of(new PdfReportGenerator(), new WordReportGenerator(), new ExcelReportGenerator()));
        var service = new AtmReportService(atms, movements, exports);
        var first = terminal(1L, "GAB-001");
        var second = terminal(2L, "GAB-002");
        when(atms.findAll(any(), eq(10L), eq("error"))).thenAnswer(call -> {
            FilterRequest filter = call.getArgument(0);
            assertEquals("Abidjan", filter.getSearch());
            return PageResponse.of(new PageImpl<>(List.of(filter.getPage() == 0 ? first : second), PageRequest.of(filter.getPage(), 1), 2));
        });
        Instant from = Instant.parse("2026-10-01T00:00:00Z");
        when(movements.all(anyLong(), eq(10L), eq("DOWN"), eq(from), isNull())).thenReturn(List.of(
                new AtmMovementDto(1, from, "Interface", "UP", "DOWN", "error", "État dégradé")));
        byte[] bytes = service.export(null, 10L, "Abidjan", "error", "DOWN", from, null, format);
        String text;
        if (format.equals("PDF")) {
            try (var document = Loader.loadPDF(bytes)) {
                for (var page : document.getPages()) assertTrue(page.getMediaBox().getHeight() > page.getMediaBox().getWidth());
                text = new PDFTextStripper().getText(document);
                assertTrue(text.indexOf("État dégradé") < text.indexOf("GAB GAB-002"));
                var directory = java.nio.file.Path.of(System.getProperty("basedir"), "tmp/report-qa");
                java.nio.file.Files.createDirectories(directory);
                java.nio.file.Files.write(directory.resolve("rapport-gabs.pdf"), bytes);
                var renderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    javax.imageio.ImageIO.write(renderer.renderImageWithDPI(page, 110), "png", directory.resolve("page-" + (page + 1) + ".png").toFile());
                }
            }
        } else if (format.equals("WORD")) {
            try (var document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                var pageSize = document.getDocument().getBody().getSectPr().getPgSz();
                assertEquals(org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation.PORTRAIT, pageSize.getOrient());
                String ordered = document.getBodyElements().stream().map(element ->
                        element instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph ? paragraph.getText() :
                        element instanceof org.apache.poi.xwpf.usermodel.XWPFTable table ? table.getText() : "").reduce("", (a, b) -> a + b);
                assertTrue(ordered.indexOf("État dégradé") < ordered.indexOf("GAB GAB-002"));
                text = document.getParagraphs().stream().map(p -> p.getText()).reduce("", (a, b) -> a + b)
                        + document.getTables().stream().map(t -> t.getText()).reduce("", (a, b) -> a + b);
            }
        } else {
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                for (var sheet : workbook) assertFalse(sheet.getPrintSetup().getLandscape());
                var gab = workbook.getSheet("GABs et mouvements");
                assertNotNull(gab);
                assertEquals("Institution : Institution de test", gab.getRow(4).getCell(0).getStringCellValue());
                assertEquals("Mouvements du GAB GAB-001", gab.getRow(8).getCell(0).getStringCellValue());
                assertEquals("DOWN · Hors ligne", gab.getRow(10).getCell(3).getStringCellValue());
                assertEquals("GAB GAB-002", gab.getRow(12).getCell(0).getStringCellValue());
                StringBuilder content = new StringBuilder();
                workbook.forEach(sheet -> sheet.forEach(row -> row.forEach(cell -> content.append(cell.toString()))));
                text = content.toString();
            }
        }
        assertTrue(text.contains("GAB-001"));
        assertTrue(text.contains("GAB-002"));
        assertTrue(text.contains("DOWN"));
        assertTrue(text.contains("État dégradé"));
        verify(movements).all(1L, 10L, "DOWN", from, null);
        verify(movements).all(2L, 10L, "DOWN", from, null);
    }

    private AtmDto terminal(Long id, String terminal) {
        var result = new AtmDto(); result.setId(id); result.setTerminalId(terminal);
        result.setInstitutionName("Institution de test"); result.setStatusId("0"); return result;
    }
}
