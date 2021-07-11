package GOG;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Suresh {

    public static void main(String[] args) {

        int A[] = {1,2,2,4};
        int N = A.length;

        Integer i1 = new Integer(123);
        Integer i2 = new Integer(123);
        System.out.println(i1==i2); // False


        Integer i3 = 128;
        Integer i4 = 128;

        System.out.println(i3==i4); // False


        //  System.out.println(a);

        Set<Integer> num = new HashSet<>();

        for(int i = 0 ; i < N ; i++){

            num.add(A[0]);

        }
       // System.out.println(num.size());


    }

}
