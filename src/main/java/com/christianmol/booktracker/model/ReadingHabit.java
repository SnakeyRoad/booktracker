package com.christianmol.booktracker.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class ReadingHabit {
    private final int habitID;
    private String book;
    private int pagesRead;
    private final LocalDateTime submissionMoment;
    private final int userID;

    public ReadingHabit(int habitID, String book, int pagesRead, LocalDateTime submissionMoment, int userID) {
        if (habitID <= 0) {
            throw new IllegalArgumentException("Habit ID must be positive");
        }
        if (book == null || book.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be null or empty");
        }
        if (pagesRead < 0) {
            throw new IllegalArgumentException("Pages read cannot be negative");
        }
        if (submissionMoment == null) {
            throw new IllegalArgumentException("Submission moment cannot be null");
        }
        if (userID <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        
        this.habitID = habitID;
        this.book = book.trim();
        this.pagesRead = pagesRead;
        this.submissionMoment = submissionMoment;
        this.userID = userID;
    }

    // Getters
    public int getHabitID() {
        return habitID;
    }

    public String getBook() {
        return book;
    }

    public int getPagesRead() {
        return pagesRead;
    }

    public LocalDateTime getSubmissionMoment() {
        return submissionMoment;
    }

    public int getUserID() {
        return userID;
    }

    // Setters (only for mutable fields)
    public void setBook(String book) {
        if (book == null || book.trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be null or empty");
        }
        this.book = book.trim();
    }

    public void setPagesRead(int pagesRead) {
        if (pagesRead < 0) {
            throw new IllegalArgumentException("Pages read cannot be negative");
        }
        this.pagesRead = pagesRead;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadingHabit that = (ReadingHabit) o;
        return habitID == that.habitID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(habitID);
    }

    @Override
    public String toString() {
        return "ReadingHabit{" +
                "habitID=" + habitID +
                ", book='" + book + '\'' +
                ", pagesRead=" + pagesRead +
                ", submissionMoment=" + submissionMoment +
                ", userID=" + userID +
                '}';
    }
}
