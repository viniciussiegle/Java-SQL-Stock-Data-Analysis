package realtime;

import java.util.*;


public class Main {
    public static void main(String[] args) {

        // (start gui)

        // (fetch data from the web)

        // Update data for historical quotes in database
        String dataSourcePath = "data/";
        String databaseUrl = "jdbc:sqlite:data/stocks.db";
        DatabaseHandler dbHandler = new DatabaseHandler(databaseUrl);
        dbHandler.updateDB(dataSourcePath);

        // Prompt for valid stock in the database
        String stock = getStockTicker(dbHandler);

        // Calculate amd print analysis values
        // Get Performance (5d, 1m, 3m, YTD, 1y)

        // Get Simple Moving Average (SMA) for the last 30, 180, and 360 days
        printSMA(dbHandler, stock, 30, 180, 360);

        // Get EMA (30d)

        // Get Volatility (30d, 180d, 360d)



        // (update data and analysis)


    }


    /**
     * Prompts the user for a stock ticker. Only allows tickers existing in the database to avoid injections.
     * @param dbHandler the Database Handler
     * @return the stock ticker if there is any available. May return null if database is empty.
     */
    private static String getStockTicker(DatabaseHandler dbHandler) {
        // Get available stock tickers based on existing table names
        List<String> availableStocks = dbHandler.getAvailableStocks();

        // Handles empty database
        if (availableStocks == null || availableStocks.isEmpty()) {
            System.out.println("Sorry! No data found!");
            return null;
        }

        // Prompt user for stock ticker, restricting to existing ones to avoid injections
        Scanner scanner = new Scanner(System.in);
        String stock;
        do {
            System.out.println("Please enter stock ticker (" + availableStocks + "): ");
            stock = scanner.nextLine();
        } while (!availableStocks.contains(stock));

        return stock;
    }

    /**
     * Prints the Simple Moving Average (SMA) for a given valid stock in the given time periods.
     * @param dbHandler the database handler
     * @param stock the stock to be analyzed
     * @param days the time period to be analyzed in, in past days
     */
    private static void printSMA(DatabaseHandler dbHandler, String stock, int... days) {
        if (days.length == 0) { // If no time period is given, return at once
            return;
        }

        // Get first SMA
        float[] smas = new float[days.length];
        smas[0] = dbHandler.getSMA(stock, days[0]);

        if (smas[0] == 0) { // If stock is invalid, return at once
            System.out.println("No data found for given stock: " + stock);
            return;
        }

        // Get remaining SMAs
        for (int i = 1; i < days.length; i++) {
            smas[i] = dbHandler.getSMA(stock, days[i]);
        }

        // Print results
        System.out.println("SMAs:");
        for (int i = 0; i < days.length; i++) {
            System.out.printf("%d days: %.2f", days[i], smas[i]);
            System.out.println();
        }
        System.out.println();
    }
}