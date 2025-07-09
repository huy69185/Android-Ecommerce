package com.example.newEcom.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.OrderDetailsActivity;
import com.example.newEcom.model.OrderItemModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class OrderAdminAdapter extends RecyclerView.Adapter<OrderAdminAdapter.OrderAdminViewHolder> {
    private static final String TAG = "OrderAdminAdapter";
    private Context context;
    private List<OrderItemModel> items; // Danh sách thủ công

    public OrderAdminAdapter(com.firebase.ui.firestore.FirestoreRecyclerOptions<OrderItemModel> options, Context context) {
        this.context = context;
        this.items = new ArrayList<>(); // Khởi tạo danh sách rỗng
    }

    @NonNull
    @Override
    public OrderAdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_order, parent, false);
        return new OrderAdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderAdminViewHolder holder, int position) {
        if (position < items.size()) {
            OrderItemModel model = items.get(position);
            holder.productName.setText(model.getName() != null ? model.getName() : "N/A");
            holder.productPrice.setText(String.format("%,.0f USD", model.getPrice()));
            holder.statusTextView.setText("Status: " + (model.getStatus() != null ? model.getStatus() : "N/A"));

            if (model.getImage() != null && !model.getImage().isEmpty()) {
                Picasso.get().load(model.getImage()).into(holder.productImage);
            } else {
                holder.productImage.setImageResource(R.drawable.temp);
            }

            holder.detailArrow.setOnClickListener(v -> {
                String orderParentId = model.getOrderParentId();
                String itemId = model.getItemId();
                if (orderParentId != null && itemId != null && model.getStatus() != null) {
                    Intent intent = new Intent(context, OrderDetailsActivity.class);
                    intent.putExtra("orderParentId", orderParentId);
                    intent.putExtra("itemId", itemId);
                    intent.putExtra("status", model.getStatus());
                    context.startActivity(intent);
                    Log.d(TAG, "Navigating to OrderDetailsActivity for Order Parent ID: " + orderParentId + ", Item ID: " + itemId + ", Status: " + model.getStatus());
                } else {
                    Log.e(TAG, "Invalid data: orderParentId=" + orderParentId + ", itemId=" + itemId + ", status=" + model.getStatus());
                    Toast.makeText(context, "Invalid order data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Phương thức để cập nhật danh sách
    public void setItems(List<OrderItemModel> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public static class OrderAdminViewHolder extends RecyclerView.ViewHolder {
        public TextView productName, productPrice, statusTextView, detailArrow;
        public ImageView productImage;

        public OrderAdminViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            detailArrow = itemView.findViewById(R.id.detailArrow);
            productImage = itemView.findViewById(R.id.productImage);
        }
    }
}