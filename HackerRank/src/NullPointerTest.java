import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class NullPointerTest {

    public static void main(String[] args) {


        List<Integer> list = new ArrayList<>();
    /*    list.add(404);
        list.add(404);*/

         List<Integer> onlyOkDtos = list.stream()
                .filter(inventory -> {
                    Boolean isOk = inventory >= 200 && inventory < 300;
                    if (!isOk) {
                        System.out.println("YIH return error for following inventory item: {}. Skipping it." + inventory );
                    }
                    return isOk;
                })
                .collect(toList());

        // convert YIH items to our items
        System.out.println(onlyOkDtos);

         List<Integer> items = onlyOkDtos.stream()
                .collect(toList());

        System.out.println(items);
    }
}
