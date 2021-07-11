package GOG;

public class MinimumDistance {

    public static void main(String[] args) {
   //     minDist();
    }

    int minDist(int a[], int n, int x, int y) {
        // code here
        // minimum distance = length of the array
        int minDistance = n, ptrX = 2*n, ptrY = 2*n;
        for(int i = 0; i < n; i++) {
            if(a[i] == x) {
                ptrX = i;
                minDistance = Math.min(minDistance, Math.abs(ptrX-ptrY));
            }
            else if(a[i] == y) {
                ptrY = i;
                minDistance = Math.min(minDistance, Math.abs(ptrY-ptrX));
            }
        }
        return minDistance == n ? -1 : minDistance;
    }
}
