package bakery;

import bakery.observer.StockListener;
import bakery.observer.StockNotifier;
import bakery.repository.DatabaseAdapter;
import bakery.rest.AuthRequest;
import bakery.service.BakeryServiceImpl;
import bakery.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BakeryHiddenCasesTest {

    @Test
    void existingPastryWithZeroStockReturnsZeroNotMinusOne() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Empty",
                0,
                2.0
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        (name, price) -> true,
                        new StockNotifier(2)
                );

        assertEquals(
                0,
                service.queryStock("Empty")
                        .getAmount()
        );
    }

    @Test
    void failedPrepareDoesNotLeavePastryLocked() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                0,
                2.5
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        (name, price) -> true,
                        new StockNotifier(2)
                );

        assertFalse(
                service.prepare("Croissant")
        );

        /*
         * Simulates restocking.
         * stockPastries replaces the stored Pastry.
         */
        database.stockPastries(
                "Croissant",
                3,
                2.5
        );

        assertTrue(
                service.prepare("Croissant")
        );
    }

    @Test
    void failedPaymentReleasesLockForNextPurchase() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                2,
                2.5
        );

        AtomicInteger attempts =
                new AtomicInteger();

        PaymentService payment =
                (name, price) ->
                        attempts.incrementAndGet() > 1;

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        payment,
                        new StockNotifier(1)
                );

        assertFalse(
                service.executeBuy(
                        "Croissant"
                ).isSuccess()
        );

        assertEquals(
                2,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );

        /*
         * The second attempt must not fail
         * because of a stale lock.
         */
        assertTrue(
                service.executeBuy(
                        "Croissant"
                ).isSuccess()
        );

        assertEquals(
                1,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );
    }

    @Test
    void repeatedPurchasesStopExactlyAtZero() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                2,
                2.5
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        (name, price) -> true,
                        new StockNotifier(1)
                );

        assertTrue(
                service.executeBuy(
                        "Croissant"
                ).isSuccess()
        );

        assertTrue(
                service.executeBuy(
                        "Croissant"
                ).isSuccess()
        );

        assertFalse(
                service.executeBuy(
                        "Croissant"
                ).isSuccess()
        );

        assertEquals(
                0,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );
    }

    @Test
    void successfulLastPurchaseNotifiesWithZeroStock() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Last",
                1,
                3.0
        );

        List<Integer> notifications =
                new ArrayList<>();

        StockNotifier notifier =
                new StockNotifier(1);

        notifier.registerListener(
                "Last",
                (name, stock) ->
                        notifications.add(stock)
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        (name, price) -> true,
                        notifier
                );

        assertTrue(
                service.executeBuy(
                        "Last"
                ).isSuccess()
        );

        assertEquals(
                List.of(0),
                notifications
        );
    }

    @Test
    void failedPaymentDoesNotNotifyListener() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                2,
                2.5
        );

        List<Integer> notifications =
                new ArrayList<>();

        StockNotifier notifier =
                new StockNotifier(2);

        notifier.registerListener(
                "Croissant",
                (name, stock) ->
                        notifications.add(stock)
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        (name, price) -> false,
                        notifier
                );

        assertFalse(
                service.executeBuy(
                        "Croissant"
                ).isSuccess()
        );

        assertTrue(
                notifications.isEmpty()
        );

        assertEquals(
                2,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );
    }

    @Test
    void removedAndRegisteredAgainListenerReceivesOneNotification() {
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

        notifier.removeListener(
                "Croissant",
                listener
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
    void unknownPurchaseDoesNotModifyKnownPastry() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                5,
                2.5
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        (name, price) -> true,
                        new StockNotifier(2)
                );

        assertFalse(
                service.executeBuy(
                        "Unknown"
                ).isSuccess()
        );

        assertEquals(
                5,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );
    }

    @Test
    void exactAuthenticationMessagesRemainStable() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.registerClient(
                "alice",
                "secret"
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        (name, price) -> true,
                        new StockNotifier(2)
                );

        var success =
                service.authenticate(
                        new AuthRequest(
                                "alice",
                                "secret"
                        )
                );

        var failure =
                service.authenticate(
                        new AuthRequest(
                                "alice",
                                "wrong"
                        )
                );

        assertAll(
                () -> assertTrue(
                        success.isSuccess()
                ),
                () -> assertEquals(
                        "Authentication successful.",
                        success.getMessage()
                ),
                () -> assertFalse(
                        failure.isSuccess()
                ),
                () -> assertEquals(
                        "Invalid username or password.",
                        failure.getMessage()
                )
        );
    }
}