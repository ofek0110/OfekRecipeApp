package com.example.ofek.models;

import androidx.annotation.NonNull;

public class User {
    private String id, email, firstname, lastname, password, phone;
    private boolean isAdmin;

    public User() {
    }

    public User(String id, String email, String firstname, String lastname, String password, String phone, boolean isAdmin) {
        this.id = id;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.password = password;
        this.phone = phone;
        this.isAdmin = isAdmin;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id +
                ", email='" + email +
                ", firstname='" + firstname +
                ", lastname='" + lastname +
                ", password='" + password +
                ", phone='" + phone +
                 ", isAdmin=" + isAdmin +
                '}';
    }
}
