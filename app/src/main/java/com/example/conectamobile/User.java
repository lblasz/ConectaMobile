package com.example.conectamobile;

public class User {
    public String uid;
    public String email;
    public String username;

    public User() {
        // Constructor vacío requerido por Firebase
    }

    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
        this.username = email; // Por defecto usaremos el email como nombre
    }
}
