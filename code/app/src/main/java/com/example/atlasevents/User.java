package com.example.atlasevents;


/**
 * This is an abstract class that defines a generic User.
 */
public class User {
    /*
    * To do: implement database
     */
    private String name;
    private String email;
    private String password;
    private String phoneNumber;



    // No-arg constructor
    public User() {
    }

    // Constructor with parameters
    public User(String name, String email, String password, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    /**
     * This method allows a user to edit their profile.
     * @param name this is the new name of the user
     * @param email this is the new email of the user
     * @param password this is the new password of the user
     * @param phoneNumber this is the new phone number of the user
     */
    // Edit profile method
    public void editProfile(String name, String email, String password, String phoneNumber) {
        if (name != null) this.name = name;
        if (email != null) this.email = email;
        if (password != null) this.password = password;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
    }
}