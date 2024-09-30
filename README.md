# Stock Data Analysis

## Overview
The Stock Data Analysis is a personal project developed in **Java** and **SQL (SQLite3)**, with a focus on financial data processing. The application imports stock data from `.csv` files, stores it in a database, and provides functionality to analyze stock prices by calculating **Simple Moving Averages (SMA)**, **Exponential Moving Averages (EMA)**, and **Price Volatility** using optimized SQL queries and custom implementations.

## Features
1. **CSV File Processing:**
   - The project reads `.csv` files containing historical stock prices.
   - It leverages the **Apache Commons CSV™** library to process and extract the data.
   - The parsed data is inserted into an **SQLite3** database using **batch insertion**, improving performance when updating relevant tables.
  
     ```java
        /**
        * Inserts the data from the csv file in the given path using the given connection and prepared
        * statement text
        * @param csvPath the path from the source csv file
        * @param connection the database connection
        * @param insert the prepared statement text
        * @throws SQLException if a database access error occurs, or this method is called on a closed connection
        */
        private void insertCSVRecords(String csvPath, Connection connection, String insert) throws SQLException {
           try (
                   // Open reader for CSV File
                   FileReader reader = new FileReader(csvPath);
                   CSVParser csvParser = new CSVParser(reader,
                           CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build());

                   // Prepare statement
                   PreparedStatement preparedStatement = connection.prepareStatement(insert)
           ) {
               // Allow batch processing
               connection.setAutoCommit(false);

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

               // Return changes to default
               connection.setAutoCommit(true);
           }
           catch (IOException | ParseException e) {
               System.out.println(e.getMessage());
           }
        }
     ```

2. **Database and User Management:**
   - A **DatabaseHandler class** is implemented to manage database connections, queries, and data processing.
   - All user interactions—prompting the user for input, calling data processing procedures, and outputting results—are handled by a dedicated **UserHandler class**.
   - The user is prompted to select only valid stock tickers stored in the database for analysis.
  
     ```java
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



           // Calculate and print analysis values

           // Get Simple Moving Average (SMA) for the last 30, 180, and 360 days
           userHandler.analyze(Analyses.SMA, stock, 30, 180, 360);


           // Get Exponential Moving Average (EMA) for the last 30 days
           userHandler.analyze(Analyses.EMA, stock, 30, 60, 90);


           // Get Price Volatility (30d, 180d, 360d)
           userHandler.analyze(Analyses.Volatility, stock, 30, 90);

        }
     }
     ```

3. **SMA Calculation:**
   - The system calculates the **Simple Moving Average (SMA)** for selected stocks over user-specified time periods.
   - SQL queries are used to retrieve historical closing prices and calculate the moving average.
  
     ```java
        /**
        * Gets the Simple Moving Average (SMA) of a stock for a valid stock ticker in the given time period,
        * starting at the most recent data entries.
        * @param stock the stock ticker to analyze
        * @param days the time period to be analyzed in, in past days from most recent entry
        * @return the Simple Moving Average of the stock if it is valid, 0 otherwise
        */
        public float getSMA (String stock, int days) {
           // Create query for average
           String query =
                   "SELECT                                                                       "
                   +"    AVG(Close)                                                              "
                   +"FROM                                                                        "
                   +"    " + stock + "                                                           "
                   +"WHERE                                                                       "
                   +"    Date > DATE((SELECT MAX(DATE) FROM " + stock + "), '-" + days + " days')";

           return runQuery(query, stock);
        }
     ```
  
