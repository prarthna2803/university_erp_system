package erp.domain;

public class Enrollment {
    private int id;
    private int studentId;
    private int sectionId;
    private String status;

    public Enrollment(int id, int studentId, int sectionId, String status) {
        this.id = id;
        this.studentId = studentId;
        this.sectionId = sectionId;
        this.status = status;
    }

    public int getId() {
        return id;
    }
    public int getSectionId() {
        return sectionId;
    }

}

