package ComparatorComparable;

import java.util.*;

public class Sourcing {

    public static void main(String[] args) {

       // List<OrderLine> orderLineList = new ArrayList<>();

        // [ 1 , 2 ] , [2, 4] , [1, 5] , [3, 2]
        List<OrderLine> orderLine = Arrays.asList(new OrderLine("1" , 2 , ""),
                                                  new OrderLine("2" , 4 , ""),
                                                  new OrderLine("1" , 5 , ""),
                                                  new OrderLine("3" , 2 , ""));
        Sourcing.order(orderLine);
       // System.out.println(orderLine);
    }
    public static List<OrderLine>  order(List<OrderLine> orderLines){
       // System.out.println(orderLines);
        Map<String,Integer> map = new HashMap<>();

        for(int i =0 ; i<orderLines.size() ; i ++){
            if(map.containsKey(orderLines.get(i).getSku())){
              // int test=  map.get(orderLines.get(i).getSku());
               map.put(orderLines.get(i).getSku() , map.get(orderLines.get(i).getSku()) + orderLines.get(i).getQty());
            }
            else{
                map.put(orderLines.get(i).getSku() , orderLines.get(i).getQty());
            }
        }
        System.out.println(map.toString());   // outPut 1

        //_________________________________________/

       List<OrderLine>  orderLineList = new ArrayList<>();
        int q = 1;  // just for node conuter
            for(Map.Entry<String,Integer> entry : map.entrySet()){
                OrderLine  ol = new OrderLine();
                     ol.setSku(entry.getKey());
                     ol.setQty(entry.getValue());
                     ol.setNode("node" +q );
                     orderLineList.add(ol);  //adding to the list
                     q++;
            }

        System.out.println(orderLineList.toString());  // secondInput = outPut 1
        //_________________________________________/

        //List<OrderLine>  orderLineList2 = new ArrayList<>();

            // System.out.println("Yest  " + orderLineList.toString());
             for (int j = 0; j<orderLines.size() ; j++)
             {
                 for(int k= 0 ; k<orderLineList.size() ; k ++){

                     if(orderLines.get(j).getSku() == orderLineList.get(k).getSku()){
                      /*
                         OrderLine ol2 = new OrderLine();
                         ol2.setSku(orderLines.get(j).getSku());
                         ol2.setQty(orderLines.get(j).getQty());
                         ol2.setNode(orderLineList.get(k).getNode());
                         orderLineList2.add(ol2);*/
                         orderLines.get(j).setNode(orderLineList.get(k).getNode());
                         break;
                     }
                 }
             }
        System.out.println(orderLines);
     //   System.out.println(orderLineList2.toString());
        return orderLineList;
    }

}
