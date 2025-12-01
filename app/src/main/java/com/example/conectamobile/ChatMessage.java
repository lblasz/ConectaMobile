package com.example.conectamobile;

public class ChatMessage {
    public String messageId;
    public String senderId;
    public String text;
    public long timestamp;

    public ChatMessage() { } // Constructor vacío requerido por Firebase

    public ChatMessage(String messageId, String senderId, String text, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }
}