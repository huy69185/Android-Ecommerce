package com.example.newEcom.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.newEcom.R;
import com.example.newEcom.fragments.OrderAdminDetailsFragment;

public class OrderDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        // Lấy dữ liệu từ Intent
        int orderId = getIntent().getIntExtra("orderId", -1);
        String status = getIntent().getStringExtra("status");

        // Thêm OrderAdminDetailsFragment
        OrderAdminDetailsFragment fragment = new OrderAdminDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("orderId", orderId);
        args.putString("status", status);
        fragment.setArguments(args);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container_details, fragment);
        transaction.commit();
    }

    // Xử lý nút back
    public void onBackPressed() {
        super.onBackPressed();
        finish(); // Đóng activity và quay lại activity trước
    }
}