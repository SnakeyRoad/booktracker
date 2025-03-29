package com.christianmol.booktracker;

import com.christianmol.booktracker.database.DatabaseManager;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        DatabaseManager dbManager = null;
        Scanner scanner = null;
        
        try {
            System.out.println("Starting BookTracker Application...");
            dbManager = new DatabaseManager();
            
            try {
                dbManager.initializeDatabase();
                System.out.println("Database initialization completed.");
            } catch (Exception e) {
                System.err.println("Error during database initialization: " + e.getMessage());
                e.printStackTrace();
                System.out.println("Continuing with application startup despite initialization error.");
            }
        
            scanner = new Scanner(System.in);
            boolean running = true;
            
            System.out.println("Welcome to BookTracker Application");
            
            while (running) {
                System.out.println("\nPlease select an option:");
                System.out.println("1. Add a new user");
                System.out.println("2. View reading habits for a user");
                System.out.println("3. Change a book title");
                System.out.println("4. Delete a reading habit record");
                System.out.println("5. Get mean age of users");
                System.out.println("6. Get user count for a specific book");
                System.out.println("7. Get total pages read by all users");
                System.out.println("8. Get number of users who read multiple books");
                System.out.println("9. View database structure");
                System.out.println("10. Exit");
                
                System.out.print("\nEnter your choice (1-10): ");
                String choice = scanner.nextLine();
                
                try {
                    switch (choice) {
                        case "1":
                            System.out.print("Enter user name: ");
                            String name = scanner.nextLine();
                            
                            System.out.print("Enter age: ");
                            int age = Integer.parseInt(scanner.nextLine());
                            
                            System.out.print("Enter gender (Male/Female): ");
                            String gender = scanner.nextLine();
                            
                            dbManager.addUser(name, age, gender);
                            break;
                            
                        case "2":
                            System.out.println("Enter user ID (1-66) or name to view reading habits:");
                            System.out.println("Note: You can enter either:");
                            System.out.println("- A number between 1-66 (user ID)");
                            System.out.println("- A user's name (partial name will work)");
                            String userInput = scanner.nextLine();
                            
                            dbManager.getReadingHabitsForUser(userInput);
                            break;
                            
                        case "3":
                            System.out.print("Enter current book title: ");
                            String oldTitle = scanner.nextLine();
                            
                            System.out.print("Enter new book title: ");
                            String newTitle = scanner.nextLine();
                            
                            dbManager.changeBookTitle(oldTitle, newTitle);
                            break;
                            
                        case "4":
                            System.out.print("Enter habit ID to delete: ");
                            int habitID = Integer.parseInt(scanner.nextLine());
                            
                            dbManager.deleteReadingHabit(habitID);
                            break;
                            
                        case "5":
                            dbManager.getMeanUserAge();
                            break;
                            
                        case "6":
                            System.out.print("Enter book title: ");
                            String bookTitle = scanner.nextLine();
                            
                            dbManager.getUserCountForBook(bookTitle);
                            break;
                            
                        case "7":
                            dbManager.getTotalPagesRead();
                            break;
                            
                        case "8":
                            dbManager.getUsersWithMultipleBooks();
                            break;

                        case "9":
                            dbManager.viewDatabaseStructure();
                            break;
                            
                        case "10":
                            running = false;
                            System.out.println("Exiting the application...");
                            break;
                            
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: Please enter a valid number.");
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
            if (dbManager != null) {
                dbManager.close();
            }
        }
    }
}