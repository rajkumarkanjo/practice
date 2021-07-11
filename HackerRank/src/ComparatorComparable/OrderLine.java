package ComparatorComparable;

public class OrderLine {

   private String sku;
   private int qty;
   private String node ;

    public OrderLine() {

    }

    public OrderLine(String sku, int qty, String node) {
        this.sku = sku;
        this.qty = qty;
        this.node = node;
    }

    public String getSku() {
        return sku;
    }

    public int getQty() {
        return qty;
    }

    public String getNode() {
        return node;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public void setNode(String node) {
        this.node = node;
    }

    @Override
    public String toString() {
        return "OrderLine{" +
                "sku='" + sku + '\'' +
                ", qty=" + qty +
                ", node='" + node + '\'' +
                '}';
    }
}
