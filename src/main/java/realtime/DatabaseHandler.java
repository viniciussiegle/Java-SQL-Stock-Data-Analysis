package realtime;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseHandler {

    private final String url;

    public DatabaseHandler(String url) {
        this.url = url;
    }

    /**
     * Updates the database using the .csv files in the specified path. Overwrites existing tables.
     * @param path the path of the source .csv files
     */
    public void updateDB(String path) {
        // Update the database with all .csv files in the given path
        File dir = new File(path);
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getName().endsWith(".csv")) {
                updateDB(file);
            }
        }
    }

    /**
     * Creates a new table using the data in the given .csv file. Overwrites already existing table with the same name.
     * @param file .csv file to be read from
     */
    private void updateDB(File file) {
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
                for (int i = 0; i < record.size(); i++) {
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

            System.out.println("Data successfully updated for: " + tableName);
        }
        catch (SQLException | IOException e) {
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
     * Gets the average opening price of a stock for a valid stock ticker.
     * @return the average opening price of the stock if it is valid, 0 otherwise
     */
    public float getAverageOpening (String stock) {
        // Restricts again to only valid stock tickers
        if (!getAvailableStocks().contains(stock)) {
            return 0;
        }
        // Queries for average for stock sticker
        String query = "SELECT AVG(Open) FROM " + stock;
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


}
