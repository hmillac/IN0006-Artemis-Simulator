package bakery;

import bakery.observer.StockNotifier;
import bakery.repository.DatabaseAdapter;
import bakery.rest.AuthRequest;
import bakery.rest.BuyRequest;
import bakery.service.BakeryServiceImpl;
import bakery.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BakeryServiceTest {

    private DatabaseAdapter database;
    private BakeryServiceImpl service;

    @BeforeEach
    void setUp() {
        database = new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                5,
                2.5
        );

        database.stockPastries(
                "Last",
                1,
                3.0
        );

        database.stockPastries(
                "Empty",
                0,
                2.0
        );

        PaymentService payment =
                (name, price) -> true;

        service = new BakeryServiceImpl(
                database,
                payment,
                new StockNotifier(2)
        );
    }

    @Test
    void queryStockReturnsCurrentAmount() {
        assertEquals(
                5,
                service.queryStock(
                        "Croissant"
                ).getAmount()
        );
    }

    @Test
    void queryStockReturnsMinusOneForUnknown() {
        assertEquals(
                -1,
                service.queryStock(
                        "Unknown"
                ).getAmount()
        );
    }

    @Test
    void prepareLocksValidPastry() {
        assertTrue(
                service.prepare(
                        "Croissant"
                )
        );

        assertFalse(
                service.prepare(
                        "Croissant"
                )
        );
    }

    @Test
    void abortReleasesLock() {
        assertTrue(
                service.prepare(
                        "Croissant"
                )
        );

        service.abort("Croissant");

        assertTrue(
                service.prepare(
                        "Croissant"
                )
        );
    }

    @Test
    void successfulBuyReducesStock() {
        assertTrue(
                service.executeBuy(
                        "Croissant"
                ).isSuccess()
        );

        assertEquals(
                4,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );
    }

    @Test
    void paymentFailureDoesNotReduceStock() {
        PaymentService failedPayment =
                (name, price) -> false;

        BakeryServiceImpl failingService =
                new BakeryServiceImpl(
                        database,
                        failedPayment,
                        new StockNotifier(2)
                );

        assertFalse(
                failingService.executeBuy(
                        "Croissant"
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
    void buyRequestDelegatesToPurchaseWorkflow() {
        BuyRequest request =
                new BuyRequest(
                        "Croissant"
                );

        assertTrue(
                service.buy(request)
                        .isSuccess()
        );

        assertEquals(
                4,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );
    }

    @Test
    void successfulPurchaseNotifiesAtThreshold() {
        DatabaseAdapter localDatabase =
                new DatabaseAdapter();

        localDatabase.stockPastries(
                "Croissant",
                3,
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

        BakeryServiceImpl localService =
                new BakeryServiceImpl(
                        localDatabase,
                        (name, price) -> true,
                        notifier
                );

        localService.executeBuy(
                "Croissant"
        );

        assertEquals(
                List.of(2),
                notifications
        );
    }

    @Test
    void authenticationUsesExactSuccessMessage() {
        database.registerClient(
                "alice",
                "secret"
        );

        var response =
                service.authenticate(
                        new AuthRequest(
                                "alice",
                                "secret"
                        )
                );

        assertTrue(response.isSuccess());

        assertEquals(
                "Authentication successful.",
                response.getMessage()
        );
    }

    @Test
    void authenticationUsesExactFailureMessage() {
        var response =
                service.authenticate(
                        new AuthRequest(
                                "alice",
                                "wrong"
                        )
                );

        assertFalse(response.isSuccess());

        assertEquals(
                "Invalid username or password.",
                response.getMessage()
        );
    }
}