package bakery.rest;

public class BuyRequest {

    private String pastryName;

    public BuyRequest() {
    }

    public BuyRequest(String pastryName) {
        this.pastryName = pastryName;
    }

    public String getPastryName() {
        return pastryName;
    }

    public void setPastryName(String pastryName) {
        this.pastryName = pastryName;
    }
}