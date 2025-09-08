package main.java.com.stockmarketsimulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.stockmarketsimulator.Stock;

public class Market {
    private final List<Stock> stocks;

    public Market() {
        stocks = new ArrayList<>();
        initializeStocks();
    }

    private void initializeStocks() {
        stocks.add(new Stock("GOOGL", "Alphabet Inc.", 140.50));
        stocks.add(new Stock("AAPL", "Apple Inc.", 175.22));
        stocks.add(new Stock("MSFT", "Microsoft Corp.", 370.90));
        stocks.add(new Stock("AMZN", "Amazon.com, Inc.", 155.46));
        stocks.add(new Stock("TSLA", "Tesla, Inc.", 245.88));
    }

    public Stock getStockBySymbol(String symbol) {
        for (Stock stock : stocks) {
            if (stock.getSymbol().equalsIgnoreCase(symbol)) {
                return stock;
            }
        }
        return null;
    }

    public void updateMarket() {
        System.out.println("\nMarket is updating for the next day...");
        for (Stock stock : stocks) {
            stock.updatePrice();
        }
        System.out.println("Market update complete.\n");
    }

    public void displayMarket() {
        System.out.println("-------------------- CURRENT MARKET --------------------");
        for (Stock stock : stocks) {
            System.out.println(stock);
        }
        System.out.println("------------------------------------------------------");
    }
}
