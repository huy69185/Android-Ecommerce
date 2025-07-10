package com.example.newEcom.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.fragments.OrderDetailsFragment;
import com.example.newEcom.model.OrderItemModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import androidx.core.content.ContextCompat;

public class OrderListAdapter extends FirestoreRecyclerAdapter<OrderItemModel, OrderListAdapter.OrderListViewHolder> {
    private Context context;
    private AppCompatActivity activity;

    public OrderListAdapter(@NonNull FirestoreRecyclerOptions<OrderItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public OrderListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new OrderListAdapter.OrderListViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull OrderListViewHolder holder, int position, @NonNull OrderItemModel model) {
        holder.productName.setText(model.getName());
        Timestamp timestamp = model.getTimestamp();
        String time = new SimpleDateFormat("dd MMM yyyy").format(timestamp.toDate());
        holder.orderDate.setText(time);
        Picasso.get().load(model.getImage()).into(holder.productImage);
        holder.orderTotalTextView.setText("Tổng: ₹" + (model.getPrice() * model.getQuantity()));

        String status = model.getStatus();
        holder.orderStatusTextView.setText(status != null ? status : "Không xác định");
        int backgroundColor;
        switch (status != null ? status : "") {
            case "Pending":
                backgroundColor = ContextCompat.getColor(context, R.color.status_pending);
                holder.detailsArrow.setVisibility(View.GONE);
                break;
            case "Delivery":
                backgroundColor = ContextCompat.getColor(context, R.color.status_delivery);
                holder.detailsArrow.setVisibility(View.GONE);
                break;
            case "Confirm":
                backgroundColor = ContextCompat.getColor(context, R.color.status_confirm);
                holder.detailsArrow.setVisibility(View.VISIBLE);
                break;
            case "Cancel":
                backgroundColor = ContextCompat.getColor(context, R.color.status_cancel);
                holder.detailsArrow.setVisibility(View.GONE);
                break;
            default:
                backgroundColor = ContextCompat.getColor(context, R.color.gray);
                holder.detailsArrow.setVisibility(View.GONE);
                break;
        }
        holder.orderStatusTextView.setBackgroundColor(backgroundColor);
        if ("Confirm".equals(status)) {
            holder.itemView.setOnClickListener(v -> {
                if (context instanceof MainActivity) {
                    MainActivity activity = (MainActivity) context;
                    Bundle bundle = new Bundle();
                    bundle.putString("orderParentId", model.getOrderParentId());
                    bundle.putInt("orderId", model.getOrderId());
                    OrderDetailsFragment fragment = new OrderDetailsFragment();
                    fragment.setArguments(bundle);
                    activity.replaceFragment(fragment);
                }
            });
        } else {
            // Xóa onClickListener nếu có, để đảm bảo không có hành động khi click
            holder.itemView.setOnClickListener(null);
        }
    }

    public class OrderListViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage, detailsArrow; // SỬA: Thêm detailsArrow
        TextView productName, orderDate, orderStatusTextView, orderTotalTextView; // SỬA: Sửa kiểu dữ liệu

        public OrderListViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImageOrder);
            productName = itemView.findViewById(R.id.nameTextView);
            orderDate = itemView.findViewById(R.id.dateTextView);
            orderStatusTextView = itemView.findViewById(R.id.orderStatusTextView); // SỬA: Đúng id
            orderTotalTextView = itemView.findViewById(R.id.orderTotalTextView); // SỬA: Đúng id
            detailsArrow = itemView.findViewById(R.id.detailsArrow); // SỬA: Thêm khởi tạo
        }
    }
}
