package analysis.handlers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that handles all operations to the specified database.
 */
public class DatabaseHandler {

    private final String url;


    /**
     * A class that handles all operations to the specified database.
     * @param url the url from the database
     */
    public DatabaseHandler(String url) {
        this.url = url;
    }


    /**
     * Creates a new table using the data in the given .csv file. Overwrites already existing table with the same name.
     * @param file .csv file to be read from
     */
    protected void updateDB(File file) {
        // Get names
        String csvPath = file.getPath();
        String fileName = file.getName().toLowerCase();
        String tableName = fileName.substring(0, fileName.lastIndexOf('.'));

        // Prepare text
        String drop = "DROP TABLE IF EXISTS " + tableName;
        String create = "CREATE TABLE " + tableName + "(Date TEXT, Open REAL, High REAL, Low REAL, Close REAL, Volume REAL)";
        String sql = "INSERT INTO " + tableName + " VALUES (?, ?, ?, ?, ?, ?)";

        try (
                // Open reader for CSV File
                FileReader reader = new FileReader(csvPath);
                CSVParser csvParser = new CSVParser(reader,
                        CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build());

                // Connect to database
                Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()
        ) {
            // Reset / Drop table if exists
            statement.executeUpdate(drop);
            statement.executeUpdate(create);

            // Prepare statement after table has correct schema
            PreparedStatement preparedStatement = connection.prepareStatement(sql);

            connection.setAutoCommit(false); // Allow batch processing

            // Iterate over CSV Records
            for (CSVRecord record : csvParser) {
                // Convert date types for better compatibility with SQLite
                SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd");
                preparedStatement.setString(1, format2.format(format1.parse(record.get(0))));

                // Set remaining entries
                for (int i = 1; i < record.size(); i++) {
                    preparedStatement.setString(i + 1, record.get(i));
                }

                preparedStatement.addBatch();
            }

            // Execute batch insert and commit transaction
            preparedStatement.executeBatch();
            connection.commit();

            // Close necessary resources and return changes to default
            preparedStatement.close();
            connection.setAutoCommit(true);

        }
        catch (SQLException | IOException | ParseException e) {
            System.out.println(e.getMessage());
        }
    }


