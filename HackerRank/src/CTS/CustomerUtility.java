package CTS;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * /*  BlackForest   -- 500 / kg
 *     Chocolate     -- 450 /kg
 *     RedVelvet     -- 650 /kg
 */
public class CustomerUtility {


    private Customer[] customerArray;

    public Customer[] getCustomerArray() {
        return customerArray;
    }

    public void setCustomerArray(Customer[] customerArray) {
        this.customerArray = customerArray;
    }

    public String[] calculateTotalBill(){

          Customer customer;
          Customer[] customersInputValidArray = getCustomerArray();
         String[]  finalBill = new String[customersInputValidArray.length];

          for (int i = 0 ; i < customersInputValidArray.length ; i++ ){

                    customer = customersInputValidArray[i];
                     double amountforcake=0.0;
                    String custName = customer.getCustomerName();
                    String pinCode = customer.getPinCode();
                    String cakeName = customer.getCakeType();
                    String cakeQty = customer.getQuantity();

                    if (cakeName.equalsIgnoreCase("BlackForest")){
                        amountforcake = 500* Integer.parseInt(cakeQty);

                    }else if (cakeName.equalsIgnoreCase("Chocolate")){
                        amountforcake = 450* Integer.parseInt(cakeQty);
                    }else{
                        amountforcake = 650* Integer.parseInt(cakeQty);
                    }
                    String bill =  custName+":"+pinCode+":"+cakeName+":"+cakeQty+":"+amountforcake;
                    finalBill[i] = bill;
          }
           return finalBill;
    }

    public void addValidCustomerOrders(String[] records){

        Customer[] customers = new Customer[records.length];

        for (int i = 0 ; i< records.length; i ++){

            String inputData = records[i];

            String [] splitData = inputData.split(":");
            String cusTName = splitData[0];
            String pinCode = splitData[1];
            String cakeName = splitData[2];
            String CakeQty = splitData[3];

            if (CustomerUtility.isValidPinCode(pinCode) == true){

                Customer customer = new Customer(cusTName ,pinCode ,cakeName,CakeQty );
                customers[i] = customer;
            }else{
                System.out.println("No Valid customer order found");
            }
        }
        setCustomerArray(customers);
    }
    public static boolean isValidPinCode(String pinCode)
    {
        String regex = "^[1-9]{1}[0-9]{2}\\s{0,1}[0-9]{3}$";

        Pattern p = Pattern.compile(regex);

        if (pinCode == null) {
            return false;
        }
        Matcher m = p.matcher(pinCode);
        return m.matches();
    }

}
