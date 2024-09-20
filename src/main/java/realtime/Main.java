package realtime;


import realtime.handlers.DatabaseHandler;
import realtime.handlers.UserHandler;


public class Main {
    public static void main(String[] args) {

        // (start gui)

        // (fetch data from the web)


        // Initialize handlers and paths
        String dataSourcePath = "data/";
        String databaseUrl = "jdbc:sqlite:data/stocks.db";
        DatabaseHandler dbHandler = new DatabaseHandler(databaseUrl);
        UserHandler userHandler = new UserHandler(dbHandler);

        // Update data of historical quotes in database
        userHandler.updateDB(dataSourcePath);

        // Prompt for valid stock in the database
        String stock = userHandler.getStockTicker();


        // Calculate amd print analysis values
        // Get Performance (5d, 1m, 3m, YTD, 1y)

        // Get Simple Moving Average (SMA) for the last 30, 180, and 360 days
        userHandler.printResults("SMA", stock, 30, 180, 360);

        // Get Exponential Moving Average (EMA) for the last 30 days
        userHandler.printResults("EMA", stock, 30);

        // Get Price Volatility (30d, 180d, 360d)
        userHandler.printResults("Volatility", stock, 30);


        // (update data and analysis)


    }

}