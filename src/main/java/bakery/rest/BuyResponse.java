package bakery.rest;

public class BuyResponse {

    private boolean success;
    private String message;

    public BuyResponse() {
    }

    public BuyResponse(
            boolean success,
            String message
    ) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}