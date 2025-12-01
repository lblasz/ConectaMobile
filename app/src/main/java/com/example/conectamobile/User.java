package com.example.conectamobile;

public class User {
    public String uid;
    public String email;
    public String username;
    public String profileImageUrl;

    public User() {
        // Constructor vacío requerido por Firebase
    }

    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
        this.username = email;
        this.profileImageUrl = "";
    }
}
