package ComparatorComparable;

import java.util.Scanner;

public class CodeVita {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the number  ");
        int n = sc.nextInt();
        double i = 0;
        double sum=0;
        for( i =1 ; i <= 3 ; i ++) {
            sum = sum + (1 + 1/i);
        }
        System.out.format("%.6f", sum);
    }
}
