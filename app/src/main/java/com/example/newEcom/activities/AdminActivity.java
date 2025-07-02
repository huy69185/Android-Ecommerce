package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.newEcom.R;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;

public class AdminActivity extends AppCompatActivity {
    private static final String TAG = "AdminActivity";
    private LinearLayout logoutBtn;
    private CardView addProductBtn, modifyProductBtn, addCategoryBtn, modifyCategoryBtn, addBannerBtn, modifyBannerBtn, chatlistBtn;
    private TextView countOrders, priceOrders;

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
        countOrders = findViewById(R.id.countOrders);
        priceOrders = findViewById(R.id.priceOrders);
        chatlistBtn = findViewById(R.id.chatListBtn);

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
        chatlistBtn.setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, ChatListActivity.class));
        });
    }

    private void getDetails() {
        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Long count = document.getLong("countOfOrderedItems");
                            Long price = document.getLong("priceOfOrders");
                            countOrders.setText(count != null ? String.valueOf(count) : "0");
                            priceOrders.setText(price != null ? String.valueOf(price) : "0");
                        } else {
                            countOrders.setText("0");
                            priceOrders.setText("0");
                            initializeDefaultDetails();
                        }
                    } else {
                        Log.e(TAG, "Error fetching dashboard data: ", task.getException());
                        countOrders.setText("0");
                        priceOrders.setText("0");
                    }
                });
    }

    private void initializeDefaultDetails() {
        HashMap<String, Object> defaultData = new HashMap<>();
        defaultData.put("countOfOrderedItems", 0L);
        defaultData.put("priceOfOrders", 0L);
        FirebaseUtil.getDetails().set(defaultData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Default data initialized successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error initializing default data", e));
    }
}