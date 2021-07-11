package CTS;

public class Customer {

    private String customerName;
    private String pinCode;
    private String cakeType;
    private String quantity;


    public Customer(String customerName, String pinCode, String cakeType, String quantity) {
        this.customerName = customerName;
        this.pinCode = pinCode;
        this.cakeType = cakeType;
        this.quantity = quantity;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getCakeType() {
        return cakeType;
    }

    public void setCakeType(String cakeType) {
        this.cakeType = cakeType;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }
}
