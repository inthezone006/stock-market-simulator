import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Market {
    private final List<Stock> stocks;
    private final Map<String, UserRecord> users = new HashMap<>();
    private static final String USERS_FILE = ".users.dat";

    public Market() {
        stocks = new ArrayList<>();
        initializeStocks();
        loadUsers();
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

    public boolean addUser(String username, String password, double startingCash) {
        if (users.containsKey(username)) {
            return false;
        }
        String hashed = hashPassword(password);
        users.put(username, new UserRecord(username, hashed, startingCash));
        saveUsers();
        return true;
    }

    public User login(String username, String password) {
        UserRecord record = users.get(username);
        if (record != null && record.hashedPassword.equals(hashPassword(password))) {
            return new User(username, record.cashBalance);
        }
        return null;
    }

    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String username = parts[0];
                    String hashed = parts[1];
                    double cash = Double.parseDouble(parts[2]);
                    users.put(username, new UserRecord(username, hashed, cash));
                }
            }
        } catch (IOException e) {
            // File may not exist yet
        }
    }

    private void saveUsers() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (UserRecord record : users.values()) {
                writer.write(record.username + "," + record.hashedPassword + "," + record.cashBalance);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving users: " + e.getMessage());
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available");
        }
    }

    private static class UserRecord {
        String username;
        String hashedPassword;
        double cashBalance;

        UserRecord(String username, String hashedPassword, double cashBalance) {
            this.username = username;
            this.hashedPassword = hashedPassword;
            this.cashBalance = cashBalance;
        }
    }
}