    /**
     * Gets a list of available stock tickers in the database. Can serve as a list of valid tickers to avoid injections.
     * @return the list of available stock tickers if the database is not empty, null otherwise
     */
    public List<String> getAvailableStocks() {
        // Get available stock tickers based on existing table names
        List<String> availableStocks = null;
        try (
                Connection connection = DriverManager.getConnection(url)
        ) {
            // Get table names from Metadata
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", null);
            availableStocks = new ArrayList<>();
            tables.next(); // skip header line
            while (tables.next()) {
                availableStocks.add(tables.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return availableStocks;
    }


    /**
     * Gets the Simple Moving Average (SMA) of a stock for a valid stock ticker in the given time period,
     * starting at the most recent data entries.
     * @param stock the stock ticker to analyze
     * @param days the time period to be analyzed in, in past days from most recent entry
     * @return the Simple Moving Average of the stock if it is valid, 0 otherwise
     */
    public float getSMA (String stock, int days) {
        // Restricts again to only valid strings to avoid injections
        if (!getAvailableStocks().contains(stock)) {
            return 0;
        }
        // Queries for average value for stock ticker
        String query =
                "SELECT                                                               "
                +"    AVG(Close)                                                      "
                +"FROM                                                                "
                +"    "+stock+"                                                       "
                +"WHERE                                                               "
                +"    Date > DATE((SELECT MAX(DATE) FROM "+stock+"), '-"+days+" days')";
        float avg = 0;
        try (
                Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()
        ) {
            ResultSet resultSet = statement.executeQuery(query);
            resultSet.next(); // skip header line
            avg = resultSet.getFloat(1);
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return avg;
    }


    /**
     * Gets the Exponential Moving Average (EMA) of a stock for a valid stock ticker in the given time period,
     * starting at the most recent data entries.
     * @param stock the stock ticker to analyze
     * @param days the time period to be analyzed in, in past days from most recent entry
     * @return the Exponential Moving Average of the stock if it is valid, 0 otherwise
     */
    public float getEMA (String stock, int days) {
        // Restricts again to only valid strings to avoid injections
        if (!getAvailableStocks().contains(stock)) {
            return 0;
        }

        // Calculate EMA values for stock ticker
        // Calculate alpha (smoothing factor) of the EMA
        float alpha = 2 / (float)(days + 1);
        // Create query with recursive CTEs
        String query =
            "WITH RECURSIVE                                                                           "
            +"    analysis_table AS (                                                                 "
            +"        -- Isolate necessary values                                                   \n"
            +"        SELECT                                                                          "
            +"            Date,                                                                       "
            +"            Close,                                                                      "
            +"            ROW_NUMBER() OVER (ORDER BY DATE DESC) as row_number                        "
            +"        FROM                                                                            "
            +"            "+stock+"                                                                   "
            +"        WHERE                                                                           "
            +"            Date > DATE((SELECT MAX(DATE) FROM "+stock+"), '-"+days+" days')            "
            +"    ),                                                                                  "
            +"    ema_calc AS(                                                                        "
            +"        -- Get Close value of first date as initial EMA                               \n"
            +"        SELECT                                                                          "
            +"            *,                                                                          "
            +"            Close as EMA                                                                "
            +"        FROM                                                                            "
            +"            analysis_table                                                              "
            +"        WHERE                                                                           "
            +"            Date = (SELECT MIN(Date) FROM analysis_table)                               "
            +"                                                                                        "
            +"        UNION ALL                                                                       "
            +"                                                                                        "
            +"        -- Calculate EMA for subsequent dates                                         \n"
            +"        SELECT                                                                          "
            +"            analysis_table.Date,                                                        "
            +"            analysis_table.Close,                                                       "
            +"            analysis_table.row_number,                                                  "
            +"            (analysis_table.Close * "+alpha+") + (ema_calc.EMA * (1 - "+alpha+")) as EMA"
            +"        FROM                                                                            "
            +"            analysis_table                                                              "
            +"        JOIN                                                                            "
            +"            ema_calc                                                                    "
            +"        ON                                                                              "
            +"            analysis_table.row_number = ema_calc.row_number - 1                         "
            +"        WHERE                                                                           "
            +"            analysis_table.Date <= (SELECT MAX(Date) FROM analysis_table)               "
            +"    )                                                                                   "
            +"                                                                                        "
            +"SELECT EMA, MAX(Date) FROM ema_calc;                                                    ";

        float ema = 0;
        try (
                Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()
        ) {
            ResultSet resultSet = statement.executeQuery(query);
            resultSet.next(); // skip header line
            ema = resultSet.getFloat(1);
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return ema;
    }


    /**
     * Gets the Price Volatility of a stock for a valid stock ticker in the given time period, starting at
     * the most recent data entries.
     * @param stock the stock ticker to analyze
     * @param days the time period to be analyzed in, in past days from most recent entry
     * @return the Price Volatility of the stock if it is valid, 0 otherwise
     */
    public float getVolatility (String stock, int days) {
        // Restricts again to only valid strings to avoid injections
        if (!getAvailableStocks().contains(stock)) {
            return 0;
        }

        // Calculate Volatility value
        // Setup variables
        float mean = getSMA(stock, days);
        List<Float> closingPrices = new ArrayList<>();
        String query =
                "SELECT                                                               "
                +"    Close                                                           "
                +"FROM                                                                "
                +"    "+stock+"                                                       "
                +"WHERE                                                               "
                +"    Date > DATE((SELECT MAX(DATE) FROM "+stock+"), '-"+days+" days')";

        // Get closing prices
        try (
                Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()
        ){
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()) {
                closingPrices.add(resultSet.getFloat(1));
            }
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // If retrieval succeeded, calculate
        double tmp = 0;
        if (!closingPrices.isEmpty() && closingPrices.getFirst() != 0) {
            for (float closingPrice : closingPrices) {
                tmp += Math.pow(closingPrice - mean, 2);
            }
        }
        return (float) Math.sqrt(tmp / closingPrices.size()); // Return volatility
    }


}