4. **EMA Calculation with Recursive CTEs:**
   - The project uses **Recursive Common Table Expressions (Recursive CTEs)** to query the database and calculate the **Exponential Moving Average (EMA)**, showcasing advanced SQL query techniques.
  
     ```java
        /**
        * Gets the Exponential Moving Average (EMA) of a stock for a valid stock ticker in the given time period,
        * starting at the most recent data entries.
        * @param stock the stock ticker to analyze
        * @param days the time period to be analyzed in, in past days from most recent entry
        * @return the Exponential Moving Average of the stock if it is valid, 0 otherwise
        */
        public float getEMA (String stock, int days) {
           // Calculate alpha (smoothing factor) of the EMA
           float alpha = 2 / (float)(days + 1);

           // Create query with recursive CTEs
           String query =
                   "WITH RECURSIVE                                                                          "
                   +"    scope AS (                                                                         "
                   +"        -- Isolate necessary values                                                  \n"
                   +"        SELECT                                                                         "
                   +"            Date,                                                                      "
                   +"            Close,                                                                     "
                   +"            ROW_NUMBER() OVER (ORDER BY DATE DESC) as row_number                       "
                   +"        FROM                                                                           "
                   +"            " + stock + "                                                              "
                   +"        WHERE                                                                          "
                   +"            Date > DATE((SELECT MAX(DATE) FROM " + stock + "), '-" + days + " days')   "
                   +"    ),                                                                                 "
                   +"    ema_calc AS(                                                                       "
                   +"        -- Get Close value of first date as initial EMA                              \n"
                   +"        SELECT                                                                         "
                   +"            *,                                                                         "
                   +"            Close as EMA                                                               "
                   +"        FROM                                                                           "
                   +"            scope                                                                      "
                   +"        WHERE                                                                          "
                   +"            Date = (SELECT MIN(Date) FROM scope)                                       "
                   +"                                                                                       "
                   +"        UNION ALL                                                                      "
                   +"                                                                                       "
                   +"        -- Calculate EMA for subsequent dates                                        \n"
                   +"        SELECT                                                                         "
                   +"            scope.Date,                                                                "
                   +"            scope.Close,                                                               "
                   +"            scope.row_number,                                                          "
                   +"            (scope.Close * " + alpha + ") + (ema_calc.EMA * (1 - " + alpha + ")) as EMA"
                   +"        FROM                                                                           "
                   +"            scope                                                                      "
                   +"        JOIN                                                                           "
                   +"            ema_calc                                                                   "
                   +"        ON                                                                             "
                   +"            scope.row_number = ema_calc.row_number - 1                                 "
                   +"        WHERE                                                                          "
                   +"            scope.Date <= (SELECT MAX(Date) FROM scope)                                "
                   +"    )                                                                                  "
                   +"                                                                                       "
                   +"SELECT EMA, MAX(Date) FROM ema_calc;                                                   ";

           return runQuery(query, stock);
        }
     ```

5. **Price Volatility Calculation:**
   - The project calculates **Price Volatility** by computing the **Standard Deviation** of closing prices over specified time periods.
   - This feature is implemented manually due to **SQLite's** lack of support for the `STDEV()` function, demonstrating custom algorithm development.
  
     ```java
        /**
        * Gets the Price Volatility of a stock for a valid stock ticker in the given time period, starting at
        * the most recent data entries.
        * @param stock the stock ticker to analyze
        * @param days the time period to be analyzed in, in past days from most recent entry
        * @return the Price Volatility of the stock if it is valid, 0 otherwise
        */
        public float getVolatility (String stock, int days) {
           // Create query for Variance
           String query =
                   "WITH scope AS (                                                                   "
                   +"    SELECT                                                                       "
                   +"        Close as close,                                                          "
                   +"        AVG(Close) OVER () AS avg                                                "
                   +"    FROM                                                                         "
                   +"        " + stock + "                                                            "
                   +"    WHERE                                                                        "
                   +"        Date >= DATE((SELECT MAX(DATE) FROM " + stock + "), '-" + days + " days')"
                   +")                                                                                "
                   +"SELECT                                                                           "
                   +"    AVG((scope.close - scope.avg) * (scope.close - scope.avg)) as variance       "
                   +"FROM scope;                                                                      ";

           // Return Volatility (Standard Deviation)
           float variance = runQuery(query, stock);
           return (float) Math.sqrt(variance);
        }
     ```

6. **Enum-based Analysis Types:**
   - The types of stock price analyses (e.g., SMA, EMA, Price Volatility) are declared in an **enum class**, making the code for handling different analyses more compact, modular, and flexible.

## Security Considerations

   - To safeguard the system from **SQL injection attacks**, all SQL statements that handle user input are validated, ensuring that only valid data is processed. This approach helps maintain the security and reliability of the system.

## Technology Stack
- **Programming Languages:** Java, SQL
- **Database:** SQLite3
- **Libraries:**
   - **Apache Commons CSV™** for CSV file processing
   - **JDBC (Java Database Connectivity)** for SQL operations

## How to Run the Project
1. Ensure **Java** and **SQLite3** are installed on your machine.
2. Clone the repository to your local machine.
3. Provide `.csv` files with the following structure:
   ```
   Date, Open, High, Low, Close, Volume
   ```
   Each `.csv` file should correspond to a specific stock ticker.
4. Run the Java application. The program will:
   - Read the `.csv` file to update the SQLite database.
   - Prompt you to select a stock ticker from the database.
   - Allow you to calculate the **SMA**, **EMA**, and **Price Volatility** for the selected stock over a given time period.
  
   <br/>
<img width="567" alt="Screenshot 2024-09-20 at 17 03 11" src="https://github.com/user-attachments/assets/9c81f43f-a5b7-42c9-9ad2-722d73e5b1c5">

## Future Enhancements
This project can be expanded to include:
- Real-time stock price fetching from external APIs.
- Support for additional technical indicators (e.g., Bollinger Bands, Relative Strength Index).
- A GUI interface for easier interaction.
