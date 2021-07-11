import javax.crypto.spec.PSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class Student{

    private int id;
    private int rollNumber;
    private String name;

    public Student(int id, int rollNumber, String name) {
        this.id = id;
        this.rollNumber = rollNumber;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(int rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", rollNumber=" + rollNumber +
                ", name='" + name + '\'' +
                '}';
    }

    public int hashCode () {
        return 1; }

}

public class StoryTestJava8ForEach {

    public static void main(String[] args) {

        List<Student> studentList = Arrays.asList(new Student(1 , 10 , "Aakash"),
                                                  new  Student(2 , 21 , "Bikash"),
                                                  new  Student(3 , 33 , "Cikash"),
                                                   new  Student(4 , 40 , "Dikesh"));

        // filter list of student, whose roll number is even

        List<Student> studentListFiletr = studentList.stream().filter(student -> student.getRollNumber()%2 == 0).collect(Collectors.toList());

        System.out.println(studentListFiletr);

     //   for(Student s : studentList) System.out.println(s.getId() + " " + s.getName());


 /*       studentList.forEach(student -> System.out.println(student.getId()));

        studentList.forEach(student -> System.out.println(student.getName()));*/


        String s1 = "Abc";
        String s2 = "Abc";

        System.out.println(s1.equalsIgnoreCase(s2));
        System.out.println( s1 == s2);
        System.out.println(s1.equals(s2));


        String date = "2021-06-14";

       String[] str = date.split("-");

       String stree = str[0].concat(str[1]).concat(str[2]);



        System.out.println(stree);

       StringBuilder sb = new StringBuilder();


    }






}
