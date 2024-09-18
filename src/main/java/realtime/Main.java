package realtime;

import java.util.*;


public class Main {
    public static void main(String[] args) {

        // (start gui)

        // (fetch data from the web)

        // update data for historical quotes in database
        String dataSourcePath = "data/";
        String databaseUrl = "jdbc:sqlite:data/stocks.db";
        DatabaseHandler dbHandler = new DatabaseHandler(databaseUrl);
        dbHandler.updateDB(dataSourcePath);

        // retrieve data from the database (initially, only average opening prices)
        String stock = getStockTicker(dbHandler);
        float avg = dbHandler.getAverageOpening(stock);

        // calculate amd print analysis values
        if (avg == 0) {
            System.out.println("No data found");
        }
        else {
            System.out.println("Average opening price is: " + avg);
        }

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
}