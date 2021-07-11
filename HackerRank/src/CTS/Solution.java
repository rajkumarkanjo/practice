package CTS;

import sun.jvm.hotspot.jdi.SACoreAttachingConnector;

import java.util.Arrays;
import java.util.Scanner;

public class Solution {

    public static void main(String[] args) {

       // Scanner sc = new Scanner(System.in);

        System.out.println("Enter the number of customer orders");
        int custOrder = 1;//sc.nextInt();

        System.out.println("Enter the order details");
        String[] customerArray = new String[custOrder];

        int i = 0;
        while (custOrder-- != 0) {

            String custname =  "Nancy";//   sc.nextLine();
            String pincode =    "154325";      //sc.nextLine();
            String cakename =  "BlackForest";   //sc.nextLine();
            String cakeQty =    "2";             //sc.nextLine();

            String strConcatAll = custname+":"+pincode+":"+cakename+":"+cakeQty;

            customerArray[i] = strConcatAll;

            i++;
            System.out.println(Arrays.toString(customerArray));
        }
        CustomerUtility customerUtility = new CustomerUtility();
        customerUtility.addValidCustomerOrders(customerArray);
        String[] billAmount = customerUtility.calculateTotalBill();
        for (String s : billAmount){
            System.out.println(s);
        }
  }
}