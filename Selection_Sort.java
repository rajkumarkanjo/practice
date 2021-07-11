package DSA;

import java.util.Arrays;

public class Selection_Sort {

    public static void main(String[] args) {

        // find the minimum element in an array first

        int arr[] = {64, 25, 12, 22, 11};
        int n = arr.length;
       // int min = arr[0];

        for (int i = 0 ; i< n-1 ; i++ ){

            int minIndex = i ;
            for (int j = i ; j < n ; j++){

                if (arr[j] < arr[minIndex] ){
                    minIndex = j ;        // need to swap this
                }

            }
            //Swap part
            int temp = arr[i];
            arr[i] = arr[minIndex];
            arr[minIndex] = temp;

        }

        for (int e: arr) {
            System.out.println(e+" ");
            
        }
      //  System.out.println(Arrays.toString(arr));




        // bubble sort this is
/*        for (int j = 0; j < arr.length; j++) {

            for (int i = j+1; i < arr.length; i++) {

                if (arr[j] > arr[i]) {

                    int k = arr[j];
                    arr[j] = arr[i];
                    arr[i] = k;
                    min = arr[j];
                }
            }
         //   System.out.println("min value = "+min);
        }
        System.out.println(Arrays.toString(arr));*/
    }

}
