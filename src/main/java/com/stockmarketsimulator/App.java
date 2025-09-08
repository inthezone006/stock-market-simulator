package com.stockmarketsimulator;

import java.util.InputMismatchException;
import java.util.Scanner;

import main.java.com.stockmarketsimulator.Market;
import main.java.com.stockmarketsimulator.User;

public class App 
{
    public static void main( String[] args )
    {
        Scanner scanner = new Scanner(System.in);
        Market market = new Market();

        System.out.println("Welcome to the Stock Market Simulator!");
        System.out.print("Enter your name to create an account: ");
        String username = scanner.nextLine();
        System.out.print("Enter your initial cash balance: $");
        double initialCash = scanner.nextDouble();

        User user = new User(username, initialCash);
        market.addUser(user);

        System.out.println("Account created successfully! Welcome, " + user.getUsername() + "!");

        boolean isRunning = true;
        while (isRunning) {
            printMenu();
            System.out.print("Choose an option: ");
            int choice;
            try {
                choice = scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number between 1 and 6.");
                scanner.next();
                continue;
            }

            switch (choice) {
                case 1:
                    // View market prices
                    market.displayMarket();
                    break;
                case 2:
                    // View portfolio
                    System.out.println(user.displayPortfolio());
                    break;
                case 3:
                    // Buy stock
                    handleBuy(scanner, market, user);
                    break;
                case 4:
                    // Sell stock
                    handleSell(scanner, market, user);
                    break;
                case 5:
                    // Update market for next day
                    market.updateMarket();
                    market.displayMarket();
                    break;
                case 6:
                    // Exit
                    isRunning = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 6.");
                    break;
            }
        }

        System.out.println("Thank you for playing. Goodbye!");
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n===== Main Menu =====");
        System.out.println("1. View Market Prices");
        System.out.println("2. View Your Portfolio");
        System.out.println("3. Buy Stock");
        System.out.println("4. Sell Stock");
        System.out.println("5. Advance to Next Day");
        System.out.println("6. Exit Simulator");
        System.out.println("=====================");
    }

    private static void handleBuy(Scanner scanner, Market market, User user) {
        market.displayMarket();
        System.out.print("Enter the symbol of the stock you want to buy: ");
        String symbol = scanner.next();
        Stock stockToBuy = market.getStockBySymbol(symbol);

        if (stockToBuy == null) {
            System.out.println("Invalid stock symbol.");
            return;
        }

        System.out.print("Enter the number of shares to buy: ");
        try {
            int shares = scanner.nextInt();
            String result = user.buyStock(stockToBuy, shares);
            System.out.println(result);
        } catch (InputMismatchException e) {
            System.out.println("Invalid input. Please enter a whole number for shares.");
            scanner.next();
        }
    }
    
    private static void handleSell(Scanner scanner, Market market, User user) {
        System.out.println(user.getPortfolio());
        if (user.getPortfolio().getHoldings().isEmpty()) {
            System.out.println("You have no stocks to sell.");
            return;
        }
        System.out.print("Enter the symbol of the stock you want to sell: ");
        String symbol = scanner.next();
        Stock stockToSell = market.getStockBySymbol(symbol); 

        if (stockToSell == null) {
            System.out.println("Invalid stock symbol. You do not own this stock.");
            return;
        }

        System.out.print("Enter the number of shares to sell: ");
        try {
            int shares = scanner.nextInt();
            String result = user.sellStock(stockToSell, shares);
            System.out.println(result);
        } catch (InputMismatchException e) {
            System.out.println("Invalid input. Please enter a whole number for shares.");
            scanner.next();
        }
    }
}
