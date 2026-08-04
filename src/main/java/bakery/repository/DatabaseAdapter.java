package bakery.repository;

import bakery.model.Pastry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DatabaseAdapter {

    private final Map<String, Pastry> pastries =
            new HashMap<>();

    private final Map<String, String> credentials =
            new HashMap<>();

    public void stockPastries(
            String name,
            int stock,
            double price
    ) {
        pastries.put(
                name,
                new Pastry(stock, price)
        );
    }

    public Pastry getPastry(String name) {
        return pastries.get(name);
    }

    public boolean buyPastry(String name) {
        Pastry pastry = pastries.get(name);

        return pastry != null
                && pastry.buyOne();
    }

    public boolean registerClient(
            String username,
            String password
    ) {
        if (credentials.containsKey(username)) {
            return false;
        }

        credentials.put(username, password);
        return true;
    }

    public boolean checkCredentials(
            String username,
            String password
    ) {
        return credentials.containsKey(username)
                && Objects.equals(
                credentials.get(username),
                password
        );
    }
}