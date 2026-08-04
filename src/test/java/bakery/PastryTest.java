package bakery;

import bakery.model.Pastry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PastryTest {

    @Test
    void buyOneReducesStock() {
        Pastry pastry = new Pastry(2, 2.5);

        assertTrue(pastry.buyOne());
        assertEquals(1, pastry.getStock());
    }

    @Test
    void buyOneFailsWhenStockIsZero() {
        Pastry pastry = new Pastry(0, 2.5);

        assertFalse(pastry.buyOne());
        assertEquals(0, pastry.getStock());
    }

    @Test
    void priceDoesNotChange() {
        Pastry pastry = new Pastry(3, 2.5);

        pastry.buyOne();

        assertEquals(2.5, pastry.getPrice());
    }
}