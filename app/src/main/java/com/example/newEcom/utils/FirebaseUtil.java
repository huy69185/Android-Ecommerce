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
        return FirebaseFirestore.getInstance().collection("chat_rooms").document(roomId).collection("messages");
    }

    public interface OnOrderItemsLoadedListener {
        void onItemsLoaded(List<Map<String, Object>> orderItemsData); // Thay đổi tham số
    }

    public static FirebaseFirestore getFirestore() {
        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        return firestore;
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
}