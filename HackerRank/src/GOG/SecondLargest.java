package GOG;

import java.util.*;

public class SecondLargest {

    public static void main(String[] args) {

      //  int arr [] = {10, 5 , 7 , 9};
      int arr [] = {28, 43, 26, 65, 60, 54, 51, 35, 42, 48, 33, 40, 58, 38, 64, 47, 44, 49, 46 ,25 ,57 ,39 ,55 ,45 ,29 ,
                32 ,61, 53, 31, 36, 56, 63, 30, 52, 27, 34, 50 ,41 ,37 ,66, 62, 59 };
       int n = print2largest(arr);
       System.out.println("SecondLargest "+n);

    }
    private static int print2largest(int [] array ) {

        System.out.println(array.length);
        Set<Integer> set = new HashSet<Integer>();

/*        List<Integer> list1 = new ArrayList<>();
        for(int i =0 ; i < array.length ; i++){

            list1.add(array[i]);
        }
        Collections.reverse(list1);
        System.out.println(list1 + " *");*/

        for(int i = 0 ; i<array.length ; i++){
            set.add(array[i]);
        }
        System.out.println(set);
        List<Integer> list = new ArrayList<>(set);
        Collections.sort(list);
        Collections.reverse(list);
        int z = list.get(1);

        if(list.size()  > 1)
          return list.get(1);
        else
        return -1;
    }

}
