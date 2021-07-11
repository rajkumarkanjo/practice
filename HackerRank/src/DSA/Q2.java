package DSA;

import java.util.*;

public class Q2 {

    public static void main(String[] args) {

        int arr [] = {1 ,3, 4, 5, 6, 12 ,13, 17, 19 ,22 ,23, 25, 27, 28, 28, 35, 36, 37 ,39 ,43 ,46 ,48, 54,
                59 ,62 ,63 ,65, 68, 68, 70 ,70 ,72 ,79 ,82 ,83 ,92 ,92 ,93 ,95, 96 ,96 ,100};
        int arr_size = arr.length;

        Set<Integer> num = new HashSet<>();

        for(int i = 0 ; i < arr_size ; i++){

            num.add(arr[i]);

        }
        List<Integer> list = new ArrayList<>(num);
        Collections.sort(list);
          int A [] = new int[num.size()];
          int i =0 ;
            for(Integer x : list){
                A[i++] = x;
            }

        System.out.println(Arrays.toString(A));

    }
}

