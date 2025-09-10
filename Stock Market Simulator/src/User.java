public class User {
    private final String username;
    private final Portfolio portfolio;

    public User(String username, double startingCash) {
        this.username = username;
        this.portfolio = new Portfolio(startingCash);
    }

    public String getUsername() {
        return username;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * Executes a stock purchase for the user.
     * @param stock The stock to buy.
     * @param shares The number of shares to buy.
     * @return A message indicating the result of the transaction.
     */
    public String buyStock(Stock stock, int shares) {
        if (stock == null || shares <= 0) {
            return "Invalid stock or number of shares.";
        }
        
        double totalCost = stock.getPrice() * shares;

        boolean purchaseSuccessful = portfolio.removeCash(totalCost);
        
        if (purchaseSuccessful) {
            portfolio.addStock(stock, shares);
            return String.format("Successfully purchased %d shares of %s for $%.2f", shares, stock.getSymbol(), totalCost);
        } else {
            return "Error: Insufficient funds to complete purchase.";
        }
    }

    /**
     * Executes a stock sale for the user.
     * @param stock The stock to sell.
     * @param shares The number of shares to sell.
     * @return A message indicating the result of the transaction.
     */
    public String sellStock(Stock stock, int shares) {
        if (stock == null || shares <= 0) {
            return "Invalid stock or number of shares.";
        }

        boolean saleSuccessful = portfolio.removeStock(stock, shares);

        if (saleSuccessful) {
            double totalProceeds = stock.getPrice() * shares;
            portfolio.addCash(totalProceeds);
            return String.format("Successfully sold %d shares of %s for $%.2f", shares, stock.getSymbol(), totalProceeds);
        } else {
            return "Error: You do not own enough shares of " + stock.getSymbol() + " to sell.";
        }
    }
}
