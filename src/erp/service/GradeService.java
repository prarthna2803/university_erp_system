package erp.service;

import erp.data.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GradeService {

    // numeric(0–100) -> letter grade
    public static String numericToLetter(double n) {
        if (n >= 90) return "A";
        if (n >= 85) return "A-";
        if (n >= 80) return "B+";
        if (n >= 75) return "B";
        if (n >= 70) return "B-";
        if (n >= 60) return "C";
        if (n >= 50) return "D";
        return "F";
    }

    // recompute final for one enrollment (reg_id)
    public static void recomputeFinalForEnrollment(int enrollmentId) throws Exception {
        try (Connection conn = DBConnection.getERPConnection()) {

            // 1) get user + course for this reg
            int userId;
            int courseId;

            String qReg = """
                SELECT user_id, course_id
                FROM registrations
                WHERE reg_id = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(qReg)) {
                ps.setInt(1, enrollmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("No registration for reg_id=" + enrollmentId);
                    }
                    userId = rs.getInt("user_id");
                    courseId = rs.getInt("course_id");
                }
            }

            // 2) fetch components
            BigDecimal endScore = null, endMax = null;
            BigDecimal midScore = null, midMax = null;
            BigDecimal quizTotalScore = BigDecimal.ZERO;
            BigDecimal quizTotalMax   = BigDecimal.ZERO;

            String qGrades = """
                SELECT component, score, max_score
                FROM grades
                WHERE enrollment_id = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(qGrades)) {
                ps.setInt(1, enrollmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String comp = rs.getString("component");
                        BigDecimal score = rs.getBigDecimal("score");
                        BigDecimal max   = rs.getBigDecimal("max_score");

                        if (score == null || max == null || max.doubleValue() <= 0.0) {
                            continue;
                        }

                        if ("Endsem".equalsIgnoreCase(comp)) {
                            endScore = score;
                            endMax   = max;
                        } else if ("Midsem".equalsIgnoreCase(comp)) {
                            midScore = score;
                            midMax   = max;
                        } else if (comp.toLowerCase().startsWith("quiz")) {
                            quizTotalScore = quizTotalScore.add(score);
                            quizTotalMax   = quizTotalMax.add(max);
                        }
                    }
                }
            }

            // 3) percent ratios
            double endPerc = 0.0, midPerc = 0.0, quizPerc = 0.0;

            if (endScore != null && endMax != null && endMax.doubleValue() > 0) {
                endPerc = endScore.doubleValue() / endMax.doubleValue();
            }
            if (midScore != null && midMax != null && midMax.doubleValue() > 0) {
                midPerc = midScore.doubleValue() / midMax.doubleValue();
            }
            if (quizTotalMax.doubleValue() > 0) {
                quizPerc = quizTotalScore.doubleValue() / quizTotalMax.doubleValue();
            }

            // 4) final numeric 0–100 with 45/35/20
            double numeric = endPerc * 45.0 + midPerc * 35.0 + quizPerc * 20.0;
            String letter = numericToLetter(numeric);

            // 5) upsert into final_grades (one row per user+course)
            Integer fgId = null;
            String qFind = """
                SELECT fg_id
                FROM final_grades
                WHERE user_id = ? AND course_id = ?
                ORDER BY fg_id DESC
                LIMIT 1
                """;
            try (PreparedStatement ps = conn.prepareStatement(qFind)) {
                ps.setInt(1, userId);
                ps.setInt(2, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        fgId = rs.getInt("fg_id");
                    }
                }
            }

            if (fgId == null) {
                String qIns = """
                    INSERT INTO final_grades (user_id, course_id, semester, numeric_grade, letter_grade)
                    VALUES (?, ?, '2025S', ?, ?)
                    """;
                try (PreparedStatement ps = conn.prepareStatement(qIns)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, courseId);
                    ps.setBigDecimal(3, new BigDecimal(String.format("%.2f", numeric)));
                    ps.setString(4, letter);
                    ps.executeUpdate();
                }
            } else {
                String qUpd = """
                    UPDATE final_grades
                    SET numeric_grade = ?, letter_grade = ?
                    WHERE fg_id = ?
                    """;
                try (PreparedStatement ps = conn.prepareStatement(qUpd)) {
                    ps.setBigDecimal(1, new BigDecimal(String.format("%.2f", numeric)));
                    ps.setString(2, letter);
                    ps.setInt(3, fgId);
                    ps.executeUpdate();
                }
            }
        }
    }
}
