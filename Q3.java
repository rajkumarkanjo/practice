package DSA;

import java.util.ArrayList;
import java.util.Arrays;

/*
Given an array arr[] of positive integers of size N. Reverse every sub-array group of size K.
 */
public class Q3 {


    public static void main(String[] args) {

        int N = 5, K = 3;
        //int arr[] = {1, 2, 3, 4, 5};
        ArrayList<Integer> arr = new ArrayList<>();
        arr.add(1);
        arr.add(2);
        arr.add(3);
        arr.add(4);
        arr.add(5);
        ArrayList<Integer> dummy = new ArrayList<>();
        for (int i = K-1; i>=0; i--) {
           // System.out.print(arr.get(i)+" ");
            dummy.add(arr.get(i));
        }
        for(int j = arr.size()-1 ; j >=K ; j-- ){

           // System.out.print(arr.get(j)+" ");
            dummy.add(arr.get(j));
        }
       arr.addAll(dummy);
      //  System.out.println(Arrays.toString(arr));
        System.out.println(arr);
    }
}