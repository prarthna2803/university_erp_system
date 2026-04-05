package erp.domain;

public class Student {
    private int userId;
    private String rollNo;
    private String program;
    private int year;

    public Student(int userId, String rollNo, String program, int year) {
        this.userId = userId;
        this.rollNo = rollNo;
        this.program = program;
        this.year = year;
    }

    public int getUserId() {
        return userId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getProgram() {
        return program;
    }

    public int getYear() {
        return year;
    }
}

