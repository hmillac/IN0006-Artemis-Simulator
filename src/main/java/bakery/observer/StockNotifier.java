package bakery.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StockNotifier {

    private final int threshold;

    private final Map<String, List<StockListener>>
            listeners = new ConcurrentHashMap<>();

    public StockNotifier(int threshold) {
        this.threshold = threshold;
    }

    public void registerListener(
            String pastryName,
            StockListener listener
    ) {
        listeners
                .computeIfAbsent(
                        pastryName,
                        key -> new CopyOnWriteArrayList<>()
                )
                .add(listener);
    }

    public void removeListener(
            String pastryName,
            StockListener listener
    ) {
        List<StockListener> list =
                listeners.get(pastryName);

        if (list == null) {
            return;
        }

        list.remove(listener);

        if (list.isEmpty()) {
            listeners.remove(pastryName, list);
        }
    }

    public void notifyIfLowStock(
            String pastryName,
            int remainingStock
    ) {
        if (remainingStock > threshold) {
            return;
        }

        for (StockListener listener :
                listeners.getOrDefault(
                        pastryName,
                        List.of()
                )) {

            listener.onLowStock(
                    pastryName,
                    remainingStock
            );
        }
    }
}