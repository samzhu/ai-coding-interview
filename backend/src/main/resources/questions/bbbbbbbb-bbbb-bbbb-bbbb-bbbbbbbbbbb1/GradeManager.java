import java.util.*;

/**
 * Manages student grades and computes class statistics.
 * Grade scale: A(90-100), B(80-89), C(70-79), D(60-69), F(0-59)
 */
public class GradeManager {

    private final List<Student> students = new ArrayList<>();

    public void addStudent(Student student) {
        students.add(student);
    }

    public List<Student> getStudents() {
        return Collections.unmodifiableList(students);
    }

    /** Returns the grade letter for a given numeric score. */
    public String getGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    /**
     * Returns all students whose grade matches the given letter.
     *
     * BUG: Uses == to compare String objects instead of .equals().
     * This works accidentally when the argument is a compile-time constant
     * (String Pool), but fails silently for any dynamically created String
     * such as new String("A") or a value read from user input / network.
     *
     * Fix: replace  ==  with  .equals()
     */
    public List<Student> filterByGrade(String grade) {
        List<Student> result = new ArrayList<>();
        for (Student s : students) {
            if (getGrade(s.score()) == grade) {   // BUG: reference equality
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Returns the average score of all students.
     * Returns 0.0 if there are no students.
     *
     * TODO CP2: Implement this method.
     * Hint: avoid integer division — use (double) sum / count.
     */
    public double getAverageScore() {
        return 0.0;
    }

    /**
     * Returns the top N students sorted by score descending.
     * If n >= student count, returns all students.
     *
     * TODO CP2: Implement this method.
     */
    public List<Student> getTopStudents(int n) {
        return new ArrayList<>();
    }

    /**
     * Returns a map of grade letter -> count of students with that grade.
     * Only includes grades that have at least one student.
     * Example: {"A": 2, "B": 5, "F": 1}
     *
     * TODO CP2: Implement this method.
     */
    public Map<String, Integer> getGradeDistribution() {
        return new HashMap<>();
    }
}
