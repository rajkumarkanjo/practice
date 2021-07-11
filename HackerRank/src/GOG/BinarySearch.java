package GOG;

import java.util.Arrays;

public class BinarySearch {

    public static void main(String[] args) {

       // int arr [] = {2 , 5 , 8, 9 , 1 , 7 , 3};
       int arr[] = {5, 6,7,8,9,10,1,2,3};
        int n = 10 ;

      //  int arr1[] = new int[10];

      //  int x = Arrays.asList(arr).indexOf(n);
     //   System.out.println(x);
     // //  Arrays.sort(arr);
      // int c = Arrays.binarySearch(arr,n);
      //  System.out.println(c);
        find(arr , n);

    }
    private static int find(int[] arr, int a) {

        System.out.println(Arrays.toString(arr));
        int left = 0 ; int right = arr.length - 1; //[ arr.length= 7 ]
       // System.out.println("element NOT found ");

        while( left <= right){

            int middle = left + (right - left) / 2;

            if(arr[middle] == a){
                System.out.println("element present at index  "+ middle +" >> "+ arr[middle]);
                break;
            }

           else if(arr[middle] < a){

                left = middle+1 ;

            }
            else if(arr[middle] > a){

                right = middle-1 ;

            }
        }
        return -1;

    }

}
