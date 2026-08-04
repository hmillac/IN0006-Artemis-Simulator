package bakery;

import bakery.model.Pastry;
import bakery.repository.DatabaseAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseAdapterTest {

    @Test
    void getPastryReturnsStoredPastry() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                5,
                2.5
        );

        Pastry pastry =
                database.getPastry(
                        "Croissant"
                );

        assertNotNull(pastry);
        assertEquals(5, pastry.getStock());
        assertEquals(2.5, pastry.getPrice());
    }

    @Test
    void getPastryReturnsNullForUnknownName() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        assertNull(
                database.getPastry(
                        "Unknown"
                )
        );
    }

    @Test
    void buyPastryReducesStock() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Croissant",
                2,
                2.5
        );

        assertTrue(
                database.buyPastry(
                        "Croissant"
                )
        );

        assertEquals(
                1,
                database.getPastry(
                        "Croissant"
                ).getStock()
        );
    }

    @Test
    void buyPastryFailsForUnknownPastry() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        assertFalse(
                database.buyPastry(
                        "Unknown"
                )
        );
    }

    @Test
    void buyPastryFailsWhenStockIsZero() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        database.stockPastries(
                "Empty",
                0,
                2.0
        );

        assertFalse(
                database.buyPastry(
                        "Empty"
                )
        );
    }

    @Test
    void registerAndCheckCredentials() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        assertTrue(
                database.registerClient(
                        "alice",
                        "secret"
                )
        );

        assertTrue(
                database.checkCredentials(
                        "alice",
                        "secret"
                )
        );

        assertFalse(
                database.checkCredentials(
                        "alice",
                        "wrong"
                )
        );
    }

    @Test
    void duplicateUsernameIsRejected() {
        DatabaseAdapter database =
                new DatabaseAdapter();

        assertTrue(
                database.registerClient(
                        "alice",
                        "first"
                )
        );

        assertFalse(
                database.registerClient(
                        "alice",
                        "second"
                )
        );

        assertTrue(
                database.checkCredentials(
                        "alice",
                        "first"
                )
        );
    }
}