package bakery.model;

public class Pastry {

    private int stock;
    private final double price;

    public Pastry(int stock, double price) {
        this.stock = stock;
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public double getPrice() {
        return price;
    }

    public boolean buyOne() {
        if (stock <= 0) {
            return false;
        }

        stock--;
        return true;
    }
}