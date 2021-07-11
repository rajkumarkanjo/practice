package GOG;

/*
print first letter of every word in a String
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PFLOEW {

    public static void main(String[] args) {

        String str = "my name is raj";
        String [] s=  str.split(" ");

       for (int i = 0 ; i < s.length ; i ++){
           String strng = s[i];
           System.out.println(strng.charAt(0));
       }
    }
}
