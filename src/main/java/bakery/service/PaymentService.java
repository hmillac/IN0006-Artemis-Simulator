package bakery.service;

public interface PaymentService {

    boolean processPayment(
            String pastryName,
            double price
    );
}