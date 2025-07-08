package com.example.newEcom.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.OrderDetailsActivity;
import com.example.newEcom.model.OrderItemModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class OrderAdminAdapter extends FirestoreRecyclerAdapter<OrderItemModel, OrderAdminAdapter.OrderAdminViewHolder> {
    private static final String TAG = "OrderAdminAdapter";
    private Context context;

    public OrderAdminAdapter(@NonNull FirestoreRecyclerOptions<OrderItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public OrderAdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_order, parent, false);
        return new OrderAdminViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderAdminViewHolder holder, int position, @NonNull OrderItemModel model) {
        holder.productName.setText(model.getName());
        holder.productPrice.setText(String.format("%,.0f USD", model.getPrice()));
        holder.statusTextView.setText("Status: " + model.getStatus());

        // Truyền cả ID cha và ID item
        String orderParentId = getSnapshots().getSnapshot(position).getReference().getParent().getParent().getId(); // ID của document cha
        String itemId = getSnapshots().getSnapshot(position).getId(); // ID của item trong subcollection
        holder.detailArrow.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailsActivity.class);
            intent.putExtra("orderParentId", orderParentId); // Truyền ID cha
            intent.putExtra("itemId", itemId); // Truyền ID item
            intent.putExtra("status", model.getStatus());
            context.startActivity(intent);
            Log.d(TAG, "Navigating to OrderDetailsActivity for Order Parent ID: " + orderParentId + ", Item ID: " + itemId);
        });
    }

    public static class OrderAdminViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, statusTextView, detailArrow;

        public OrderAdminViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            detailArrow = itemView.findViewById(R.id.detailArrow);
        }
    }
}