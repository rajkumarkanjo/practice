package IKM;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/*
8 529 382 130 462 223 167 235 529
12 528 129 376 504 543 363 213 138 206 440 504 418
0

 */

public class Kitten2 {


        //static Map<Integer, Integer> map;

        public static void main(String[] args)  {

            Scanner sc = new Scanner(System.in);

            System.out.println("Enter the value ");
          //  int line = sc.nextInt();    // number of line in the input

            List<Integer>  list1 = new ArrayList<>();
            List<Integer>  list2 = new ArrayList<>();

           int sum1 = 0 , sum2 = 0 ;
            while (sc.nextInt() !=0){

                        String s = sc.nextLine();
                        String[] str = s.split(" ");

                         int [] inputArray = new int[str.length];

                        for (int i = 1 ; i < inputArray.length ; i++){

                                inputArray[i] = Integer.parseInt(str[i]);
                        }

                        sum1 = sum1 + inputArray[0];
                        sum2 = sum2 + inputArray[1];

                        for (int i = 2 ; i < inputArray.length  ; i++){




                        }


                        for (int i = 0 ; i < inputArray.length ; i++ ){

                            for (int j = 1 ; j < inputArray.length ; j++) {

                                if (inputArray[i] < inputArray[j]) {

                                    sum2 = sum2+ inputArray[j];

                                }else {

                                    sum1 = sum1 + inputArray[i];
                                    list1.add(inputArray[j]);
                                }
                            }

                        }




            }

        }
    }


