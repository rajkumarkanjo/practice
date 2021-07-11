import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;
/*
Sample Input 1
3
3 2 1  [3,2,1]
Sample Output 1

Array is sorted in 3 swaps.
First Element: 1
Last Element: 3
 */
public class Sorting {

    public static void main(String[] args) {

/*        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int[] a = new int[n];
        for(int a_i=0; a_i < n; a_i++){
            a[a_i] = in.nextInt();
        }*/
        int c = 1228981;
        int a[] = {3,2,1};  //{2,3,1} ->
        // Write Your Code Here
        int swapCount = 0;
        for (int i = 0 ; i < a.length-1 ; i++){

            for (int j = 1 ; j < a.length ; j++){

                if (a[i] > a[j]){

                   int temp = a[i]; //3
                   a[i] = a[j];
                   a[j] = temp;

                    swapCount++;   //no of swapping during sort process
                }
            }
            if(swapCount == 0){
                break;
            }
        }
        System.out.println("Array is sorted in "+ swapCount +" swaps.");
        System.out.println("First Element: "+ a[0]);
        System.out.println("Last Element: "+ a[a.length-1]);
    }


}
