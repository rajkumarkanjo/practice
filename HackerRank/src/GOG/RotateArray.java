package GOG;

import java.util.Arrays;

/*
To handle large D values, before rotating use-
if(D>N)
D %= N;

For left rotate-
arr[i] = arr[(i+D)%N]

For right rotate-
arr[i] = arr[(i+N-D)%N]
 */
public class RotateArray {

    public static void main(String[] args) {

        int n = 5;
        int d = 2; // number of rotation
        int arr[] =  {1,2,3,4,5};

       int ar[]=  rotateArrayCLW(n,d,arr);
       System.out.println(Arrays.toString(ar));
    }

    private static int[] rotateArrayCLW(int n, int d, int[] arr) {
/*        for (int i = 0 ; i < d ; i++){
            int first = arr[0];
            for(int j =0 ; j < n-1 ; j++){
                arr[j] = arr [j+1];
            }
            arr[arr.length-1] = first;
        }*/
        return  arr;
    }
}
