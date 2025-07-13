package com.example.newEcom.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.fragments.AdminOrderFragment;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";
    private LinearLayout logoutBtn;
    private CardView addProductBtn, modifyProductBtn, addCategoryBtn, modifyCategoryBtn, addBannerBtn, modifyBannerBtn, chatListBtn;
    private TextView countOrders, priceOrders;
    private CardView ordersCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        logoutBtn = findViewById(R.id.logoutBtn);
        addProductBtn = findViewById(R.id.addProductBtn);
        modifyProductBtn = findViewById(R.id.modifyProductBtn);
        addCategoryBtn = findViewById(R.id.addCategoryBtn);
        modifyCategoryBtn = findViewById(R.id.modifyCategoryBtn);
        addBannerBtn = findViewById(R.id.addBannerBtn);
        modifyBannerBtn = findViewById(R.id.modifyBannerBtn);
        chatListBtn = findViewById(R.id.chatListBtn);
        countOrders = findViewById(R.id.countOrders);
        priceOrders = findViewById(R.id.priceOrders);
        ordersCard = findViewById(R.id.ordersCard);

        getDetails();

        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        addProductBtn.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));
        modifyProductBtn.setOnClickListener(v -> startActivity(new Intent(this, ModifyProductActivity.class)));
        addCategoryBtn.setOnClickListener(v -> startActivity(new Intent(this, AddCategoryActivity.class)));
        modifyCategoryBtn.setOnClickListener(v -> startActivity(new Intent(this, ModifyCategoryActivity.class)));
        addBannerBtn.setOnClickListener(v -> startActivity(new Intent(this, AddBannerActivity.class)));
        modifyBannerBtn.setOnClickListener(v -> startActivity(new Intent(this, ModifyBannerActivity.class)));
        chatListBtn.setOnClickListener(v -> startActivity(new Intent(this, ChatListActivity.class)));
        ordersCard.setOnClickListener(v -> {
            String currentUserId = FirebaseUtil.getCurrentUserId();
            Log.d(TAG, "User ID: " + currentUserId + ", Admin ID: " + FirebaseUtil.ADMIN_USER_ID);
            if (currentUserId != null && currentUserId.equals(FirebaseUtil.ADMIN_USER_ID)) {
                Intent intent = new Intent(this, AdminOrderActivity.class);
                startActivity(intent);
                Log.d(TAG, "Navigating to AdminOrderActivity");
            } else {
                Toast.makeText(AdminActivity.this, "Only admin can view orders", Toast.LENGTH_SHORT).show();
            }
        });
        updateDashboardTotalPrice();

        // Kiểm tra quyền thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Lấy và lưu token không đồng bộ
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                String userId = firebaseAuth.getCurrentUser().getUid();
                new Thread(() -> {
                    try {
                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String token = task.getResult();
                                Log.d("FCM", "FCM Token fetched for userId: " + userId + ", Token: " + token);
                                saveTokenToFirestore(userId, token); // Lưu token trong luồng riêng
                            } else {
                                Log.e("FCM", "Failed to get FCM token", task.getException());
                            }
                        }).addOnFailureListener(e -> Log.e("FCM", "Error getting token", e));
                    } catch (Exception e) {
                        Log.e("FCM", "Exception in token retrieval", e);
                    }
                }).start();
            }
        });
    }

    // Phương thức lưu token vào Firestore (không đồng bộ)
    private void saveTokenToFirestore(String userId, String token) {
        runOnUiThread(() -> {
            FirebaseFirestore db = FirebaseUtil.getFirestore();
            Map<String, Object> userData = new HashMap<>();
            userData.put("fcmToken", token);
            userData.put("userName", FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : userId);
            db.collection("users").document(userId)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "Token saved successfully for userId: " + userId + ", Token: " + token))
                    .addOnFailureListener(e -> Log.e("FCM", "Failed to save token for userId: " + userId + ", Error: " + e.getMessage()));
        });
    }

    private void getDetails() {
        Log.d(TAG, "Fetching details from Firestore");
        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Long count = document.getLong("countOfOrderedItems");
                            Long price = document.getLong("priceOfOrders");
                            Log.d(TAG, "Fetched count: " + count + ", price: " + price);
                            countOrders.setText(count != null ? String.valueOf(count) : "0");
                            priceOrders.setText(price != null ? String.format("%.2f USD", price / 100.0) : "0.00 USD");
                        } else {
                            Log.w(TAG, "Document does not exist");
                            countOrders.setText("0");
                            priceOrders.setText("₹0.00");
                            initializeDefaultDetails();
                        }
                    } else {
                        Log.e(TAG, "Error fetching dashboard data: ", task.getException());
                        countOrders.setText("0");
                        priceOrders.setText("0.00 USD");
                    }
                });
    }

    private void initializeDefaultDetails() {
        HashMap<String, Object> defaultData = new HashMap<>();
        defaultData.put("countOfOrderedItems", 0L);
        defaultData.put("priceOfOrders", 0L);
        FirebaseUtil.getDetails().set(defaultData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Default data initialized successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error initializing default data: ", e));
    }

    private void loadOrderCount() {
        Log.d(TAG, "Loading order count from Firestore");
        FirebaseUtil.getAllOrderItems(new FirebaseUtil.OnOrderItemsLoadedListener() {
            @Override
            public void onItemsLoaded(List<Map<String, Object>> orderItemsData) {
                if (orderItemsData == null || orderItemsData.isEmpty()) {
                    Log.w(TAG, "No order items collections found");
                    countOrders.setText("0");
                    FirebaseUtil.getDetails().update("countOfOrderedItems", 0L)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Dashboard updated with count: 0"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error updating dashboard count: ", e));
                    return;
                }

                List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                for (Map<String, Object> data : orderItemsData) {
                    CollectionReference collection = (CollectionReference) data.get("itemsCollection");
                    tasks.add(collection.get());
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    long totalOrderCount = 0;
                    for (Object result : results) {
                        QuerySnapshot itemsSnapshot = (QuerySnapshot) result;
                        totalOrderCount += itemsSnapshot.size();
                        Log.d(TAG, "Order count from subcollection: " + itemsSnapshot.size());
                    }
                    final long finalCount = totalOrderCount;
                    if (finalCount > 0) {
                        countOrders.setText(String.valueOf(finalCount));
                        FirebaseUtil.getDetails().update("countOfOrderedItems", finalCount)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Dashboard updated with count: " + finalCount))
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating dashboard count: ", e));
                    } else {
                        Log.w(TAG, "No items found in subcollections");
                        countOrders.setText("0");
                        FirebaseUtil.getDetails().update("countOfOrderedItems", 0L)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Dashboard updated with count: 0"))
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating dashboard count: ", e));
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error aggregating order count: ", e);
                    countOrders.setText("0");
                });
            }
        });
    }

    private void updateDashboardTotalPrice() {
        Log.d(TAG, "Updating dashboard total price");
        FirebaseUtil.getAllOrderItems(new FirebaseUtil.OnOrderItemsLoadedListener() {
            @Override
            public void onItemsLoaded(List<Map<String, Object>> orderItemsData) {
                if (orderItemsData == null || orderItemsData.isEmpty()) {
                    Log.w(TAG, "No order items collections found");
                    FirebaseUtil.getDetails().update("priceOfOrders", 0L)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Dashboard price updated to 0");
                                getDetails();
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error updating dashboard price: ", e));
                    return;
                }

                List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                for (Map<String, Object> data : orderItemsData) {
                    CollectionReference collection = (CollectionReference) data.get("itemsCollection");
                    tasks.add(collection.get());
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    double totalPrice = 0;
                    for (Object result : results) {
                        QuerySnapshot snapshot = (QuerySnapshot) result;
                        for (QueryDocumentSnapshot document : snapshot) {
                            OrderItemModel item = document.toObject(OrderItemModel.class);
                            if ("confirm".equalsIgnoreCase(item.getStatus())) { // Chỉ tính khi status là "confirm"
                                totalPrice += item.getPrice();
                            }
                        }
                    }
                    Log.d(TAG, "Calculated total price for confirmed orders: " + totalPrice);
                    FirebaseUtil.getDetails().update("priceOfOrders", (long) (totalPrice * 100))
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Dashboard price updated successfully");
                                getDetails();
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error updating dashboard price: ", e));
                }).addOnFailureListener(e -> Log.e(TAG, "Error fetching items for price calculation: ", e));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        getDetails();
        loadOrderCount();
        updateDashboardTotalPrice();
    }
}