import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class InfyTQ {

    public static int CntPairs(int N , String S){

        // Step 1 : convert binary to decimal
      //  int decimal=Integer.parseInt(S,2);


        int decimal = 300000;
        int count = 0 ;


        BigDecimal val = new BigDecimal(0);  // Succeeds

        int i = 1;
        while (i <= decimal ){

            for(int j = 1 ; j <= decimal ; j++){

                    if ((i+j) == (i^j) ){

                       count = count+1;
                   }
            }
            i++;
        }
        return  count % (1000000007) ;

    }

    public static void main(String[] args) {

        int result = CntPairs(10,"100000000000");
        System.out.println("number of pairs ="+ result);

    }


}
