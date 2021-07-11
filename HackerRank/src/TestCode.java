import java.util.Collections;
import java.util.LinkedList;

public class TestCode {

    public static void main(String[] args) {
        System.out.println("hello");

        int result=0;
        LinkedList<Integer> list1 = new LinkedList<>();
        list1.add(2); list1.add(4); list1.add(3);

        Collections.reverse(list1);

        //converting a list to a integer
        int temp1 = 0;
        for (int i : list1) {

            temp1 = (10 * temp1) + i;   //left to Right Shift (10*x)

        }
        System.out.println(temp1);  //342

        LinkedList<Integer> list2 = new LinkedList<>();
        list2.add(5);list2.add(6);list2.add(4);

        Collections.reverse(list2);

        int temp2 = 0;
        for (int i : list2) {

            temp2 = (10 * temp2) + i;

        }
        System.out.println(temp1);  //465

        result = temp1+temp2;

        System.out.println(result); //807

        //reverse an integer -1
/*
        String res = String.valueOf(result);

          StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append(res);
          stringBuilder.reverse();
          System.out.println(stringBuilder);*/

         int reverse=0;
         while(result!=0){
            // reverse = reverse*10 +
             reverse = reverse*10 + result%10;
             result = result/10;
         }
        System.out.println(reverse);

        int mask = 0x000F;
        int value = 0x2222;
        System.out.println(value & mask);
    }
}
