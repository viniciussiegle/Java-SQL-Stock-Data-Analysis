# Stock Price Analyzer

## Overview
The Stock Price Analyzer is a personal project developed in **Java** and **SQL (SQLite3)**, with a focus on financial data processing. The application imports stock data from `.csv` files, stores it in a database, and provides functionality to analyze stock prices by calculating **Simple Moving Averages (SMA)**, **Exponential Moving Averages (EMA)**, and **Price Volatility** using optimized SQL queries and custom implementations.

## Features
1. **CSV File Processing:**
   - The project reads `.csv` files containing historical stock prices.
   - It leverages the **Apache Commons CSV™** library to process and extract the data.
   - The parsed data is inserted into an **SQLite3** database, updating relevant tables.

2. **Database and User Management:**
   - A **DatabaseHandler class** is implemented to manage database connections, queries, and data processing.
   - All user interactions—prompting the user for input, calling data processing procedures, and outputting results—are handled by a dedicated **UserHandler class**.
   - The user is prompted to select only valid stock tickers stored in the database for analysis.

3. **SMA Calculation:**
   - The system calculates the **Simple Moving Average (SMA)** for selected stocks over user-specified time periods.
   - Efficient SQL queries are used to retrieve historical closing prices and calculate the moving average.

4. **EMA Calculation with Recursive CTEs:**
   - The project uses **Recursive Common Table Expressions (Recursive CTEs)** to query the database and calculate the **Exponential Moving Average (EMA)**, showcasing advanced SQL query techniques.

5. **Price Volatility Calculation:**
   - The project calculates **Price Volatility** by computing the **Standard Deviation** of closing prices over specified time periods.
   - This feature is implemented manually due to **SQLite's** lack of support for the `STDEV()` function, demonstrating custom algorithm development.

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

## Future Enhancements
This project can be expanded to include:
- Real-time stock price fetching from external APIs.
- Support for additional technical indicators (e.g., Bollinger Bands, Relative Strength Index).
- A GUI interface for easier interaction.
