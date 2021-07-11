package DSA;

public class Count1s {


    public static void main(String[] args) {

        int count = 0 ;

        // you can write the code the take input using scanner other class
        String input = "0000 1111";
        input.trim();

        char[] ch = input.toCharArray();

          for (int i = 0 ; i< ch.length ; i++ ){

              char a = ch[i];
              if (a == '1'){
                  count++;
              }
          }
        System.out.println(count);
    }

}
