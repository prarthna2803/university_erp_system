package erp.domain;

public class Grade {
    private int enrollmentId;
    private String component;
    private double score;
    private String finalGrade;

    public Grade(int enrollmentId, String component, double score, String finalGrade) {
        this.enrollmentId = enrollmentId;
        this.component = component;
        this.score = score;
        this.finalGrade = finalGrade;
    }

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public String getComponent() {
        return component;
    }

    public double getScore() {
        return score;
    }

    public String getFinalGrade() {
        return finalGrade;
    }
}

