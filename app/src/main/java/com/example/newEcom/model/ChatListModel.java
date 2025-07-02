package com.example.newEcom.model;
import com.google.firebase.Timestamp;

public class ChatListModel {
    private String userId;
    private String lastMessage;
    private Timestamp timestamp;

    public ChatListModel() {}
    public ChatListModel(String userId, String lastMessage, Timestamp timestamp) {
        this.userId = userId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }
    public String getUserId() { return userId; }
    public String getLastMessage() { return lastMessage; }
    public Timestamp getTimestamp() { return timestamp; }
}
