package IKM;

import java.util.HashSet;
import java.util.Set;

public class SubArraySum {


    public static void main(String[] args) {


        int [] arr = new int[8];
        arr[0] = 529 ; arr[1] = 382 ; arr[2] = 130 ;arr[3] = 462 ;  arr[4] = 223 ;  arr[5] = 167 ;  arr[6] = 235 ;  arr[7] = 529 ;

        Set<Integer> listOfAllPossibleSum = SubArraySum.subsetSums(arr , arr.length);

       // int a =  SubArraySum.subarraySum(arr);



     //   System.out.println(a);

        System.out.println(listOfAllPossibleSum);

    }

    public  static Set<Integer> subsetSums(int arr[], int n)
    {
        Set<Integer> sums = new HashSet<Integer>(arr.length);
        int total = 1 << n;
        for(int i = 0; i < total; i++)
        {
            int sum= 0;
            for(int j = 0; j < n; j++)
                if ((i & (1 << j)) != 0)
                    sums.add(sum += arr[j]);
        }
        return sums;
    }


 /*   public static int subarraySum(int[] arr) {
        int result = 0;
        for (int i = 0; i < arr.length; i++)  {
            result += arr[i] * (i + 1) * (arr.length - i);

            System.out.println(result);
        }
        return result;
    }*/

}
