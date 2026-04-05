package erp.domain;

public class Instructor {
    private int userId;
    private String department;
    private String title;

    public Instructor(int userId, String department, String title) {
        this.userId = userId;
        this.department = department;
        this.title = title;
    }

    public int getUserId() {
        return userId;
    }
    public String getDepartment() { return department; }
    public String getTitle() {
        return title;
    }
}

