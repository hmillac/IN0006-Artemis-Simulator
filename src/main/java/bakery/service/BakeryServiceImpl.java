package bakery.service;

import bakery.model.Pastry;
import bakery.observer.StockNotifier;
import bakery.repository.DatabaseAdapter;
import bakery.rest.AuthRequest;
import bakery.rest.AuthResponse;
import bakery.rest.BuyRequest;
import bakery.rest.BuyResponse;
import bakery.rest.StockResponse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BakeryServiceImpl {

    private final DatabaseAdapter database;
    private final PaymentService paymentService;
    private final StockNotifier stockNotifier;

    private final Set<String> lockedPastries =
            ConcurrentHashMap.newKeySet();

    public BakeryServiceImpl(
            DatabaseAdapter database,
            PaymentService paymentService,
            StockNotifier stockNotifier
    ) {
        this.database = database;
        this.paymentService = paymentService;
        this.stockNotifier = stockNotifier;
    }

    public StockResponse queryStock(
            String pastryName
    ) {
        Pastry pastry =
                database.getPastry(pastryName);

        int amount =
                pastry == null
                        ? -1
                        : pastry.getStock();

        return new StockResponse(amount);
    }

    public BuyResponse buy(
            BuyRequest request
    ) {
        return executeBuy(
                request.getPastryName()
        );
    }

    public boolean prepare(
            String pastryName
    ) {
        Pastry pastry =
                database.getPastry(pastryName);

        if (pastry == null) {
            return false;
        }

        if (pastry.getStock() <= 0) {
            return false;
        }

        return lockedPastries.add(
                pastryName
        );
    }

    public boolean commit(
            String pastryName
    ) {
        try {
            Pastry pastry =
                    database.getPastry(
                            pastryName
                    );

            if (pastry == null) {
                return false;
            }

            boolean paid =
                    paymentService.processPayment(
                            pastryName,
                            pastry.getPrice()
                    );

            if (!paid) {
                return false;
            }

            return database.buyPastry(
                    pastryName
            );
        } finally {
            lockedPastries.remove(
                    pastryName
            );
        }
    }

    public void abort(
            String pastryName
    ) {
        lockedPastries.remove(
                pastryName
        );
    }

    public BuyResponse executeBuy(
            String pastryName
    ) {
        if (!prepare(pastryName)) {
            abort(pastryName);

            return new BuyResponse(
                    false,
                    "Pastry unavailable."
            );
        }

        if (!commit(pastryName)) {
            abort(pastryName);

            return new BuyResponse(
                    false,
                    "Payment failed."
            );
        }

        Pastry pastry =
                database.getPastry(
                        pastryName
                );

        if (pastry != null) {
            stockNotifier.notifyIfLowStock(
                    pastryName,
                    pastry.getStock()
            );
        }

        return new BuyResponse(
                true,
                "Purchase successful."
        );
    }

    public AuthResponse authenticate(
            AuthRequest request
    ) {
        boolean authenticated =
                database.checkCredentials(
                        request.getUsername(),
                        request.getPassword()
                );

        if (authenticated) {
            return new AuthResponse(
                    true,
                    "Authentication successful."
            );
        }

        return new AuthResponse(
                false,
                "Invalid username or password."
        );
    }
}