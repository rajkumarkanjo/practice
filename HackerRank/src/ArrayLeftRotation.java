import java.util.Arrays;

/*
 a ->no of element in array
 n-> no.of rotation
 ex :- a=5 , n = 4
________________
input = 5 4
1 2 3 4 5

output = 5 1 2 3 4
__________________________
 [1,2,3,4,5] <LR1> [2,3,4,5,1] <LR2> [3,4,5,1,2] <LR3> [4,5,1,2,3] <LR4> [5,4,3,2,1]
 */
public class ArrayLeftRotation {

    public static void main(String[] args) {

/*        int inputArray[] = {1,2,3,4,5};
        int numberOfRotation = 2;
        System.out.println("input "+Arrays.toString(inputArray) + " ,rotation = " + numberOfRotation);

        int dummy[] = new int[inputArray.length];

        int firstElement = inputArray[0];
        int lastElement = inputArray[inputArray.length-1];
            for(int j = 0 ; j < numberOfRotation ; j++){

                for(int i = 0 ; i < inputArray.length ; i++){
                    dummy[0] = lastElement;
                    dummy[i] = inputArray[i];
                    dummy[dummy.length-1] = firstElement;
            }
                firstElement = dummy[0];
                lastElement =dummy[dummy.length-1];
                System.out.println(Arrays.toString(dummy));
        }*/

          int array [] = {1,2,3,5};
          int n = 5;
            // Your Code Here
            int totalSeriesSum = 0 ;
            int missingNumber = 0 ;
            int a1 = array[0];
            int al = array[array.length - 1];

        System.out.println(a1 +" "+al);

            totalSeriesSum = (n*(n+1)/2);

           System.out.println(totalSeriesSum);

            int givenSeriesSum = 0;

            for(int i = 0 ; i < array.length ; i++){

                givenSeriesSum = givenSeriesSum + array[i];

            }

            missingNumber = totalSeriesSum - givenSeriesSum;
            System.out.println(missingNumber);

    }

}
