package DSA;
import java.util.Scanner;

/**
 * For left rotate-
 * arr[i] = arr[(i+D)%N]
 *
 * For right rotate-
 * arr[i] = arr[(i+N-D)%N]
 *
 */


public class RotateArratCW {

    public static void main (String[] args)
    {

        Scanner sc = new Scanner(System.in);
        int testCase; //Number of test cases from input.
        testCase = sc.nextInt();

        while (testCase != 0) {
            int count = 0;
            int n; //Number of elements  in an array.
            n = sc.nextInt();
            int x = sc.nextInt();

            int[] arr = new int[n];

            for (int i = 0; i < n; i++) {
                arr[i] = sc.nextInt();
            }

            int ar[] = rotateArrayCLW(n, x, arr);

            for (int i = 0; i < n; i++) {

                System.out.print(ar[i] + " ");
            }
            System.out.print("\n");
            testCase--;

        }
    }
    private static int [] rotateArrayCLW(int n, int d, int[] arr) {

        for (int i = 0 ; i < d ; i++){

            int first = arr[0];
            for(int j =0 ; j < n-1 ; j++){

                //  int first = arr[0];
                arr[j] = arr [j+1];

            }
            arr[arr.length-1] = first;

        }
        return arr;

    }


}
