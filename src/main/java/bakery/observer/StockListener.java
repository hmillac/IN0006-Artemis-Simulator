package bakery.observer;

public interface StockListener {

    void onLowStock(
            String pastryName,
            int remainingStock
    );
}