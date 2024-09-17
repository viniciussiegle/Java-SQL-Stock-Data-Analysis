package realtime;

import java.sql.*;
import java.util.*;


public class Main {
    public static void main(String[] args) {

        // (start gui)

        // (fetch data from the web)

        // save data to new database


        // retrieve data from the database (initially, only average opening prices)
        float avg = getAverageOpening();

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
     * Gets the average opening price of a stock from all available ones in the database for a prompted stock ticker.
     * @return the average opening price
     */
    private static float getAverageOpening() {
        String preparedQuery = "select avg(Open) from ";
        String url = "jdbc:sqlite:data/stocks.db";
        float avg = 0;
        try (
                Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()
        ) {
            String stock = getStockTicker(connection);

            ResultSet resultSet = statement.executeQuery(preparedQuery + stock);
            resultSet.next();
            avg = resultSet.getFloat(1);
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return avg;
    }

    /**
     * Prompts the user for a stock ticker. Only allows tickers existing in the database to avoid injections.
     * @param connection the connection to the database
     * @return the stock ticker
     * @throws SQLException if a database access error occurs
     */
    private static String getStockTicker(Connection connection) throws SQLException {
        // Get available stock tickers based on existing table names
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet tables = metaData.getTables(null, null, "%", null);
        List<String> availableStocks = new ArrayList<>();
        tables.next();
        while (tables.next()) {
            availableStocks.add(tables.getString("TABLE_NAME"));
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