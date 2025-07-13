package com.example.newEcom.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class FirebaseUtil {
    public static final String ADMIN_USER_ID = "3QRm0nJTKnU9OpVul6N7kEV0OFF3";
    private static FirebaseFirestore firestore;
    private static final String TAG = "FirebaseUtil";

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
                    List<Map<String, Object>> orderItemsData = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String orderParentId = doc.getId();
                        CollectionReference itemsCollection = doc.getReference().collection("items");
                        Map<String, Object> data = new HashMap<>();
                        data.put("orderParentId", orderParentId);
                        data.put("itemsCollection", itemsCollection);
                        orderItemsData.add(data);
                    }
                    listener.onItemsLoaded(orderItemsData);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseUtil", "Error fetching order items: ", e);
                    listener.onItemsLoaded(null);
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
        return FirebaseFirestore.getInstance().collection("chats").document(roomId).collection("messages");
    }
    public static CollectionReference getNotifications() {
        FirebaseFirestore db = getFirestore();
        if (db != null) {
            return db.collection("notifications");
        }
        Log.e(TAG, "Firestore is null, cannot get notifications collection");
        return null;
    }

    public interface OnOrderItemsLoadedListener {
        void onItemsLoaded(List<Map<String, Object>> orderItemsData); // Thay đổi tham số
    }

    public static FirebaseFirestore getFirestore() {
        return FirebaseFirestore.getInstance();
    }

    public static void updateDashboardTotalPrice() {
        AtomicReference<Double> totalRevenue = new AtomicReference<>(0.0); // Tổng doanh thu
        FirebaseUtil.getFirestore().collection("orders")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        FirebaseUtil.getDetails().update("totalPrice", 0.0)
                                .addOnSuccessListener(aVoid -> Log.d("Dashboard", "Tổng doanh thu đã được cập nhật: 0.0"))
                                .addOnFailureListener(e -> Log.e("Dashboard", "Lỗi cập nhật tổng doanh thu", e));
                        return;
                    }

                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String orderId = doc.getId();
                        String status = doc.getString("status"); // Lấy trạng thái của đơn hàng
                        if ("Confirm".equals(status)) { // Chỉ xử lý nếu trạng thái là "Confirm"
                            Task<QuerySnapshot> task = doc.getReference().collection("items").get();
                            tasks.add(task);
                        }
                    }

                    if (tasks.isEmpty()) {
                        FirebaseUtil.getDetails().update("totalPrice", 0.0)
                                .addOnSuccessListener(aVoid -> Log.d("Dashboard", "Không có đơn hàng xác nhận, tổng doanh thu: 0.0"))
                                .addOnFailureListener(e -> Log.e("Dashboard", "Lỗi cập nhật tổng doanh thu", e));
                        return;
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        double finalTotal = 0.0;
                        for (Object result : results) {
                            QuerySnapshot itemSnapshot = (QuerySnapshot) result;
                            double orderTotal = 0;
                            for (DocumentSnapshot item : itemSnapshot) {
                                Double price = item.getDouble("price");
                                Long quantity = item.getLong("quantity");
                                if (price != null && quantity != null) {
                                    orderTotal += price * quantity;
                                }
                            }
                            finalTotal += orderTotal;
                        }
                        totalRevenue.set(finalTotal); // Cập nhật tổng doanh thu
                        FirebaseUtil.getDetails().update("totalPrice", totalRevenue.get())
                                .addOnSuccessListener(aVoid -> Log.d("Dashboard", "Tổng doanh thu đã được cập nhật: " + totalRevenue.get()))
                                .addOnFailureListener(e -> Log.e("Dashboard", "Lỗi cập nhật tổng doanh thu", e));
                    }).addOnFailureListener(e -> Log.e("Dashboard", "Lỗi tổng hợp dữ liệu orders", e));
                })
                .addOnFailureListener(e -> Log.e("Dashboard", "Lỗi lấy dữ liệu orders", e));
    }
    public static void sendChatNotification(String receiverId, String roomId, String message) {
        Log.d(TAG, "sendChatNotification called for receiverId: " + receiverId + ", roomId: " + roomId + ", message: " + message);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(receiverId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "Document fetched for receiverId: " + receiverId + ", exists: " + documentSnapshot.exists());
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("fcmTokens")) {
                            List<String> fcmTokens = (List<String>) documentSnapshot.get("fcmTokens");
                            Log.d(TAG, "fcmTokens found: " + (fcmTokens != null ? fcmTokens.size() : 0));
                            if (fcmTokens != null && !fcmTokens.isEmpty()) {
                                for (String fcmToken : fcmTokens) {
                                    Log.d(TAG, "Sending to fcmToken: " + fcmToken);
                                    Map<String, String> data = new HashMap<>();
                                    data.put("title", "New Chat Message");
                                    data.put("body", message);
                                }
                            } else {
                                Log.w(TAG, "No valid FCM tokens for receiver: " + receiverId);
                            }
                        } else {
                            String fcmToken = documentSnapshot.getString("fcmToken");
                            Log.d(TAG, "Single fcmToken found: " + fcmToken);
                            if (fcmToken != null && !fcmToken.isEmpty()) {
                                Map<String, String> data = new HashMap<>();
                                data.put("title", "New Chat Message");
                                data.put("body", message);
                            } else {
                                Log.w(TAG, "No valid FCM token for receiver: " + receiverId);
                            }
                        }
                    } else {
                        Log.w(TAG, "No user document found for receiver: " + receiverId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching receiver token: ", e));
    }

    public static void sendOrderStatusNotification(String userId, String orderId, String status) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fcmToken = documentSnapshot.getString("token");
                        if (fcmToken != null) {
                            Map<String, String> data = new HashMap<>();
                            data.put("title", "Order Status Update");
                            data.put("body", "Order " + orderId + " status changed to " + status);
                        } else {
                            Log.w(TAG, "No FCM token for user: " + userId);
                        }
                    } else {
                        Log.w(TAG, "No user document for user: " + userId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting user token: ", e));
    }

}