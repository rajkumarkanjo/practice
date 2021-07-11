package GOG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SolutionGOG {

    public static void main(String[] args) {

      //  https://practice.geeksforgeeks.org/problems/count-smaller-elements2214/0/?track=interview-arrays&batchId=117


        List<Integer> list = new ArrayList<>();
        list.add(4);
        list.add(6);
        list.add(9);

         int arr [] = {4,67,78,23};

        Arrays.sort(arr);

        System.out.println(arr[0]+" "+arr[arr.length-1]);

        System.out.println(list.size());
        for (int i = 0 ; i <= list.size(); i++){

            System.out.println(list.get(i)+" ");
        }


    }

}
