package GOG;

/*
Input: 2
Output:
2 2 1 1 $2 1 $
 */

public class PrintThePattern {

    public static void main(String[] args) {

         int n = 3;    // input value
         printPat(n);
    }

     static void printPat(int n) {

             for (int i = 1; i <= n; i++) {
                 if (i <= n) {
                     System.out.print(n + " ");
                 }
             }
         }
}
