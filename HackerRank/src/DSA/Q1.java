package DSA;

/*
Given an integral number N. The task is to find the count of digits present in this number.

number of digits in N = log10(N) + 1.
 */


public class Q1 {

        public static void main(String[] args) {
            int number = 563466556;

            double n = findNumberOfDigits(number);
            System.out.println("findnNubmerOfDigits = " + n);
        }
        private static double findNumberOfDigits(int number) {

        /*    String str = String.valueOf(number);
            char [] ch = str.toCharArray();
            return ch.length;*/

            // double v = Math.log10(number) + 1;
            return Math.floor(Math.log10(number)+1);

        }

    }

