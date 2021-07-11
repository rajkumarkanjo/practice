package DSA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class LaderCount {

    public static void main(String[] args) {

        int  A[] = {16,17,4,3,5,2};

        ArrayList<Integer> out =  LaderCount.leaders(A,6);

        System.out.println("Answer "+ out);

    }

    static ArrayList<Integer> leaders(int arr[], int n){
        // Your code here

        ArrayList<Integer> outPutList = new ArrayList<>();

       // Collections.reverse(Arrays.asList(arr));

      //  System.out.println(Arrays.toString(arr));

        outPutList.add(arr[arr.length-1]);
/*        int i = 0 ;
        for (int j = 1 ; j < n ; j++){

            if (arr[i] < arr[j]){

                outPutList.add(arr[j]);

                arr[i] = arr[j];
            }

        }*/
        int i = n-1 ;
        for (int j = n-2 ; 0 <= j ; j--){

            if (arr[i] < arr[j]){

                outPutList.add(arr[j]);
                i = j ;
                arr[i] = arr[j];

            }

        }
        return outPutList;
    }
}
