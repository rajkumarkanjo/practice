package IKM;


import java.util.*;

/*
1344 1313
2181 2181
 */

public class KittenTest {

    public static void main (String args[]) {
        Scanner sc = new Scanner(System.in);

     //   long t1 = System.currentTimeMillis();

        while (true) {

            String line = sc.nextLine();
            if ((line.trim()).length() == 1 && Integer.parseInt(line) == 0)
                break;

            String [] strArray = line.split(" ");

            int [] inputArray = new int[strArray.length];
            int arraySum = 0;
                 int sum = 0;
                 int offset = 0 ;
            for (int i = 1 ; i < inputArray.length ; i++){

                inputArray[i] = Integer.parseInt(strArray[i]);
                arraySum = arraySum + inputArray[i];
            }
            ///
                long yt2 = System.currentTimeMillis();
               Set<Integer> listOfAllPossibleSum = KittenTest.sumsAll(sum , offset , inputArray);


               // System.out.println("kiiiiiiiiii"+(System.currentTimeMillis() - yt2)/1000);



           // Set<Integer> listOfAllPossibleSum = KittenTest.subsetSums(inputArray , inputArray.length);

            int totalSum = arraySum;

            int kattisBagWeight = 0;
            int properWeightbalanse = totalSum/2;
            //  System.out.println("properWeightbalanse  "+properWeightbalanse);

            while(true){

                boolean isPerfectWeightBalanse =  listOfAllPossibleSum.contains(properWeightbalanse);

                if (isPerfectWeightBalanse == true)
                {
                    kattisBagWeight = properWeightbalanse;
                    break;
                }
                properWeightbalanse++;

            }
            int kittensBagWeight = totalSum - kattisBagWeight;

            System.out.println(kattisBagWeight+" "+kittensBagWeight );

         //   System.out.println((System.currentTimeMillis() - t1)/1000);
            ///
        }
    }
    // to find all the possible sum


 /*   public  static Set<Integer> subsetSums(int arr[], int n)
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
    }*/

    public static Set<Integer> sumsAll(int sum, int offset, int[] array) {

    //    long t1 = System.currentTimeMillis();

        Set<Integer> sums = new HashSet<Integer>(array.length - offset);

        for (int i = offset; i < array.length; ++i) {
            int total = sum + array[i];

            sums.add(total);
            sums.addAll(sumsAll(total, i + 1, array));
        }
        return sums;
    }

}




