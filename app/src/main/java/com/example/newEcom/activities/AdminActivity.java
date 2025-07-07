package com.example.newEcom.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.newEcom.R;
import com.example.newEcom.fragments.AdminOrderFragment;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

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
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new AdminOrderFragment());
            transaction.addToBackStack(null);
            transaction.commit();
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
                            priceOrders.setText(price != null ? String.format("₹%.2f", price / 100.0) : "₹0.00");
                        } else {
                            Log.w(TAG, "Document does not exist");
                            countOrders.setText("0");
                            priceOrders.setText("₹0.00");
                            initializeDefaultDetails();
                        }
                    } else {
                        Log.e(TAG, "Error fetching dashboard data: ", task.getException());
                        countOrders.setText("0");
                        priceOrders.setText("₹0.00");
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
        FirebaseUtil.getAllOrderItems().get() // Truy cập collection "orders"
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                            for (DocumentSnapshot doc : snapshot) {
                                String docId = doc.getId(); // Lưu ID của document
                                Task<QuerySnapshot> itemsTask = doc.getReference().collection("items").get();
                                tasks.add(itemsTask);
                            }
                            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                                long totalOrderCount = 0;
                                for (int i = 0; i < results.size(); i++) {
                                    QuerySnapshot itemsSnapshot = (QuerySnapshot) results.get(i);
                                    totalOrderCount += itemsSnapshot.size();
                                    // Sử dụng docId từ vòng lặp trước
                                    String docId = snapshot.getDocuments().get(i).getId();
                                    Log.d(TAG, "Order count from subcollection " + docId + ": " + itemsSnapshot.size());
                                }
                                if (totalOrderCount > 0) {
                                    final long finalCount = totalOrderCount;
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
                        } else {
                            Log.w(TAG, "No orders found in Firestore");
                            countOrders.setText("0");
                            FirebaseUtil.getDetails().update("countOfOrderedItems", 0L)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Dashboard updated with count: 0"))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating dashboard count: ", e));
                        }
                    } else {
                        Log.e(TAG, "Error fetching order list: ", task.getException());
                        countOrders.setText("0");
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        getDetails();
        loadOrderCount();
    }
}