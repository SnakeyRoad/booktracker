package com.christianmol.booktracker.model;

import java.util.Objects;

public class User {
    private final int userID;
    private int age;
    private String gender;
    private String name;

    public User(int userID, int age, String gender, String name) {
        if (userID <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 0 and 150");
        }
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        
        this.userID = userID;
        this.age = age;
        this.gender = gender.trim();
        this.name = name.trim();
    }

    // Getters
    public int getUserID() {
        return userID;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getName() {
        return name;
    }

    // Setters (except for userID which is immutable)
    public void setAge(int age) {
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 0 and 150");
        }
        this.age = age;
    }

    public void setGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or empty");
        }
        this.gender = gender.trim();
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        this.name = name.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userID == user.userID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }

    @Override
    public String toString() {
        return "User{" +
                "userID=" + userID +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

