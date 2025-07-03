package com.example.newEcom.model;

import com.google.firebase.Timestamp;

public class ChatRoomModel {
    private String roomId;
    private String userId;
    private String userName;
    private String lastMessage;
    private Timestamp lastMessageTimestamp;

    public ChatRoomModel() {
    }

    public ChatRoomModel(String roomId, String userId, String userName, String lastMessage, Timestamp lastMessageTimestamp) {
        this.roomId = roomId;
        this.userId = userId;
        this.userName = userName;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }
}