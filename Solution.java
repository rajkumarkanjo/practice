package DSA;

import java.sql.SQLOutput;
import java.util.Scanner;

public class Solution {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
     /*   System.out.println("first year");
        int t = sc.nextInt();
        System.out.println("fifa year");
        int y = sc.nextInt();*/
       int t = 2 , y = 2012;
/*        int diff= (y-1950)%4;
        diff= 4- diff;
        y= y+diff;
        System.out.println(y);*/
        int copyYear = y;

      //  while(t != 0 ){
        while(y < copyYear+4){
                if (y % 4 == 2) {
                    System.out.println(y);
                    break;
                }
                y++;
            }
            t--;
        }
   // }
}
