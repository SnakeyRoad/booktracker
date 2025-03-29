# BookTracker Application

This Java application allows Booktracker to manage their user reading habits database. It provides various functionalities for tracking reading habits, user information, and book data.

## Features

The application provides the following functionalities:

1. Add a new user to the database
2. View reading habit data for a specific user (by ID or name)
3. Change the title of a book in the database
4. Delete a record from the ReadingHabit table
5. Calculate the mean age of users
6. Count the total number of users who have read a specific book
7. Calculate the total number of pages read by all users
8. Count the number of users who have read more than one book
9. View complete database structure and statistics
10. Exit the application

## Database Structure

The application uses SQLite with the following schema:

### User Table
- userID: INTEGER (Primary Key)
- age: INTEGER
- gender: TEXT
- Name: TEXT

### ReadingHabit Table
- habitID: INTEGER (Primary Key)
- book: TEXT
- pagesRead: INTEGER
- submissionMoment: DATETIME
- user: INTEGER (Foreign Key referencing User.userID)

## Prerequisites

- Java JDK 8 or higher
- Maven for dependency management

## Dependencies

- SQLite JDBC Driver
- Apache POI for Excel file processing
- Java Logging API

## Setup and Running

1. Clone this repository
2. Ensure the `reading_habits_dataset.xlsx` file is in the project's root directory
3. Build the project with Maven:
   ```
   mvn clean package
   ```
4. Run the application:
   ```
   java -jar target/booktracker-1.0-SNAPSHOT.jar
   ```
   
   Or run it directly from your IDE by executing the Main class.

## How to Use

When you run the application, you'll be presented with a menu of options:

1. **Add a new user**: Add a user with a name, age, and gender
2. **View reading habits for a user**: Display all reading habits for a specific user (can search by ID or name)
3. **Change a book title**: Update all instances of a book title in the database
4. **Delete a reading habit record**: Remove a specific reading habit by ID
5. **Get mean age of users**: Calculate and display the average age of all users
6. **Get user count for a specific book**: Count users who have read a particular book
7. **Get total pages read by all users**: Calculate the total number of pages read
8. **Get number of users who read multiple books**: Count users who have read more than one book
9. **View database structure**: Display comprehensive database information including:
   - List of all users
   - Books and reading statistics
   - Users without reading habits
   - Overall reading habits summary
10. **Exit**: Close the application

## First Run

On the first run, the application will:
1. Create the SQLite database (booktracker.db) in the project root directory
2. Create the necessary tables
3. Import data from the `reading_habits_dataset.xlsx` file
4. Generate random age and gender data for users since this isn't in the Excel file
5. Set the name field to "User [ID]" for imported users

## Project Structure

```
src/main/java/com/christianmol/booktracker/
├── Main.java                    # Application entry point
├── database/
│   └── DatabaseManager.java     # SQLite database operations
├── model/
│   ├── User.java                # User entity model
│   └── ReadingHabit.java        # Reading habit entity model
└── ui/                          # User interface components
```

## Technical Features

- **Transaction Management**: All database operations are wrapped in transactions
- **Error Handling**: Comprehensive error handling with custom exceptions
- **Input Validation**: All user inputs are validated before processing
- **Logging**: Detailed logging of all operations and errors
- **Resource Management**: Proper handling of database connections and file resources
- **Data Integrity**: Foreign key constraints and data validation
- **Flexible Search**: User search by ID or name
- **Comprehensive Statistics**: Detailed database structure and usage statistics
