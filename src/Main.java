// import java.util.InputMismatchException;
import java.util.Scanner;

public class Main
{
    public static void main( String[] args )
    {
        Scanner scanner = new Scanner(System.in);
        Market market = new Market();

        System.out.println("Welcome to the Stock Market Simulator!");
        User user = null;

        while (user == null) {
            System.out.println("1. Log In");
            System.out.println("2. Sign Up");
            System.out.print("Choose an option (1 or 2): ");
            String option = scanner.nextLine().trim();

            if (option.equals("1")) {
                System.out.print("Enter your username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Enter your password: ");
                String password = scanner.nextLine().trim();
                user = market.login(username, password);
                if (user == null) {
                    System.out.println("Login failed. Invalid username or password.");
                } else {
                    System.out.println("Login successful! Welcome back, " + user.getUsername() + "!");
                }
            } else if (option.equals("2")) {
                System.out.print("Choose a username: ");
                String username = scanner.nextLine().trim();
                System.out.print("Choose a password: ");
                String password = scanner.nextLine().trim();
                System.out.print("Enter your initial cash balance: $");
                double initialCash;
                try {
                    initialCash = Double.parseDouble(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid cash amount.");
                    continue;
                }
                boolean created = market.addUser(username, password, initialCash);
                if (created) {
                    user = market.login(username, password);
                    System.out.println("Account created successfully! Welcome, " + user.getUsername() + "!");
                } else {
                    System.out.println("Username already exists. Please try a different username.");
                }
            } else {
                System.out.println("Invalid option. Please enter 1 or 2.");
            }
        }

        boolean isRunning = true;
        while (isRunning) {
            printMenu();
            System.out.print("Choose an option: ");
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number between 1 and 6.");
                continue;
            }

            switch (choice) {
                case 1:
                    // View market prices
                    market.displayMarket();
                    break;
                case 2:
                    // View portfolio
                    System.out.println(user.getPortfolio());
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
        String symbol = scanner.nextLine().trim();
        Stock stockToBuy = market.getStockBySymbol(symbol);

        if (stockToBuy == null) {
            System.out.println("Invalid stock symbol.");
            return;
        }

        System.out.print("Enter the number of shares to buy: ");
        try {
            int shares = Integer.parseInt(scanner.nextLine().trim());
            String result = user.buyStock(stockToBuy, shares);
            System.out.println(result);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a whole number for shares.");
        }
    }

    private static void handleSell(Scanner scanner, Market market, User user) {
        System.out.println(user.getPortfolio());
        if (user.getPortfolio().getHoldings().isEmpty()) {
            System.out.println("You have no stocks to sell.");
            return;
        }
        System.out.print("Enter the symbol of the stock you want to sell: ");
        String symbol = scanner.nextLine().trim();
        Stock stockToSell = market.getStockBySymbol(symbol);

        if (stockToSell == null) {
            System.out.println("Invalid stock symbol. You do not own this stock.");
            return;
        }

        System.out.print("Enter the number of shares to sell: ");
        try {
            int shares = Integer.parseInt(scanner.nextLine().trim());
            String result = user.sellStock(stockToSell, shares);
            System.out.println(result);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a whole number for shares.");
        }
    }
}