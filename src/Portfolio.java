import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class Portfolio {
    private double cashBalance;
    private final Map<Stock, Integer> holdings;

    public Portfolio(double initialCash) {
        this.cashBalance = initialCash;
        this.holdings = new HashMap<>();
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public Map<Stock, Integer> getHoldings() {
        return holdings;
    }

    public void addStock(Stock stock, int shares) {
        holdings.put(stock, holdings.getOrDefault(stock, 0) + shares);
    }

    public boolean removeStock(Stock stock, int shares) {
        if (!holdings.containsKey(stock) || holdings.get(stock) < shares) {
            return false;
        }
        int newShares = holdings.get(stock) - shares;
        if (newShares == 0) {
            holdings.remove(stock);
        } else {
            holdings.put(stock, newShares);
        }
        return true;
    }

    public void addCash(double amount) {
        this.cashBalance += amount;
    }

    public boolean removeCash(double amount) {
        if (cashBalance >= amount) {
            this.cashBalance -= amount;
            return true;
        }
        return false;
    }

    public double getTotalValue() {
        double stocksValue = 0.0;
        for (Map.Entry<Stock, Integer> entry : holdings.entrySet()) {
            stocksValue += entry.getKey().getPrice() * entry.getValue();
        }
        return cashBalance + stocksValue;
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        StringBuilder sb = new StringBuilder();
        sb.append("-------------------- PORTFOLIO --------------------\n");
        sb.append("Cash Balance: $").append(df.format(cashBalance)).append("\n");
        sb.append("Stock Holdings:\n");
        if (holdings.isEmpty()) {
            sb.append("  No stocks owned.\n");
        } else {
            for (Map.Entry<Stock, Integer> entry : holdings.entrySet()) {
                Stock stock = entry.getKey();
                int shares = entry.getValue();
                double value = stock.getPrice() * shares;
                sb.append("  - ").append(stock.getName()).append(" (").append(stock.getSymbol()).append("): ")
                  .append(shares).append(" shares @ $").append(df.format(stock.getPrice()))
                  .append(" | Total Value: $").append(df.format(value)).append("\n");
            }
        }
        sb.append("Total Portfolio Value: $").append(df.format(getTotalValue())).append("\n");
        sb.append("---------------------------------------------------\n");
        return sb.toString();
    }
}
