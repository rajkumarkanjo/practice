package IKM;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Scanner;

public class Peanuts {


    public static void main(String[] args) {


        Scanner sc = new Scanner(System.in);

        int t = sc.nextInt();

        while (t-- != 0){


            String line = sc.nextLine();
            String [] strArray = line.split(" ");

            double x1 =Double.parseDouble(strArray[0]);

            System.out.println(x1);
            BigDecimal bdx1 =new BigDecimal(x1);

            double y1 =Double.parseDouble(strArray[1]);
            BigDecimal bdy1 =new BigDecimal(y1);


            double x2 =Double.parseDouble(strArray[2]);
            BigDecimal bdx2 =new BigDecimal(x2);


            double y2 =Double.parseDouble(strArray[3]);
            BigDecimal bdy2 =new BigDecimal(y2);

            String pSize = strArray[4];


            System.out.println(bdx1+" "+bdy1+" "+bdx2+" "+bdy2+" "+pSize);


        }
    }

}
