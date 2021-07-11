package ComparatorComparable;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Objects;

// [5:00 pm] Mohanty, Anubhab
//Int i; int k; int j;
//
//[5:01 pm] Mohanty, Anubhab
//i = 5; j= 100 ; k= 10;
//
//[5:02 pm] Mohanty, Anubhab
//31*(31(31+5)+100)+10;

public class Anubhab {

    public static void main(String[] args) {
        //  sisd(5 , 4, 11) ;

/*

      int i = 5 , j =100 , k=10;


            int x =  Objects.hash(i, j, k);

        System.out.println("from inbuilt ===="+ x);

        int b = 31*(31*(31+5)+100)+10;

        System.out.println(b);
*/

        int i = 0;
        int sum = 0;
        while (i < 100) {
            sum = sum + 1;
            sum = i + sum;
            i += 1;
        }
       // System.out.println(sum);

        boolean x = isValidStore("0658");
        System.out.println(x);


       double v = Math.round(99.8987 * 100.00) / 100.00;

        System.out.println(v);

    }

    private static boolean isValidStore(String storeId) {

       // StringUt
        return Integer.valueOf(storeId) > 0 || Integer.valueOf(storeId) <= 9999;
    }
    }
   //method to find strictly increasing first and decreasing
/*     static void sisd(int n, int lo, int lh){
      int[] a = {-1};
       if(n<lo+lh){
           System.out.println("sequence possible ");
          // an = a+ (n-1)*d // (lo-lh)*2 -1
           int max=lh;
           int arraysize = lh-lo;
           int[] arr = new int[arraysize+1];   //all the sequence element
           int j=0;
          for(int i=lo ; i <=lh ;i++){
                  arr[j] = i;
                   j++;
          }
           System.out.println(Arrays.toString(arr));
       }
       else{
           System.out.println(Arrays.toString(a));
       }

    }*/

