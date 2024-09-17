package realtime;

import java.sql.*;


public class Main {
    public static void main(String[] args) {

        // (start gui)

        // (fetch data from the web)

        // save data to new database


        // *retrieve data from the database (initially, only average opening prices)
        String stock = "googl";
        String query = "select avg(Open) from " + stock;
        String url = "jdbc:sqlite:data/stocks.db";

        float avg = 0;
        try (
                Connection connection = DriverManager.getConnection(url);
                Statement statement = connection.createStatement()
        ) {
            ResultSet resultSet = statement.executeQuery(query);
            resultSet.next();
            avg = resultSet.getFloat(1);
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // *calculate amd print analysis values
        if (avg == 0) {
            System.out.println("No data found");
        }
        else {
            System.out.println("Average opening price for '" + stock + "' is: " + avg);
        }

        // (update data and analysis)


    }
}