package DSA;


import java.util.HashMap;
import java.util.*;

public class HashMapDemo {

    public static void main(String[] args) {

        Map<Employee,String> mapDemo= new HashMap<Employee,String>();

        Employee e1 = new Employee();
        e1.setId(1);
        e1.setName("jcp");

        Employee e2 = new Employee();
        e2.setId(2);
        e2.setName("by");

        Employee e3 = new Employee();
        e3.setId(2);
        e3.setName("by");

       // System.out.println(e2.equals(e3));  // false

        e2=e3;

         mapDemo.put(e1,"Abc");    //>> e1.hashcode , bucket = hascode $$ (n-1)
         mapDemo.put(e1,"Abcd444");   // //>> e1.hashcode , bucket = hascode $$ (n-1)

        mapDemo.put(e1,"Abcgggg");




        //mapDemo.get(e1);

        System.out.println(mapDemo.get(e2));
        System.out.println(mapDemo.size());

    }

}

class Employee {

   private int id;
   private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
//_______________________________-
/*    @Override
    public boolean equals(Object o) {

        System.out.println("in insert equals");

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;

        return id == employee.id &&
                Objects.equals(name, employee.name);
    }*/

/*    @Override
    public int hashCode() {
        System.out.println("in insert hashcode" + Objects.hash(id, name));
        return Objects.hash(id, name);
    }*/
}


