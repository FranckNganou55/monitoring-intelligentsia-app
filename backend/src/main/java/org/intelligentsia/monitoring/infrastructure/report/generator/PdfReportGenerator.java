package org.intelligentsia.monitoring.infrastructure.report.generator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportColumn;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportDocument;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportKpi;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportMetadata;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportSection;
import org.intelligentsia.monitoring.infrastructure.report.model.ReportTable;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PdfReportGenerator implements ReportGenerator {

    private static final float MARGIN = 38f;
    private static final float FOOTER_HEIGHT = 24f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final Color NAVY = new Color(31, 48, 69);
    private static final Color GOLD = new Color(201, 168, 76);
    private static final Color MUTED = new Color(103, 115, 129);
    private static final Color BORDER = new Color(221, 226, 231);
    private static final Color PALE = new Color(246, 248, 250);
    private static final Color PALE_BLUE = new Color(230, 238, 246);

    @Override
    public String format() {
        return "PDF";
    }

    @Override
    public void generate(ReportDocument report, Path target) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        try (PDDocument document = new PDDocument()) {
            PDType0Font regular = loadFont(document, "/fonts/Vera.ttf");
            PDType0Font bold = loadFont(document, "/fonts/VeraBd.ttf");
            ReportMetadata metadata = report.getMetadata();
            document.getDocumentInformation().setTitle(metadata.getReportTitle());
            document.getDocumentInformation().setAuthor(metadata.getApplicationName());

            Layout layout = new Layout(document, regular, bold, metadata);
            layout.startFirstPage();
            layout.heading("Synthèse");
            layout.kpis(report.getKpis());
            boolean commentWritten = false;
            for (ReportSection section : report.getSections()) {
                if (!commentWritten && "Validation".equalsIgnoreCase(section.getTitle())) {
                    layout.comment();
                    commentWritten = true;
                }
                layout.section(section);
            }
            if (!commentWritten) layout.comment();
            layout.finish();
            document.save(target.toFile());
        }
    }

    private PDType0Font loadFont(PDDocument document, String resource) throws IOException {
        try (InputStream font = getClass().getResourceAsStream(resource)) {
            if (font == null) throw new IOException("Police PDF introuvable : " + resource);
            return PDType0Font.load(document, font);
        }
    }

    private final class Layout {
        private final PDDocument document;
        private final PDType0Font regular;
        private final PDType0Font bold;
        private final ReportMetadata metadata;
        private PDPageContentStream stream;
        private float y;

        private Layout(PDDocument document, PDType0Font regular, PDType0Font bold, ReportMetadata metadata) {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            this.metadata = metadata;
        }

        private void startFirstPage() throws IOException {
            newPage(false);
            stream.setNonStrokingColor(GOLD);
            stream.addRect(MARGIN, PAGE_HEIGHT - 43, PAGE_WIDTH - 2 * MARGIN, 3);
            stream.fill();
            y = PAGE_HEIGHT - 62;
            write(metadata.getApplicationName(), bold, 10, NAVY, 14);
            if (notBlank(metadata.getInstitutionName())) {
                write("Institution : " + metadata.getInstitutionName(), regular, 9, MUTED, 14);
            }
            y -= 4;
            writeWrapped(metadata.getReportTitle(), bold, 18, NAVY, 22, PAGE_WIDTH - 2 * MARGIN);
            if (notBlank(metadata.getSubtitle())) {
                writeWrapped(metadata.getSubtitle(), regular, 9, MUTED, 13, PAGE_WIDTH - 2 * MARGIN);
            }
            y -= 3;
            writeWrapped("Période : " + value(metadata.getPeriod()), regular, 9, NAVY, 13, PAGE_WIDTH - 2 * MARGIN);
            writeWrapped("Source : " + value(metadata.getSource()), regular, 9, NAVY, 13, PAGE_WIDTH - 2 * MARGIN);
            y -= 5;
            rule();
            y -= 14;
        }

        private void comment() throws IOException {
            if (!notBlank(metadata.getComment())) return;
            heading("Commentaire de la journée");
            writeWrapped(metadata.getComment(), regular, 9, NAVY, 13, PAGE_WIDTH - 2 * MARGIN);
            y -= 8;
        }

        private void newPage(boolean continuation) throws IOException {
            closeStream();
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = PAGE_HEIGHT - MARGIN;
            if (continuation) {
                writeWrapped(metadata.getApplicationName() + " — " + metadata.getReportTitle(), bold, 8, MUTED, 12, PAGE_WIDTH - 2 * MARGIN);
                rule();
                y -= 10;
            }
        }

        private void heading(String text) throws IOException {
            List<String> lines = wrap(text, bold, 12, PAGE_WIDTH - 2 * MARGIN - 20);
            float height = lines.size() * 17 + 12;
            ensure(height + 6);
            stream.setNonStrokingColor(PALE_BLUE);
            stream.addRect(MARGIN, y - height + 7, PAGE_WIDTH - 2 * MARGIN, height);
            stream.fill();
            stream.setNonStrokingColor(GOLD);
            stream.addRect(MARGIN, y - height + 7, 3, height);
            stream.fill();
            for (int i = 0; i < lines.size(); i++) drawText(lines.get(i), MARGIN + 10, y - 10 - i * 17, bold, 12, NAVY);
            y -= height + 6;
        }

        private void kpis(List<ReportKpi> kpis) throws IOException {
            if (kpis == null || kpis.isEmpty()) return;
            float gap = 9;
            float cardWidth = (PAGE_WIDTH - 2 * MARGIN - gap) / 2;
            for (int start = 0; start < kpis.size(); start += 2) {
                ensure(64);
                int end = Math.min(start + 2, kpis.size());
                float rowHeight = 60;
                for (int index = start; index < end; index++) {
                    float x = MARGIN + (index - start) * (cardWidth + gap);
                    stream.setNonStrokingColor(PALE);
                    stream.setStrokingColor(BORDER);
                    stream.addRect(x, y - rowHeight, cardWidth, rowHeight);
                    stream.fillAndStroke();
                    ReportKpi kpi = kpis.get(index);
                    drawText(kpi.getLabel(), x + 9, y - 16, bold, 8.5f, MUTED);
                    drawText(kpi.getValue(), x + 9, y - 36, bold, 15, NAVY);
                    if (notBlank(kpi.getDescription())) {
                        List<String> lines = wrap(kpi.getDescription(), regular, 7.2f, cardWidth - 18);
                        drawText(lines.get(0), x + 9, y - 51, regular, 7.2f, MUTED);
                    }
                }
                y -= rowHeight + 14;
            }
        }

        private void section(ReportSection section) throws IOException {
            ensure(110);
            heading(section.getTitle());
            if (notBlank(section.getDescription())) {
                writeWrapped(section.getDescription(), regular, 9, MUTED, 13, PAGE_WIDTH - 2 * MARGIN);
                y -= 3;
            }
            if (section.getItems() != null) {
                for (Map<String, Object> item : section.getItems()) {
                    String label = value(item.get("label"));
                    Object total = item.get("total");
                    writeWrapped(total == null ? label : label + " : " + total,
                            regular, 9, NAVY, 13, PAGE_WIDTH - 2 * MARGIN - 12);
                }
                y -= 3;
            }
            if (section.getTable() != null) table(section.getTable());
            y -= 18;
        }

        private void table(ReportTable table) throws IOException {
            List<ReportColumn> columns = table.getColumns();
            List<Map<String, Object>> rows = table.getRows();
            if (columns == null || columns.isEmpty()) return;
            if (notBlank(table.getTitle())) {
                ensure(100);
                write(table.getTitle(), bold, 9, NAVY, 13);
                y -= 2;
            }
            if (table.isRecordLayout() || columns.size() > 8) {
                if (rows == null || rows.isEmpty()) write("Aucune donnée", regular, 9, MUTED, 14);
                else for (int index = 0; index < rows.size(); index++) record(columns, rows.get(index), index + 1);
                return;
            }
            float tableWidth = PAGE_WIDTH - 2 * MARGIN;
            float[] widths = columnWidths(columns, tableWidth);
            int headerLines = 1;
            for (int i = 0; i < columns.size(); i++) {
                headerLines = Math.max(headerLines, wrap(columns.get(i).getLabel(), bold, 7.5f, widths[i] - 8).size());
            }
            float headerHeight = Math.max(26, headerLines * 9 + 12);
            if (rows == null || rows.isEmpty()) {
                ensure(headerHeight + 22);
                drawTableHeader(columns, widths, tableWidth);
                Map<String, Object> empty = new LinkedHashMap<>();
                columns.forEach(column -> empty.put(column.getKey(), ""));
                empty.put(columns.get(0).getKey(), "Aucune donnée");
                drawTableRow(columns, empty, widths, tableWidth, false, 22);
                return;
            }
            boolean needsHeader = true;
            for (int index = 0; index < rows.size(); index++) {
                List<List<String>> cellLines = new ArrayList<>();
                int lineCount = 1;
                for (int c = 0; c < columns.size(); c++) {
                    var lines = wrap(value(rows.get(index).get(columns.get(c).getKey())), regular, 7.5f, widths[c] - 8);
                    cellLines.add(lines);
                    lineCount = Math.max(lineCount, lines.size());
                }
                int offset = 0;
                while (offset < lineCount) {
                    float minimum = (needsHeader ? headerHeight : 0) + Math.min(lineCount - offset, 3) * 9 + 10;
                    if (y - minimum < MARGIN + FOOTER_HEIGHT) {
                        newPage(true);
                        needsHeader = true;
                    }
                    if (needsHeader) {
                        drawTableHeader(columns, widths, tableWidth);
                        needsHeader = false;
                    }
                    int capacity = Math.max(1, (int) ((y - MARGIN - FOOTER_HEIGHT - 10) / 9));
                    int count = Math.min(lineCount - offset, capacity);
                    Map<String, Object> part = new LinkedHashMap<>();
                    for (int c = 0; c < columns.size(); c++) {
                        var lines = cellLines.get(c);
                        part.put(columns.get(c).getKey(), offset >= lines.size() ? "" :
                                String.join("\n", lines.subList(offset, Math.min(offset + count, lines.size()))));
                    }
                    drawTableRow(columns, part, widths, tableWidth, index % 2 == 1, Math.max(20, count * 9 + 10));
                    offset += count;
                    if (offset < lineCount) { newPage(true); needsHeader = true; }
                }
            }
        }

        private void record(List<ReportColumn> columns, Map<String, Object> row, int index) throws IOException {
            float width = PAGE_WIDTH - 2 * MARGIN;
            List<ReportColumn> shortFields = columns.stream().filter(c -> !longField(c.getKey()) && !"id".equals(c.getKey())).toList();
            List<ReportColumn> longFields = columns.stream().filter(c -> longField(c.getKey())).toList();
            float height = 28;
            for (int i = 0; i < shortFields.size(); i += 2) {
                int lines = 1;
                for (int j = i; j < Math.min(i + 2, shortFields.size()); j++) {
                    ReportColumn c = shortFields.get(j);
                    lines = Math.max(lines, wrap(c.getLabel() + " : " + value(row.get(c.getKey())), regular, 8.5f, width / 2 - 16).size());
                }
                height += lines * 12 + 4;
            }
            for (ReportColumn c : longFields) height += wrap(c.getLabel() + " : " + value(row.get(c.getKey())), "title".equals(c.getKey()) ? bold : regular, 8.5f, width).size() * 12 + 6;
            ensure(Math.min(height + 12, PAGE_HEIGHT - MARGIN * 2 - FOOTER_HEIGHT - 30));
            stream.setNonStrokingColor(Color.decode("#" + ReportDisplay.riskColor(row)));
            stream.addRect(MARGIN, y - 21, width, 23);
            stream.fill();
            drawText(ReportDisplay.recordTitle(row, index), MARGIN + 8, y - 13, bold, 10, Color.WHITE);
            y -= 30;
            for (int i = 0; i < shortFields.size(); i += 2) {
                List<List<String>> lines = new ArrayList<>();
                int count = 1;
                for (int j = i; j < Math.min(i + 2, shortFields.size()); j++) {
                    ReportColumn c = shortFields.get(j);
                    List<String> text = wrap(c.getLabel() + " : " + value(row.get(c.getKey())), regular, 8.5f, width / 2 - 16);
                    lines.add(text); count = Math.max(count, text.size());
                }
                ensure(count * 12 + 4);
                for (int j = 0; j < lines.size(); j++) {
                    for (int k = 0; k < lines.get(j).size(); k++) drawText(lines.get(j).get(k), MARGIN + 8 + j * width / 2, y - k * 12, regular, 8.5f, NAVY);
                }
                y -= count * 12 + 4;
            }
            for (ReportColumn c : longFields) {
                ensure(38);
                writeWrapped(c.getLabel() + " : " + value(row.get(c.getKey())), "title".equals(c.getKey()) ? bold : regular, 8.5f, NAVY, 12, width);
                y -= 6;
            }
            rule(); y -= 16;
        }

        private boolean longField(String key) {
            return List.of("title", "description", "rule", "rule_label", "rule_name", "comment", "message").contains(key);
        }

        private float[] columnWidths(List<ReportColumn> columns, float tableWidth) {
            float[] widths = new float[columns.size()];
            float totalWeight = 0;
            for (ReportColumn column : columns) totalWeight += columnWeight(column.getKey());
            for (int index = 0; index < columns.size(); index++) {
                widths[index] = tableWidth * columnWeight(columns.get(index).getKey()) / totalWeight;
            }
            return widths;
        }

        private float columnWeight(String key) {
            return switch (key == null ? "" : key) {
                case "id" -> 0.45f;
                case "detected_at" -> 0.95f;
                case "category" -> 0.65f;
                case "terminal_id" -> 0.9f;
                case "card_number" -> 1.2f;
                case "customer_name" -> 1.15f;
                case "rule_label", "rule_name" -> 2.1f;
                case "risk_level" -> 0.65f;
                case "status", "treatment_state" -> 0.9f;
                case "source", "previous" -> 0.65f;
                case "date" -> 1.0f;
                case "message" -> 1.8f;
                case "amount" -> 0.95f;
                case "transaction_reference" -> 1.0f;
                default -> 1.0f;
            };
        }

        private void drawTableHeader(List<ReportColumn> columns, float[] widths, float tableWidth) throws IOException {
            int lineCount = 1;
            for (int i = 0; i < columns.size(); i++) {
                lineCount = Math.max(lineCount, wrap(columns.get(i).getLabel(), bold, 7.5f, widths[i] - 8).size());
            }
            float height = Math.max(26, lineCount * 9 + 12);
            ensure(height);
            stream.setNonStrokingColor(NAVY);
            stream.addRect(MARGIN, y - height, tableWidth, height);
            stream.fill();
            float x = MARGIN;
            for (int column = 0; column < columns.size(); column++) {
                List<String> labels = wrap(columns.get(column).getLabel(), bold, 7.5f, widths[column] - 8);
                for (int line = 0; line < labels.size(); line++) {
                    drawText(labels.get(line), x + 4, y - 12 - line * 9, bold, 7.5f, Color.WHITE);
                }
                x += widths[column];
            }
            y -= height;
        }

        private void drawTableRow(List<ReportColumn> columns, Map<String, Object> row,
                                  float[] widths, float tableWidth, boolean alternate,
                                  float rowHeight) throws IOException {
            if (alternate) {
                stream.setNonStrokingColor(PALE);
                stream.addRect(MARGIN, y - rowHeight, tableWidth, rowHeight);
                stream.fill();
            }
            float x = MARGIN;
            for (int column = 0; column < columns.size(); column++) {
                ReportColumn definition = columns.get(column);
                List<String> lines = wrap(value(row.get(definition.getKey())), regular, 7.5f, widths[column] - 8);
                for (int line = 0; line < lines.size(); line++) {
                    drawText(lines.get(line), x + 3,
                            y - 11 - line * 9f, regular, 7.5f, NAVY);
                }
                x += widths[column];
            }
            stream.setStrokingColor(BORDER);
            stream.moveTo(MARGIN, y - rowHeight);
            stream.lineTo(MARGIN + tableWidth, y - rowHeight);
            stream.stroke();
            y -= rowHeight;
        }

        private void writeWrapped(String text, PDType0Font font, float size, Color color,
                                  float leading, float maxWidth) throws IOException {
            for (String line : wrap(value(text), font, size, maxWidth)) write(line, font, size, color, leading);
        }

        private List<String> wrap(String text, PDType0Font font, float size, float maxWidth) throws IOException {
            String normalized = Normalizer.normalize(value(text).replace('\u00a0', ' '), Normalizer.Form.NFC);
            List<String> lines = new ArrayList<>();
            for (String paragraph : normalized.split("\\R", -1)) {
                String current = "";
                for (String word : paragraph.split("\\s+")) {
                    if (word.isEmpty()) continue;
                    if (font.getStringWidth(word) * size / 1000f > maxWidth) {
                        if (!current.isEmpty()) { lines.add(current); current = ""; }
                        String fragment = "";
                        for (int cp : word.codePoints().toArray()) {
                            String character = new String(Character.toChars(cp));
                            if (!fragment.isEmpty() && font.getStringWidth(fragment + character) * size / 1000f > maxWidth) {
                                lines.add(fragment);
                                fragment = "";
                            }
                            fragment += character;
                        }
                        current = fragment;
                        continue;
                    }
                    String candidate = current.isEmpty() ? word : current + " " + word;
                    if (!current.isEmpty() && font.getStringWidth(candidate) * size / 1000f > maxWidth) {
                        lines.add(current);
                        current = word;
                    } else {
                        current = candidate;
                    }
                }
                lines.add(current);
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        private void write(String text, PDType0Font font, float size, Color color, float leading) throws IOException {
            for (String line : wrap(text, font, size, PAGE_WIDTH - 2 * MARGIN)) {
                ensure(leading + 2);
                drawText(line, MARGIN, y, font, size, color);
                y -= leading;
            }
        }

        private void drawText(String text, float x, float baseline, PDType0Font font,
                              float size, Color color) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(Normalizer.normalize(value(text), Normalizer.Form.NFC));
            stream.endText();
        }

        private void ensure(float height) throws IOException {
            if (y - height < MARGIN + FOOTER_HEIGHT) newPage(true);
        }

        private void rule() throws IOException {
            stream.setStrokingColor(BORDER);
            stream.moveTo(MARGIN, y);
            stream.lineTo(PAGE_WIDTH - MARGIN, y);
            stream.stroke();
        }

        private void finish() throws IOException {
            closeStream();
            int pageNumber = 1;
            for (PDPage current : document.getPages()) {
                try (PDPageContentStream footer = new PDPageContentStream(
                        document, current, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    footer.setStrokingColor(BORDER);
                    footer.moveTo(MARGIN, MARGIN + 12);
                    footer.lineTo(PAGE_WIDTH - MARGIN, MARGIN + 12);
                    footer.stroke();
                    footer.beginText();
                    footer.setFont(regular, 7);
                    footer.setNonStrokingColor(MUTED);
                    footer.newLineAtOffset(MARGIN, MARGIN);
                    footer.showText("Généré le " + metadata.getGeneratedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " · Page "
                            + pageNumber + "/" + document.getNumberOfPages());
                    footer.endText();
                }
                pageNumber++;
            }
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String value(Object value) {
        return ReportDisplay.value(value);
    }
}
