package ComparatorComparable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ComparatorLaptop implements Comparator<Laptop> {

    public static void main(String[] args) {
        List<Laptop> laptops = Arrays.asList(new Laptop(1,"HP",5000),
                new Laptop(2, "Dell", 75000),
                new Laptop(3, "Lenevo", 45000),
                new Laptop(4, "Mac", 95000));

        System.out.println(laptops);
        Collections.sort(laptops, new ComparatorLaptop());
        System.out.println("**After Sort**"+laptops);

        int arr[] = new int[5];
        System.out.println(arr);

    }
    @Override
    public int compare(Laptop l1, Laptop l2) {
        if(l1.getPrice() > l2.getPrice()){
            return 1;
        }else if (l1.getPrice()<l2.getPrice()){
            return -1;
        }
        else{
            return 0;
        }
    }


}
