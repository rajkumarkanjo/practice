/*
  1> only for Functional Interface(have only one abstract method) this lamba function nis applicable

  Lamda Exprewssion is a Annonymes function means , no Name , no return Type , no modifires

 */


import com.sun.rowset.internal.Row;

import java.util.Arrays;

interface  Sum {

    public void add(int a , int b);
   // public void add3(int a , int b); ambiguity will come
}

public class Java_8 {

    public static void main(String[] args) {

        int arr [] = {2,3,1};

        int sumjava8 = Arrays.stream(arr).sum();    System.out.println(sumjava8);




        Sum sum = (a,b) -> System.out.printf("the sum  = " +  (a+b));
        sum.add(4,5);

    }
}
