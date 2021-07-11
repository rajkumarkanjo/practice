package GOG;

public class FacingTheSun {

    public static void main(String[] args) {
        int h[] = {7, 4, 8, 2, 9};
        int n = h.length;
        int count = 0 ;

       // int h[] = {1, 2, 3, 4, 5};
     // finding the largest element in an array
/*        int MAX = h[0];
        for (int i = 0 ; i < n ; i++ ){
            if (h[i] > MAX){
                MAX = h[i];
            }
        }
        int indexOfMaxElement = Ints.in*/

        for (int i = 0 ; i<n-1 ; i++){

            if (h[n-1-i] >= h[n-1-(i+1)] ){

                count++;
            }

        }

        System.out.println("total buildig "+ (count+1) );
    }

}
