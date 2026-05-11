package stocktraderproapp;

// includes trade information
public class Trade {

    private final String buyOrSell; // is "Buy" or "Sell", if its anything else declare an exception
    private final String stock;
    private final double shareAmount;
    private final double priceAmount;
    private final String date;

    public Trade(String buyOrSell, String stock, double shareAmount, double priceAmount, String date) {
        this.buyOrSell = buyOrSell;
        this.stock = stock;
        this.shareAmount = shareAmount;
        this.priceAmount = priceAmount;
        this.date = date;
    }

    public String getBuyOrSell() {
        return buyOrSell;
    }

    public String getStock() {
        return stock;
    }

    public double getShareAmount() {
        return shareAmount;
    }

    public double getPriceAmount() {
        return priceAmount;
    }
    
    public String getDate() {
        return date;
    }
}
