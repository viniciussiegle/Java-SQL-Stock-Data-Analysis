package analysis.handlers;
import analysis.Analyses;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * A class that handles all interactions between user and DatabaseHandler, such as prompting,
 * forwarding calls to procedures and outputting results.
 */
public class UserHandler {

    private final DatabaseHandler dbHandler;


    /**
     * A class that handles all relations between the user and the DatabaseHandler, such as prompting,
     * forwarding calls to procedures and outputting results.
     * @param dbHandler the DatabaseHandler
     */
    public UserHandler(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }


    /**
     * Calls updates to the database using the .csv files in the specified path. Updates overwrite existing tables.
     * @param path the path of the source .csv files
     */
    public void updateDB(String path) {
        // Update the database with all .csv files in the given path
        File dir = new File(path);
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getName().endsWith(".csv")) {
                dbHandler.updateDB(file);
            }
        }
    }

    /**
     * Prompts the user for a stock ticker. Only allows tickers existing in the database to avoid injections
     * in following uses.
     * @return the selected stock ticker if the database is not empty, null otherwise
     */
    public String getStockTicker() {
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
     * Prints the given analysis for a given valid stock in the given time periods, starting
     * at the most recent data entries.
     * @param analysis analysis to be performed
     * @param stock the stock to be analyzed
     * @param days the time periods to be analyzed in, in past business days from most recent entry
     */
    public void printResults (Analyses analysis, String stock, int... days) {
        if (days.length == 0) { // If no time period is given, return at once
            return;
        }

        // Get analysis
        int n = days.length;
        float[] results = new float[n];

        switch (analysis) {
            case SMA:
                for (int i = 0; i < n; i++) {
                    results[i] = dbHandler.getSMA(stock, days[i]);
                }
                break;
            case EMA:
                for (int i = 0; i < n; i++) {
                    results[i] = dbHandler.getEMA(stock, days[i]);
                }
                break;
            case Volatility:
                for (int i = 0; i < n; i++) {
                    results[i] = dbHandler.getVolatility(stock, days[i]);
                }
                break;
        }

        if (results[0] == 0) { // If stock is invalid, return at once
            System.out.println("No data found for given stock: " + stock);
            return;
        }

        // Print results
        System.out.println(analysis + ":");
        for (int i = 0; i < days.length; i++) {
            System.out.printf("%d days: %.2f", days[i], results[i]);
            System.out.println();
        }
        System.out.println();
    }

}
