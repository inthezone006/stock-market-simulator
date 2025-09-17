import java.text.DecimalFormat;

public class Stock {
    private String symbol;
    private String name;
    private double price;

    public Stock(String symbol, String name, double price) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void updatePrice() {
        double changePercent = (Math.random() - 0.5) / 10.0;
        this.price += this.price * changePercent;
        if (this.price < 1.0) {
            this.price = 1.0;
        }
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.00");
        return name + " (" + symbol + "): $" + df.format(price);
    }
}
