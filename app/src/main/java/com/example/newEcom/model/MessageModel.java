package com.example.newEcom.model;

import com.google.firebase.Timestamp;

public class MessageModel {
    private String senderId;
    private String message;
    private Timestamp timestamp;
    private boolean isAdmin;

    public MessageModel() {
    }

    public MessageModel(String senderId, String message, Timestamp timestamp, boolean isAdmin) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
        this.isAdmin = isAdmin;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}