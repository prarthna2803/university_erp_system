package erp.util;

import erp.data.DBConnection;

import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TranscriptExporter {

    private static final String TRANSCRIPT_SQL =
            "SELECT c.code, c.title, c.credits, g.final_grade " +
                    "FROM enrollments e " +
                    "JOIN sections s ON e.section_id = s.section_id " +
                    "JOIN courses c ON s.course_id = c.course_id " +
                    "LEFT JOIN grades g ON e.enrollment_id = g.enrollment_id " +
                    "WHERE e.student_id=? AND e.status='COMPLETED'";

    public Path exportCsv(int studentId, Path file) {
        List<TranscriptRow> rows = fetchRows(studentId);
        try (FileWriter writer = new FileWriter(file.toFile())) {
            writer.write("Code,Title,Credits,Final Grade\n");
            for (TranscriptRow row : rows) {
                writer.write(String.format("%s,%s,%d,%s%n",
                        row.code(), row.title(), row.credits(),
                        row.finalGrade() == null ? "" : row.finalGrade()));
            }
            return file;
        } catch (Exception e) {
            throw new RuntimeException("Unable to export transcript", e);
        }
    }

    public Path exportPdf(int studentId, Path file) {
        List<TranscriptRow> rows = fetchRows(studentId);
        List<String> lines = new ArrayList<>();
        lines.add("IIIT Delhi ERP Transcript");
        lines.add("Student ID: " + studentId);
        lines.add("");
        lines.add(String.format("%-10s %-30s %-8s %-10s", "Code", "Title", "Credits", "Final"));
        for (TranscriptRow row : rows) {
            lines.add(String.format("%-10s %-30s %-8d %-10s",
                    row.code(),
                    truncate(row.title(), 28),
                    row.credits(),
                    row.finalGrade() == null ? "" : row.finalGrade()));
        }

        try {
            writeSimplePdf(lines, file);
            return file;
        } catch (Exception e) {
            throw new RuntimeException("Unable to export transcript PDF", e);
        }
    }

    private List<TranscriptRow> fetchRows(int studentId) {
        List<TranscriptRow> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(TRANSCRIPT_SQL)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new TranscriptRow(
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getInt("credits"),
                        rs.getString("final_grade")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to fetch transcript data", e);
        }
        return rows;
    }

    private void writeSimplePdf(List<String> lines, Path file) throws Exception {
        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 12 Tf\n72 730 Td\n");
        boolean first = true;
        for (String line : lines) {
            if (!first) {
                stream.append("0 -16 Td\n");
            }
            first = false;
            stream.append("(").append(escapePdf(line)).append(") Tj\n");
        }
        stream.append("ET\n");

        byte[] streamBytes = stream.toString().getBytes(StandardCharsets.US_ASCII);

        List<String> objects = new ArrayList<>();
        objects.add("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        objects.add("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");
        objects.add("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n");
        objects.add("4 0 obj << /Length " + streamBytes.length + " >> stream\n" +
                stream + "endstream\nendobj\n");
        objects.add("5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (String obj : objects) {
            offsets.add(pdf.length());
            pdf.append(obj);
        }
        int xrefPos = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer << /Size ").append(objects.size() + 1)
                .append(" /Root 1 0 R >>\nstartxref\n")
                .append(xrefPos).append("\n%%EOF");

        Files.writeString(file, pdf.toString(), StandardCharsets.US_ASCII);
    }

    private String escapePdf(String line) {
        return line.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private record TranscriptRow(String code, String title, int credits, String finalGrade) { }
}

