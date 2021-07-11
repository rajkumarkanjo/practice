package IKM;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/*

8 529 382 130 462 223 167 235 529
12 528 129 376 504 543 363 213 138 206 440 504 418
0

 */

public class Kitten {

    static Map<Integer, Integer> map;

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);
        map = new HashMap<>();

        String st = sc.nextLine();

        while (true) {

            if ((st.trim()).length() == 1 && Integer.parseInt(st) == 0)
                break;

            String[] c = st.split(" ");

            int to = Integer.parseInt(c[0]);
            if (to == -1) {
                break;
            }
            for (int i = 1; i < c.length; i++) {
                map.put(Integer.parseInt(c[i]), to);
            }
        }
        dfs(Integer.parseInt(st));
    }

    static void dfs(int s) {
        System.out.print(s + " ");
        if (map.containsKey(s)) {
            dfs(map.get(s));
        }
    }

}











