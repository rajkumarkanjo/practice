package GOG;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/*
Remove Duplicates from unsorted array
Input:
1
6
1 2 3 1 4 2
Output:
1 2 3 4
 */
public class RemoveDuplicate {

    public static void main(String[] args) {

        int n = 5;
        int arr[] = {2, 3, 1, 2, 3};

        ArrayList<Integer> list=  duplicates(arr, n);
        if(list.size() == 0)
        for(Integer i : list){
            System.out.print(i+" ");
        }

    }

    public static ArrayList<Integer> duplicates(int arr[], int n) {
        // code here
        for(int i=0;i<n;i++){

            int index = arr[i] % n;
            arr[index] = arr[index] + n;
        }
        ArrayList<Integer> res = new ArrayList<Integer>();
        for(int i=0;i<n;i++) {
            if(arr[i]/n > 1){
                res.add(i);
            }
        }
        if(res.size()==0)
            res.add(-1);
        return res;
    }
}
