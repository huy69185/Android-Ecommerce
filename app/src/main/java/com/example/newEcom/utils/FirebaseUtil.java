package com.example.newEcom.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class FirebaseUtil {
    public static final String ADMIN_USER_ID = "3QRm0nJTKnU9OpVul6N7kEV0OFF3";
    private static FirebaseFirestore firestore;

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

    public static void getAllOrderItems(OnOrderItemsLoadedListener listener) {
        FirebaseFirestore.getInstance().collection("orders")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<CollectionReference> itemCollections = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        itemCollections.add(doc.getReference().collection("items"));
                    }
                    listener.onItemsLoaded(itemCollections);
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi nếu cần
                });
    }

    public static CollectionReference getUserOrderItems() {
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

    public static DocumentReference getUserProfile(String userId) {
        return FirebaseFirestore.getInstance().collection("users").document(userId);
    }

    public static CollectionReference getChatRooms() {
        return FirebaseFirestore.getInstance().collection("chat_rooms");
    }

    public static CollectionReference getChatMessages(String roomId) {
        return FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId).collection("messages");
    }

    public interface OnOrderItemsLoadedListener {
        void onItemsLoaded(List<CollectionReference> itemCollections);
    }
    public static FirebaseFirestore getFirestore() {
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        return firestore;
    }
}