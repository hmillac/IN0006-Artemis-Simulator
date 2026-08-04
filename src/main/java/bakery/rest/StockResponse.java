package bakery.rest;

public class StockResponse {

    private int amount;

    public StockResponse() {
    }

    public StockResponse(int amount) {
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}