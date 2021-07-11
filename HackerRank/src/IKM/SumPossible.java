package IKM;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SumPossible {

    public static void main(String[] args) {


        int [] arr = new int[8];
        arr[0] = 529 ; arr[1] = 382 ; arr[2] = 130 ;arr[3] = 462 ;  arr[4] = 223 ;  arr[5] = 167 ;  arr[6] = 235 ;  arr[7] = 529 ;

        int n = 2;
        int sum = 0 ;

        Set<Integer> listOfAllPossibleSum = SumPossible.sumsAll(sum , n , arr);
      //  System.out.println(list);

        int divisor = 2 ;
        int totalSum = 2657;

        int kattisBagWeight = 0;
        int properWeightbalanse = (int)Math.ceil(totalSum / divisor);
        System.out.println(properWeightbalanse);


        while(true){

            boolean isPerfectWeightBalanse =  listOfAllPossibleSum.contains(properWeightbalanse);

            if (isPerfectWeightBalanse == true)
            {
                kattisBagWeight = properWeightbalanse;
                break;
            }
            properWeightbalanse++;
        }

        int kittensBagWeight = 2675 - kattisBagWeight;

        System.out.println(kattisBagWeight +" "+kittensBagWeight );


    }

    public static Set<Integer> sumsAll(int sum, int offset, int[] array) {

        long startTime = System.currentTimeMillis();

        System.out.println(startTime);

        Set<Integer> sums = new HashSet<Integer>(array.length - offset);

        for (int i = offset; i < array.length; ++i) {
            int total = sum + array[i];

            sums.add(total);
            sums.addAll(sumsAll(total, i + 1, array));
        }
        long endtTime = System.currentTimeMillis();

        System.out.println("time taken this method="+ (startTime-endtTime));

        return sums;
    }

}
