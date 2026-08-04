package bakery;

import bakery.model.Pastry;
import bakery.observer.StockNotifier;
import bakery.repository.DatabaseAdapter;
import bakery.rest.BuyResponse;
import bakery.service.BakeryServiceImpl;
import bakery.service.PaymentService;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BakeryEasyMockTest {

    @Test
    void executeBuySucceedsWithCorrectInteractions() {
        DatabaseAdapter database =
                EasyMock.createMock(
                        DatabaseAdapter.class
                );

        PaymentService payment =
                EasyMock.createMock(
                        PaymentService.class
                );

        Pastry beforePurchase =
                new Pastry(5, 2.5);

        Pastry afterPurchase =
                new Pastry(4, 2.5);

        // prepare()
        EasyMock.expect(
                database.getPastry(
                        "Croissant"
                )
        ).andReturn(beforePurchase);

        // commit()
        EasyMock.expect(
                database.getPastry(
                        "Croissant"
                )
        ).andReturn(beforePurchase);

        EasyMock.expect(
                payment.processPayment(
                        "Croissant",
                        2.5
                )
        ).andReturn(true);

        EasyMock.expect(
                database.buyPastry(
                        "Croissant"
                )
        ).andReturn(true);

        // Observer check after successful purchase
        EasyMock.expect(
                database.getPastry(
                        "Croissant"
                )
        ).andReturn(afterPurchase);

        EasyMock.replay(
                database,
                payment
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        payment,
                        new StockNotifier(2)
                );

        BuyResponse response =
                service.executeBuy(
                        "Croissant"
                );

        assertTrue(response.isSuccess());

        assertEquals(
                "Purchase successful.",
                response.getMessage()
        );

        EasyMock.verify(
                database,
                payment
        );
    }

    @Test
    void executeBuyFailsForUnknownPastryWithoutPayment() {
        DatabaseAdapter database =
                EasyMock.createMock(
                        DatabaseAdapter.class
                );

        PaymentService payment =
                EasyMock.createMock(
                        PaymentService.class
                );

        EasyMock.expect(
                database.getPastry(
                        "Unknown"
                )
        ).andReturn(null);

        /*
         * No expectation is registered for
         * payment.processPayment().
         *
         * If the service calls it, EasyMock fails.
         */

        EasyMock.replay(
                database,
                payment
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        payment,
                        new StockNotifier(2)
                );

        BuyResponse response =
                service.executeBuy(
                        "Unknown"
                );

        assertFalse(response.isSuccess());

        assertEquals(
                "Pastry unavailable.",
                response.getMessage()
        );

        EasyMock.verify(
                database,
                payment
        );
    }

    @Test
    void executeBuyFailsForOutOfStockWithoutPayment() {
        DatabaseAdapter database =
                EasyMock.createMock(
                        DatabaseAdapter.class
                );

        PaymentService payment =
                EasyMock.createMock(
                        PaymentService.class
                );

        Pastry empty =
                new Pastry(0, 2.5);

        EasyMock.expect(
                database.getPastry(
                        "Croissant"
                )
        ).andReturn(empty);

        /*
         * processPayment() and buyPastry()
         * must never be called.
         */

        EasyMock.replay(
                database,
                payment
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        payment,
                        new StockNotifier(2)
                );

        BuyResponse response =
                service.executeBuy(
                        "Croissant"
                );

        assertFalse(response.isSuccess());

        EasyMock.verify(
                database,
                payment
        );
    }

    @Test
    void failedPaymentDoesNotCallBuyPastry() {
        DatabaseAdapter database =
                EasyMock.createMock(
                        DatabaseAdapter.class
                );

        PaymentService payment =
                EasyMock.createMock(
                        PaymentService.class
                );

        Pastry pastry =
                new Pastry(5, 2.5);

        // prepare()
        EasyMock.expect(
                database.getPastry(
                        "Croissant"
                )
        ).andReturn(pastry);

        // commit()
        EasyMock.expect(
                database.getPastry(
                        "Croissant"
                )
        ).andReturn(pastry);

        EasyMock.expect(
                payment.processPayment(
                        "Croissant",
                        2.5
                )
        ).andReturn(false);

        /*
         * No expectation for buyPastry().
         * Therefore it must not be called.
         */

        EasyMock.replay(
                database,
                payment
        );

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        payment,
                        new StockNotifier(2)
                );

        BuyResponse response =
                service.executeBuy(
                        "Croissant"
                );

        assertFalse(response.isSuccess());

        assertEquals(
                "Payment failed.",
                response.getMessage()
        );

        EasyMock.verify(
                database,
                payment
        );
    }

    @Test
    void failedPaymentKeepsRealStockUnchanged() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                5,
                2.5
        );

        PaymentService payment =
                EasyMock.createMock(
                        PaymentService.class
                );

        EasyMock.expect(
                payment.processPayment(
                        "Croissant",
                        2.5
                )
        ).andReturn(false);

        EasyMock.replay(payment);

        BakeryServiceImpl service =
                new BakeryServiceImpl(
                        database,
                        payment,
                        new StockNotifier(2)
                );

        BuyResponse response =
                service.executeBuy(
                        "Croissant"
                );

        assertFalse(response.isSuccess());

        assertEquals(
                5,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );

        EasyMock.verify(payment);
    }
}