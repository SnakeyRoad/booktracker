package com.christianmol.booktracker.database;

import java.sql.*;
import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DatabaseManager implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final String DB_URL = "jdbc:sqlite:booktracker.db";
    private Connection connection;
    private final Random random = new Random();
    private final Set<Integer> existingUserIds = new HashSet<>();

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(false); // Enable transaction support
            LOGGER.info("Connection to SQLite has been established.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error connecting to database", e);
            throw new DatabaseException("Failed to connect to database", e);
        }
    }

    public void initializeDatabase() {
        try {
            createUserTable();
            createReadingHabitTable();
            if (isDatabaseEmpty()) {
                LOGGER.info("Database is empty, importing data from Excel file...");
                importExcelData();
                importUserData();
                commitTransaction();
            } else {
                LOGGER.info("Database already contains data, skipping import.");
            }
        } catch (Exception e) {
            rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Error during database initialization", e);
            throw new DatabaseException("Failed to initialize database", e);
        }
    }

    private void commitTransaction() {
        try {
            connection.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error committing transaction", e);
            throw new DatabaseException("Failed to commit transaction", e);
        }
    }

    private void rollbackTransaction() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error rolling back transaction", e);
            throw new DatabaseException("Failed to rollback transaction", e);
        }
    }

    private boolean isDatabaseEmpty() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM User")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if database is empty", e);
            throw new DatabaseException("Failed to check database state", e);
        }
        return true;
    }

    public void importUserData() {
        String excelFilePath = "reading_habits_dataset.xlsx";
        FileInputStream inputStream = null;
        Workbook workbook = null;
        
        try {
            // Check if file exists
            File excelFile = findExcelFile(excelFilePath);
            if (excelFile == null || !excelFile.exists()) {
                LOGGER.severe("Excel file not found for user data import.");
                throw new DatabaseException("Excel file not found for user data import.");
            }
            
            inputStream = new FileInputStream(excelFile);
            workbook = new XSSFWorkbook(inputStream);
            
            // Check if User sheet exists
            Sheet userSheet = null;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if ("User".equalsIgnoreCase(workbook.getSheetName(i))) {
                    userSheet = workbook.getSheetAt(i);
                    break;
                }
            }
            
            if (userSheet == null) {
                LOGGER.warning("No 'User' sheet found in Excel file. Will generate random user data.");
                return;
            }
            
            LOGGER.info("Found User sheet. Importing user data...");
            
            // Skip header row
            for (int i = 1; i <= userSheet.getLastRowNum(); i++) {
                try {
                    Row row = userSheet.getRow(i);
                    if (row != null) {
                        // Read user data
                        int userID = (int) row.getCell(0).getNumericCellValue();
                        int age = (int) row.getCell(1).getNumericCellValue();
                        
                        // Get gender (could be string 'm'/'f' or 'Male'/'Female')
                        String gender;
                        Cell genderCell = row.getCell(2);
                        if (genderCell.getCellType() == CellType.STRING) {
                            String rawGender = genderCell.getStringCellValue().trim().toLowerCase();
                            if (rawGender.startsWith("m")) {
                                gender = "Male";
                            } else if (rawGender.startsWith("f")) {
                                gender = "Female";
                            } else {
                                gender = "Other";
                            }
                        } else {
                            // Default to random gender if cell type is unexpected
                            gender = random.nextBoolean() ? "Male" : "Female";
                        }
                        
                        // Set name to "User [ID]"
                        String name = "User " + userID;
                        
                        // Check if this user already exists (it might have been added during reading habit import)
                        if (!existingUserIds.contains(userID)) {
                            addUser(userID, age, gender, name);
                            existingUserIds.add(userID);
                        } else {
                            // Update existing user with correct age and gender from User sheet
                            updateUser(userID, age, gender, name);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing user row " + i, e);
                    // Continue with next row instead of aborting the entire import
                }
            }
            
            LOGGER.info("User data imported successfully.");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error importing user data", e);
            throw new DatabaseException("Failed to import user data", e);
        } finally {
            try {
                if (workbook != null) {
                    workbook.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing resources", e);
            }
        }
    }
    
    private void updateUser(int userID, int age, String gender, String name) {
        String sql = "UPDATE User SET age = ?, gender = ?, Name = ? WHERE userID = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, age);
            pstmt.setString(2, gender);
            pstmt.setString(3, name);
            pstmt.setInt(4, userID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating user: " + e.getMessage());
        }
    }
    
    private void createUserTable() {
        String sql = "CREATE TABLE IF NOT EXISTS User (\n"
                + "    userID INTEGER PRIMARY KEY,\n"
                + "    age INTEGER,\n"
                + "    gender TEXT\n"
                + ");";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            
            // Check if Name column exists, if not add it
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "User", "Name");
            
            if (!rs.next()) {
                String alterSql = "ALTER TABLE User ADD COLUMN Name TEXT;";
                stmt.execute(alterSql);
                System.out.println("Added Name column to User table.");
            }
            
        } catch (SQLException e) {
            System.out.println("Error creating User table: " + e.getMessage());
        }
    }

    private void createReadingHabitTable() {
        String sql = "CREATE TABLE IF NOT EXISTS ReadingHabit (\n"
                + "    habitID INTEGER PRIMARY KEY,\n"
                + "    book TEXT,\n"
                + "    pagesRead INTEGER,\n"
                + "    submissionMoment DATETIME,\n"
                + "    user INTEGER,\n"
                + "    FOREIGN KEY (user) REFERENCES User(userID)\n"
                + ");";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error creating ReadingHabit table: " + e.getMessage());
        }
    }

    private File findExcelFile(String filename) {
        // Try current directory first
        File excelFile = new File(filename);
        if (excelFile.exists()) {
            LOGGER.info("Excel file found at: " + excelFile.getAbsolutePath());
            return excelFile;
        }
        
        // Try other common locations
        String[] possiblePaths = {
            "./reading_habits_dataset.xlsx",
            "../reading_habits_dataset.xlsx",
            "src/main/resources/reading_habits_dataset.xlsx"
        };
        
        for (String path : possiblePaths) {
            File alternative = new File(path);
            if (alternative.exists()) {
                LOGGER.info("Found Excel file at alternative location: " + alternative.getAbsolutePath());
                return alternative;
            }
        }
        
        LOGGER.severe("Excel file not found in any of the expected locations.");
        throw new DatabaseException("Excel file not found. Please ensure reading_habits_dataset.xlsx is in the project root directory.");
    }

    private void addUser(int userID, int age, String gender, String name) {
        String sql = "INSERT INTO User(userID, age, gender, Name) VALUES(?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setInt(2, age);
            pstmt.setString(3, gender);
            pstmt.setString(4, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error adding user: " + e.getMessage());
        }
    }

    private void addReadingHabit(int habitID, String book, int pagesRead, LocalDateTime submissionMoment, int userID) {
        String sql = "INSERT INTO ReadingHabit(habitID, book, pagesRead, submissionMoment, user) VALUES(?, ?, ?, datetime(?), ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, habitID);
            pstmt.setString(2, book);
            pstmt.setInt(3, pagesRead);
            pstmt.setString(4, submissionMoment.toString().replace('T', ' ')); // Convert to SQLite datetime format
            pstmt.setInt(5, userID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error adding reading habit", e);
            throw new DatabaseException("Failed to add reading habit", e);
        }
    }

    public void addUser(String name, int age, String gender) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 0 and 150");
        }
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }

        String sql = "INSERT INTO User (Name, age, gender) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name.trim());
            pstmt.setInt(2, age);
            pstmt.setString(3, gender.trim());
            pstmt.executeUpdate();
            commitTransaction();
            LOGGER.info("User added successfully: " + name);
        } catch (SQLException e) {
            rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Error adding user", e);
            throw new DatabaseException("Failed to add user", e);
        }
    }

    public void getReadingHabitsForUser(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        String sql;
        if (input.matches("\\d+")) {
            // If input is a number, search by userID
            int userID = Integer.parseInt(input);
            if (userID <= 0) {
                throw new IllegalArgumentException("User ID must be positive");
            }
            sql = "SELECT rh.* FROM ReadingHabit rh WHERE rh.user = ?";
        } else {
            // If input is text, search by name
            sql = "SELECT rh.* FROM ReadingHabit rh " +
                  "JOIN User u ON rh.user = u.userID " +
                  "WHERE u.Name LIKE ?";
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            if (input.matches("\\d+")) {
                pstmt.setInt(1, Integer.parseInt(input));
            } else {
                pstmt.setString(1, "%" + input.trim() + "%");
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    String dateStr = rs.getString("submissionMoment");
                    System.out.printf("Book: %s, Pages Read: %d, Date: %s%n",
                            rs.getString("book"),
                            rs.getInt("pagesRead"),
                            dateStr);
                }
                if (!found) {
                    if (input.matches("\\d+")) {
                        System.out.println("No reading habits found for user ID " + input);
                    } else {
                        System.out.println("No reading habits found for user name containing '" + input + "'");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving reading habits", e);
            throw new DatabaseException("Failed to retrieve reading habits", e);
        }
    }

    public void changeBookTitle(String oldTitle, String newTitle) {
        if (oldTitle == null || oldTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Old title cannot be null or empty");
        }
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("New title cannot be null or empty");
        }

        String sql = "UPDATE ReadingHabit SET book = ? WHERE book = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newTitle.trim());
            pstmt.setString(2, oldTitle.trim());
            int updated = pstmt.executeUpdate();
            commitTransaction();
            System.out.println("Updated " + updated + " records");
        } catch (SQLException e) {
            rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Error changing book title", e);
            throw new DatabaseException("Failed to change book title", e);
        }
    }

    public void deleteReadingHabit(int habitID) {
        if (habitID <= 0) {
            throw new IllegalArgumentException("Habit ID must be positive");
        }

        String sql = "DELETE FROM ReadingHabit WHERE habitID = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, habitID);
            int deleted = pstmt.executeUpdate();
            commitTransaction();
            System.out.println("Deleted " + deleted + " record(s)");
        } catch (SQLException e) {
            rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Error deleting reading habit", e);
            throw new DatabaseException("Failed to delete reading habit", e);
        }
    }

    public void getMeanUserAge() {
        String sql = "SELECT AVG(age) as mean_age FROM User";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double meanAge = rs.getDouble("mean_age");
                System.out.printf("Mean user age: %.2f%n", meanAge);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating mean user age", e);
            throw new DatabaseException("Failed to calculate mean user age", e);
        }
    }

    public void getUserCountForBook(String bookTitle) {
        if (bookTitle == null || bookTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be null or empty");
        }

        String sql = "SELECT COUNT(DISTINCT user) as user_count FROM ReadingHabit WHERE book = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bookTitle.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("user_count");
                    System.out.printf("Number of users who read '%s': %d%n", bookTitle, count);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting users for book", e);
            throw new DatabaseException("Failed to count users for book", e);
        }
    }

    public void getTotalPagesRead() {
        String sql = "SELECT SUM(pagesRead) as total_pages FROM ReadingHabit";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int totalPages = rs.getInt("total_pages");
                System.out.printf("Total pages read by all users: %d%n", totalPages);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating total pages read", e);
            throw new DatabaseException("Failed to calculate total pages read", e);
        }
    }

    public void getUsersWithMultipleBooks() {
        String sql = "SELECT COUNT(*) as user_count FROM (SELECT user FROM ReadingHabit GROUP BY user HAVING COUNT(DISTINCT book) > 1)";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt("user_count");
                System.out.printf("Number of users who read multiple books: %d%n", count);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting users with multiple books", e);
            throw new DatabaseException("Failed to count users with multiple books", e);
        }
    }

    public void viewDatabaseStructure() {
        System.out.println("\n=== Database Structure Overview ===\n");
        
        // View Users
        System.out.println("Users:");
        System.out.println("----------------------------------------");
        String userSql = "SELECT userID, Name, age, gender FROM User ORDER BY userID";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(userSql)) {
            while (rs.next()) {
                System.out.printf("ID: %d | Name: %s | Age: %d | Gender: %s%n",
                    rs.getInt("userID"),
                    rs.getString("Name"),
                    rs.getInt("age"),
                    rs.getString("gender"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving user data", e);
            throw new DatabaseException("Failed to retrieve user data", e);
        }

        // View Books and Reading Statistics
        System.out.println("\nBooks and Reading Statistics:");
        System.out.println("----------------------------------------");
        String bookSql = "SELECT book, COUNT(*) as read_count, " +
                        "SUM(pagesRead) as total_pages, " +
                        "COUNT(DISTINCT user) as unique_readers " +
                        "FROM ReadingHabit " +
                        "GROUP BY book " +
                        "ORDER BY read_count DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(bookSql)) {
            while (rs.next()) {
                System.out.printf("Book: %s%n", rs.getString("book"));
                System.out.printf("  Times read: %d | Total pages: %d | Unique readers: %d%n",
                    rs.getInt("read_count"),
                    rs.getInt("total_pages"),
                    rs.getInt("unique_readers"));
                System.out.println("----------------------------------------");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving book statistics", e);
            throw new DatabaseException("Failed to retrieve book statistics", e);
        }

        // View Users without Reading Habits
        System.out.println("\nUsers without Reading Habits:");
        System.out.println("----------------------------------------");
        String inactiveUsersSql = "SELECT u.userID, u.Name, u.age, u.gender " +
                                "FROM User u " +
                                "LEFT JOIN ReadingHabit rh ON u.userID = rh.user " +
                                "WHERE rh.habitID IS NULL " +
                                "ORDER BY u.userID";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(inactiveUsersSql)) {
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("ID: %d | Name: %s | Age: %d | Gender: %s%n",
                    rs.getInt("userID"),
                    rs.getString("Name"),
                    rs.getInt("age"),
                    rs.getString("gender"));
            }
            if (!found) {
                System.out.println("No users without reading habits found.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving inactive users", e);
            throw new DatabaseException("Failed to retrieve inactive users", e);
        }

        // View Reading Habits Summary
        System.out.println("\nReading Habits Summary:");
        System.out.println("----------------------------------------");
        String summarySql = "SELECT " +
                          "COUNT(DISTINCT u.userID) as total_users, " +
                          "COUNT(DISTINCT rh.user) as users_with_habits, " +
                          "COUNT(DISTINCT rh.book) as total_books, " +
                          "COUNT(rh.habitID) as total_reading_records, " +
                          "SUM(rh.pagesRead) as total_pages_read " +
                          "FROM User u " +
                          "LEFT JOIN ReadingHabit rh ON u.userID = rh.user";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(summarySql)) {
            if (rs.next()) {
                System.out.printf("Total Users in Database: %d%n", rs.getInt("total_users"));
                System.out.printf("Users with Reading Habits: %d%n", rs.getInt("users_with_habits"));
                System.out.printf("Users without Reading Habits: %d%n", 
                    rs.getInt("total_users") - rs.getInt("users_with_habits"));
                System.out.printf("Total Books: %d%n", rs.getInt("total_books"));
                System.out.printf("Total Reading Records: %d%n", rs.getInt("total_reading_records"));
                System.out.printf("Total Pages Read: %d%n", rs.getInt("total_pages_read"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving reading habits summary", e);
            throw new DatabaseException("Failed to retrieve reading habits summary", e);
        }
    }

    private void importExcelData() {
        String excelFilePath = "reading_habits_dataset.xlsx";
        File excelFile = findExcelFile(excelFilePath);
        
        if (excelFile == null || !excelFile.exists()) {
            LOGGER.warning("Excel file not found for data import.");
            return;
        }

        try (FileInputStream inputStream = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            LOGGER.info("Found " + sheet.getLastRowNum() + " rows in Excel file.");
            
            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                try {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        // Read data from Excel
                        int habitID = (int) row.getCell(0).getNumericCellValue();
                        int userID = (int) row.getCell(1).getNumericCellValue();
                        int pagesRead = (int) row.getCell(2).getNumericCellValue();
                        
                        // Handle book title
                        String book;
                        Cell bookCell = row.getCell(3);
                        if (bookCell.getCellType() == CellType.STRING) {
                            book = bookCell.getStringCellValue().trim();
                        } else if (bookCell.getCellType() == CellType.NUMERIC) {
                            book = String.valueOf(bookCell.getNumericCellValue()).trim();
                        } else {
                            LOGGER.warning("Invalid book title format for habitID " + habitID + ", using 'Unknown'");
                            book = "Unknown";
                        }
                        
                        // Handle submission moment
                        Cell dateCell = row.getCell(4);
                        LocalDateTime submissionMoment;
                        
                        if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                            submissionMoment = dateCell.getLocalDateTimeCellValue();
                        } else {
                            submissionMoment = LocalDateTime.now();
                            LOGGER.warning("Using current time for habitID " + habitID + " as date was not in correct format");
                        }
                        
                        // Add user if not already added
                        if (!existingUserIds.contains(userID)) {
                            int age = 18 + random.nextInt(48); // 18-65
                            String gender = random.nextBoolean() ? "Male" : "Female";
                            String name = "User " + userID;
                            
                            addUser(userID, age, gender, name);
                            existingUserIds.add(userID);
                        }
                        
                        // Add reading habit
                        addReadingHabit(habitID, book, pagesRead, submissionMoment, userID);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing row " + i, e);
                    // Continue with next row instead of aborting the entire import
                }
            }
            
            LOGGER.info("Excel data imported successfully.");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error importing Excel data", e);
            throw new DatabaseException("Failed to import Excel data", e);
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                LOGGER.info("Database connection closed.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error closing database connection", e);
            throw new DatabaseException("Failed to close database connection", e);
        }
    }

    // Custom exception class
    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String message) {
            super(message);
        }

        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
