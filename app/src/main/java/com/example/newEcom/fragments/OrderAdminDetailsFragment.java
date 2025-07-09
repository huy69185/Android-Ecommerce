package com.example.newEcom.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.newEcom.R;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class OrderAdminDetailsFragment extends Fragment {
    private static final String TAG = "OrderAdminDetailsFragment";
    private TextView orderIdTextView, nameTextView, emailTextView, phoneTextView, addressTextView, commentTextView, productName;
    private Button deliveryBtn, confirmBtn, cancelBtn;
    private String orderParentId, itemId, currentStatus;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_admin_details, container, false);

        // Ánh xạ các view
        orderIdTextView = view.findViewById(R.id.orderIdTextView);
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        phoneTextView = view.findViewById(R.id.phoneTextView);
        addressTextView = view.findViewById(R.id.addressTextView);
        commentTextView = view.findViewById(R.id.commentTextView);
        productName = view.findViewById(R.id.productName);
        deliveryBtn = view.findViewById(R.id.deliveryBtn);
        confirmBtn = view.findViewById(R.id.confirmBtn);
        cancelBtn = view.findViewById(R.id.cancelBtn);

        // Lấy dữ liệu từ Bundle
        Bundle args = getArguments();
        if (args != null) {
            orderParentId = args.getString("orderParentId");
            itemId = args.getString("itemId");
            currentStatus = args.getString("status", "Unknown");
            if (orderParentId != null && itemId != null) {
                orderIdTextView.setText(itemId); // Hiển thị itemId làm ID
                productName.setText("Amazon Bag"); // Có thể lấy từ model nếu cần
                Log.d(TAG, "Loaded with orderParentId: " + orderParentId + ", itemId: " + itemId + ", status: " + currentStatus);
                updateStatusButtons();
            } else {
                Log.e(TAG, "Missing orderParentId or itemId: " + orderParentId + ", " + itemId);
                Toast.makeText(getActivity(), "Invalid order data", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "No arguments passed to fragment");
            Toast.makeText(getActivity(), "Failed to load order details", Toast.LENGTH_SHORT).show();
        }

        // Xử lý nút back
        view.findViewById(R.id.backBtn).setOnClickListener(v -> getActivity().onBackPressed());

        // Xử lý các nút trạng thái
        deliveryBtn.setOnClickListener(v -> updateStatus(orderParentId, itemId, currentStatus, "Delivery"));
        confirmBtn.setOnClickListener(v -> updateStatus(orderParentId, itemId, currentStatus, "Confirm"));
        cancelBtn.setOnClickListener(v -> updateStatus(orderParentId, itemId, currentStatus, "Cancel"));

        return view;
    }

    private void updateStatusButtons() {
        deliveryBtn.setVisibility(View.GONE);
        confirmBtn.setVisibility(View.GONE);
        cancelBtn.setVisibility(View.GONE);

        if ("Pending".equals(currentStatus)) {
            deliveryBtn.setVisibility(View.VISIBLE);
            cancelBtn.setVisibility(View.VISIBLE);
        } else if ("Delivery".equals(currentStatus)) {
            confirmBtn.setVisibility(View.VISIBLE);
        } else if ("Unknown".equals(currentStatus) || currentStatus == null) {
            deliveryBtn.setVisibility(View.VISIBLE);
            cancelBtn.setVisibility(View.VISIBLE);
        }
    }

    private void updateStatus(String orderParentId, String itemId, String currentStatus, String newStatus) {
        if (orderParentId == null || itemId == null) {
            Log.e(TAG, "Invalid document path: orderParentId=" + orderParentId + ", itemId=" + itemId);
            Toast.makeText(getActivity(), "Invalid order data", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore();
        DocumentReference orderRef = db.collection("orders")
                .document(orderParentId)
                .collection("items")
                .document(itemId);

        // Kiểm tra tồn tại tài liệu trước khi cập nhật
        orderRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Log.e(TAG, "Document does not exist for orderParentId: " + orderParentId + ", itemId: " + itemId);
                Toast.makeText(getActivity(), "Order item not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Current status: " + currentStatus + ", Attempting to set to: " + newStatus);
            // Logic chuyển trạng thái
            if ("Pending".equals(currentStatus) && ("Delivery".equals(newStatus) || "Cancel".equals(newStatus))) {
                orderRef.update("status", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            this.currentStatus = newStatus;
                            updateStatusButtons();
                            Toast.makeText(getActivity(), "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Status successfully updated to " + newStatus);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update status: ", e);
                            Toast.makeText(getActivity(), "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else if ("Delivery".equals(currentStatus) && "Confirm".equals(newStatus)) {
                orderRef.update("status", newStatus)
                        .addOnSuccessListener(aVoid -> {
                            this.currentStatus = newStatus;
                            updateStatusButtons();
                            Toast.makeText(getActivity(), "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Status successfully updated to " + newStatus);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update status: ", e);
                            Toast.makeText(getActivity(), "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else if ("Unknown".equals(currentStatus) && "Pending".equals(newStatus)) {
                orderRef.set(new HashMap<String, Object>() {{
                            put("status", newStatus);
                        }})
                        .addOnSuccessListener(aVoid -> {
                            this.currentStatus = newStatus;
                            updateStatusButtons();
                            Toast.makeText(getActivity(), "Status set to " + newStatus, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Status successfully set to " + newStatus);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to set initial status: ", e);
                            Toast.makeText(getActivity(), "Failed to set status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(getActivity(), "Invalid status transition. Follow: Pending -> Delivery -> Confirm or Pending -> Cancel", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Invalid transition: current=" + currentStatus + ", new=" + newStatus);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking document existence: ", e);
            Toast.makeText(getActivity(), "Error checking order item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}