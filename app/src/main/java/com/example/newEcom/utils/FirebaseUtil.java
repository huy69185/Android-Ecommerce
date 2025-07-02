package com.example.newEcom.utils;

import com.example.newEcom.model.CartItemModel;
import com.example.newEcom.model.ProductModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseUtil {
    public static final String ADMIN_USER_ID = "3QRm0nJTKnU9OpVul6N7kEV0OFF3"; // UserId cố định cho admin

    public static CollectionReference getCategories() {
        return FirebaseFirestore.getInstance().collection("categories");
    }

    public static CollectionReference getProducts() {
        return FirebaseFirestore.getInstance().collection("products");
    }

    public static CollectionReference getBanner() {
        return FirebaseFirestore.getInstance().collection("banners");
    }

    public static CollectionReference getCartItems() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return FirebaseFirestore.getInstance().collection("cart").document(userId).collection("items");
        }
        return null;
    }

    public static CollectionReference getWishlistItems() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return FirebaseFirestore.getInstance().collection("wishlists").document(userId).collection("items");
        }
        return null;
    }

    public static CollectionReference getOrderItems() {
        String userId = getCurrentUserId();
        if (userId != null) {
            return FirebaseFirestore.getInstance().collection("orders").document(userId).collection("items");
        }
        return null;
    }

    public static CollectionReference getReviews(int pid) {
        return FirebaseFirestore.getInstance().collection("reviews").document(String.valueOf(pid)).collection("review");
    }

    public static DocumentReference getDetails() {
        return FirebaseFirestore.getInstance().collection("dashboard").document("details");
    }

    public static StorageReference getProductImageReference(String id) {
        return FirebaseStorage.getInstance().getReference().child("product_images").child(id);
    }

    public static StorageReference getCategoryImageReference(String id) {
        return FirebaseStorage.getInstance().getReference().child("category_images").child(id);
    }

    public static StorageReference getBannerImageReference(String id) {
        return FirebaseStorage.getInstance().getReference().child("banner_images").child(id);
    }

    public static String getCurrentUserId() {
        return FirebaseAuth.getInstance().getUid();
    }

    public static CollectionReference getChatMessages(String userId) {
        if (userId == null) return null;
        return FirebaseFirestore.getInstance()
                .collection("chats")
                .document(userId)
                .collection("messages");
    }


    // Người dùng chỉ chat với admin
    public static CollectionReference getUserChatMessages() {
        String userId = getCurrentUserId();
        if (userId != null && !userId.equals(ADMIN_USER_ID)) {
            return FirebaseFirestore.getInstance().collection("user_chats").document(ADMIN_USER_ID).collection("messages");
        }
        return null;
    }

    // Admin chat với nhiều người dùng
    public static CollectionReference getAdminChatMessages(String userId) {
        if (userId != null) {
            return FirebaseFirestore.getInstance().collection("admin_chats").document(userId).collection("messages");
        }
        return null;
    }
}