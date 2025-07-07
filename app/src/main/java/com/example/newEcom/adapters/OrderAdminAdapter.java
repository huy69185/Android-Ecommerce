package com.example.newEcom.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

public class OrderAdminAdapter extends FirestoreRecyclerAdapter<OrderItemModel, OrderAdminAdapter.OrderAdminViewHolder> {
    private Context context;

    public OrderAdminAdapter(@NonNull FirestoreRecyclerOptions<OrderItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public OrderAdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_order, parent, false);
        if (view.getContext() instanceof AppCompatActivity) {
            // Ép kiểu chỉ khi context là AppCompatActivity
        }
        return new OrderAdminViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderAdminViewHolder holder, int position, @NonNull OrderItemModel model) {
        holder.orderIdTextView.setText("Order ID: " + model.getOrderId());
        holder.statusTextView.setText("Status: " + model.getStatus());

        // Hiển thị button dựa trên trạng thái
        if ("Pending".equals(model.getStatus())) {
            holder.deliveryBtn.setVisibility(View.VISIBLE);
            holder.finishBtn.setVisibility(View.GONE);
        } else if ("Delivery".equals(model.getStatus())) {
            holder.deliveryBtn.setVisibility(View.GONE);
            holder.finishBtn.setVisibility(View.VISIBLE);
        } else {
            holder.deliveryBtn.setVisibility(View.GONE);
            holder.finishBtn.setVisibility(View.GONE);
        }

        holder.deliveryBtn.setOnClickListener(v -> {
            updateStatus(holder.getAdapterPosition(), "Delivery");
        });

        holder.finishBtn.setOnClickListener(v -> {
            updateStatus(holder.getAdapterPosition(), "Confirm");
        });
    }

    private void updateStatus(int position, String newStatus) {
        DocumentSnapshot snapshot = getSnapshots().getSnapshot(position);
        FirebaseUtil.getAllOrderItems().document(snapshot.getId()).update("status", newStatus)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show());
    }

    public static class OrderAdminViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdTextView, statusTextView;
        Button deliveryBtn, finishBtn;

        public OrderAdminViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdTextView = itemView.findViewById(R.id.orderIdTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            deliveryBtn = itemView.findViewById(R.id.deliveryBtn);
            finishBtn = itemView.findViewById(R.id.finishBtn);
        }
    }
}