package analysis;


import analysis.handlers.DatabaseHandler;
import analysis.handlers.UserHandler;


public class Main {
    public static void main(String[] args) {


        // Initialize handlers and paths
        String dataSourcePath = "data/";
        String databaseUrl = "jdbc:sqlite:data/stocks.db";
        DatabaseHandler dbHandler = new DatabaseHandler(databaseUrl);
        UserHandler userHandler = new UserHandler(dbHandler);


        // Update data of historical quotes in database with .csv files in path
        userHandler.updateDB(dataSourcePath);


        // Prompt for valid stock in the database
        String stock = userHandler.getStockTicker();



        // Calculate amd print analysis values

        // Get Simple Moving Average (SMA) for the last 30, 180, and 360 days
        userHandler.analyze(Analyses.SMA, stock, 30, 180, 360);


        // Get Exponential Moving Average (EMA) for the last 30 days
        userHandler.analyze(Analyses.EMA, stock, 30, 60, 90);


        // Get Price Volatility (30d, 180d, 360d)
        userHandler.analyze(Analyses.Volatility, stock, 30, 90);


    }

}