package bakery;

import bakery.observer.StockListener;
import bakery.observer.StockNotifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockNotifierTest {

    @Test
    void notifiesAtThreshold() {
        StockNotifier notifier =
                new StockNotifier(2);

        List<String> events =
                new ArrayList<>();

        StockListener listener =
                (name, stock) ->
                        events.add(
                                name + ":" + stock
                        );

        notifier.registerListener(
                "Croissant",
                listener
        );

        notifier.notifyIfLowStock(
                "Croissant",
                2
        );

        assertEquals(
                List.of("Croissant:2"),
                events
        );
    }

    @Test
    void notifiesBelowThreshold() {
        StockNotifier notifier =
                new StockNotifier(2);

        List<Integer> stocks =
                new ArrayList<>();

        notifier.registerListener(
                "Croissant",
                (name, stock) ->
                        stocks.add(stock)
        );

        notifier.notifyIfLowStock(
                "Croissant",
                1
        );

        assertEquals(
                List.of(1),
                stocks
        );
    }

    @Test
    void doesNotNotifyAboveThreshold() {
        StockNotifier notifier =
                new StockNotifier(2);

        List<Integer> stocks =
                new ArrayList<>();

        notifier.registerListener(
                "Croissant",
                (name, stock) ->
                        stocks.add(stock)
        );

        notifier.notifyIfLowStock(
                "Croissant",
                3
        );

        assertTrue(stocks.isEmpty());
    }

    @Test
    void onlyNotifiesListenersForCorrectPastry() {
        StockNotifier notifier =
                new StockNotifier(2);

        List<String> events =
                new ArrayList<>();

        notifier.registerListener(
                "Croissant",
                (name, stock) ->
                        events.add(name)
        );

        notifier.notifyIfLowStock(
                "Eclair",
                1
        );

        assertTrue(events.isEmpty());
    }

    @Test
    void removedListenerIsNotNotified() {
        StockNotifier notifier =
                new StockNotifier(2);

        List<String> events =
                new ArrayList<>();

        StockListener listener =
                (name, stock) ->
                        events.add(name);

        notifier.registerListener(
                "Croissant",
                listener
        );

        notifier.removeListener(
                "Croissant",
                listener
        );

        notifier.notifyIfLowStock(
                "Croissant",
                1
        );

        assertTrue(events.isEmpty());
    }

    @Test
    void multipleListenersAreAllNotified() {
        StockNotifier notifier =
                new StockNotifier(2);

        List<String> events =
                new ArrayList<>();

        notifier.registerListener(
                "Croissant",
                (name, stock) ->
                        events.add("A")
        );

        notifier.registerListener(
                "Croissant",
                (name, stock) ->
                        events.add("B")
        );

        notifier.notifyIfLowStock(
                "Croissant",
                2
        );

        assertEquals(2, events.size());
        assertTrue(events.contains("A"));
        assertTrue(events.contains("B"));
    }
}