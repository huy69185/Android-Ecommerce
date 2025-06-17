package com.example.newEcom.model;

import com.google.firebase.Timestamp;

public class CartItemModel {
    private String name, image;
    private int productId, quantity;
    private double price, originalPrice;
    private Timestamp timestamp;

    public CartItemModel() {
    }

    public CartItemModel(int productId, String name, String image, int quantity, double price, double originalPrice, Timestamp timestamp) {
        this.name = name;
        this.image = image;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.originalPrice = originalPrice;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}