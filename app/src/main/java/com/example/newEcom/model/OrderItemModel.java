package com.example.newEcom.model;

import com.google.firebase.Timestamp;

public class OrderItemModel {
    private int orderId, productId;
    private String name, image;
    private double price;
    private int quantity;
    private Timestamp timestamp;
    private String fullName, email, phoneNumber, address, comments;
    private String status;
    private String userId; // Thêm trường này
    private Timestamp createdAt; // Thêm trường này
    private String orderParentId; // Thêm trường này
    private String itemId; // Thêm trường này

    public OrderItemModel() {
    }

    public OrderItemModel(int orderId, int productId, String name, String image, double price, int quantity, Timestamp timestamp, String fullName, String email, String phoneNumber, String address, String comments, String status) {
        this.orderId = orderId;
        this.productId = productId;
        this.name = name;
        this.image = image;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.comments = comments;
        this.status = status;
    }

    public OrderItemModel(int orderId, int productId, String name, String image, double price, int quantity, Timestamp timestamp, String fullName, String email, String phoneNumber, String address, String comments) {
        this(orderId, productId, name, image, price, quantity, timestamp, fullName, email, phoneNumber, address, comments, "Pending");
    }

    // Getter và Setter cho userId và createdAt
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Getter và Setter cho orderParentId và itemId
    public String getOrderParentId() { return orderParentId; }
    public void setOrderParentId(String orderParentId) { this.orderParentId = orderParentId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    // Getter và Setter hiện tại (đã có)
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}