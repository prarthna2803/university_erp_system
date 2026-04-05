package erp.domain;

public class Section {
    private int id;
    private int courseId;
    private int instructorId;
    private String dayTime;
    private String room;
    private int capacity;
    private String semester;
    private int year;

    public Section(int id, int courseId, int instructorId, String dayTime, String room,
                   int capacity, String semester, int year) {
        this.id = id;
        this.courseId = courseId;
        this.instructorId = instructorId;
        this.dayTime = dayTime;
        this.room = room;
        this.capacity = capacity;
        this.semester = semester;
        this.year = year;
    }

    public int getId() {
        return id;
    }

    public int getCourseId() {
        return courseId;
    }

    public int getInstructorId() {
        return instructorId;
    }

    public String getDayTime() {
        return dayTime;
    }

    public String getRoom() {
        return room;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getSemester() {
        return semester;
    }

    public int getYear() {
        return year;
    }
}

