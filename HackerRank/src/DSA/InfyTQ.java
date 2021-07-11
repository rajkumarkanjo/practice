package DSA;

import java.util.*;

public class InfyTQ {

    public static void main(String[] args) {

     // find the second largest without sorting the array

        int arr[] = {1,5,3,2};

        // output = 3
        int first_MAX = arr[0];

        for (int i = 0 ; i < arr.length ; i++ ){

            if (first_MAX < arr[i]){
                first_MAX = arr[i];
            }
        }

// delte the element
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == first_MAX) {
                arr[i] = -1;
            }
        }
        int second_MAX = arr[0];
        for (int i = 0 ; i < arr.length ; i++ ){

            if (second_MAX < arr[i]){
                second_MAX = arr[i];
            }
        }
        System.out.println(second_MAX);
    }

}
