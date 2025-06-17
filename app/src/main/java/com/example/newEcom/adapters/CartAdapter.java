package com.example.newEcom.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.model.CartItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.logging.Logger;

public class CartAdapter extends FirestoreRecyclerAdapter<CartItemModel, CartAdapter.CartViewHolder> {

    private final Context context;
    private AppCompatActivity activity;
    private int[] stock = new int[1];
    private double totalPrice = 0;

    public CartAdapter(@NonNull FirestoreRecyclerOptions<CartItemModel> options, Context context) {
        super(options);
        this.context = context;
        if (context instanceof AppCompatActivity) {
            this.activity = (AppCompatActivity) context;
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_adapter, parent, false);
        if (activity == null && context instanceof AppCompatActivity) {
            activity = (AppCompatActivity) context;
        }
        return new CartViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull CartItemModel model) {
        if (activity != null) {
            activity.findViewById(R.id.emptyCartImageView).setVisibility(View.INVISIBLE);
        }

        holder.productName.setText(model.getName());
        holder.singleProductPrice.setText(String.format("$%.2f", model.getPrice()));
        holder.productPrice.setText(String.format("$%.2f", model.getPrice() * model.getQuantity()));
        holder.originalPrice.setText(String.format("$%.2f", model.getOriginalPrice()));
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.productQuantity.setText(String.valueOf(model.getQuantity()));
        Picasso.get().load(model.getImage()).into(holder.productCartImage, new Callback() {
            @Override
            public void onSuccess() {
                if (holder.getBindingAdapterPosition() == getSnapshots().size() - 1) {
                    if (activity != null) {
                        ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);
                        activity.findViewById(R.id.mainLinearLayout).setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Logger.getLogger("PicassoError").severe("Failed to load image: " + model.getImage() + ", Error: " + e.getMessage());
            }
        });

        holder.plusBtn.setOnClickListener(v -> changeQuantity(model, true));
        holder.minusBtn.setOnClickListener(v -> changeQuantity(model, false));
    }

    private void calculateTotalPrice() {
        totalPrice = 0;
        for (CartItemModel model : getSnapshots()) {
            totalPrice += model.getPrice() * model.getQuantity();
        }
        updateTotalPrice();
    }

    public void addToCart(CartItemModel item) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(context, "Please log in to add items to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUtil.getCartItems().whereEqualTo("productId", item.getProductId())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            item.setTimestamp(Timestamp.now());
                            item.setQuantity(1);
                            FirebaseUtil.getCartItems().add(item)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(context, "Added to cart!", Toast.LENGTH_SHORT).show();
                                        notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Failed to add to cart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            long currentQuantity = document.getLong("quantity") != null ? document.getLong("quantity") : 0;
                            FirebaseUtil.getCartItems().document(document.getId())
                                    .update("quantity", currentQuantity + 1)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "Quantity updated!", Toast.LENGTH_SHORT).show();
                                        notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Failed to update quantity: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(context, "Query failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void changeQuantity(CartItemModel model, boolean plus) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUtil.getProducts().whereEqualTo("productId", model.getProductId())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        stock[0] = document.getLong("stock") != null ? document.getLong("stock").intValue() : 0;
                    } else {
                        stock[0] = 0;
                        Logger.getLogger("StockQuery").warning("Failed to fetch stock: " + task.getException());
                    }
                });

        FirebaseUtil.getCartItems().whereEqualTo("productId", model.getProductId())
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String docId = document.getId();
                        long quantity = document.getLong("quantity") != null ? document.getLong("quantity") : 0;
                        if (plus) {
                            if (quantity < stock[0]) {
                                FirebaseUtil.getCartItems().document(docId).update("quantity", quantity + 1)
                                        .addOnSuccessListener(aVoid -> {
                                            calculateTotalPrice(); // Tính lại tổng giá tiền
                                        })
                                        .addOnFailureListener(e -> Logger.getLogger("UpdateQuantity").severe("Failed to increase quantity: " + e.getMessage()));
                            } else {
                                Toast.makeText(context, "Max stock available: " + stock[0], Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            if (quantity > 1) {
                                FirebaseUtil.getCartItems().document(docId).update("quantity", quantity - 1)
                                        .addOnSuccessListener(aVoid -> {
                                            calculateTotalPrice(); // Tính lại tổng giá tiền
                                        })
                                        .addOnFailureListener(e -> Logger.getLogger("UpdateQuantity").severe("Failed to decrease quantity: " + e.getMessage()));
                            } else {
                                FirebaseUtil.getCartItems().document(docId).delete()
                                        .addOnSuccessListener(aVoid -> {
                                            calculateTotalPrice(); // Tính lại tổng giá tiền
                                        })
                                        .addOnFailureListener(e -> Logger.getLogger("DeleteItem").severe("Failed to delete item: " + e.getMessage()));
                            }
                        }
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).addOrRemoveBadge();
                        }
                    } else {
                        Logger.getLogger("CartQuery").warning("Failed to fetch cart item: " + task.getException());
                    }
                });
    }

    private void updateTotalPrice() {
        Intent intent = new Intent("price");
        intent.putExtra("totalPrice", totalPrice); // Truyền double
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        Logger.getLogger("CartAdapter").info("Data changed, item count: " + getItemCount());
        calculateTotalPrice(); // Tính lại tổng giá tiền khi dữ liệu thay đổi
        if (getItemCount() == 0) {
            if (activity != null) {
                ShimmerFrameLayout shimmerLayout = activity.findViewById(R.id.shimmerLayout);
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                activity.findViewById(R.id.mainLinearLayout).setVisibility(View.VISIBLE);
                activity.findViewById(R.id.emptyCartImageView).setVisibility(View.VISIBLE);
            } else if (context instanceof Activity) {
                Activity contextActivity = (Activity) context;
                ShimmerFrameLayout shimmerLayout = contextActivity.findViewById(R.id.shimmerLayout);
                shimmerLayout.stopShimmer();
                shimmerLayout.setVisibility(View.GONE);
                contextActivity.findViewById(R.id.mainLinearLayout).setVisibility(View.VISIBLE);
                contextActivity.findViewById(R.id.emptyCartImageView).setVisibility(View.VISIBLE);
            }
        } else {
            if (activity != null) {
                activity.findViewById(R.id.emptyCartImageView).setVisibility(View.INVISIBLE);
            } else if (context instanceof Activity) {
                ((Activity) context).findViewById(R.id.emptyCartImageView).setVisibility(View.INVISIBLE);
            }
        }
    }

    public class CartViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productPrice, singleProductPrice, productQuantity, minusBtn, plusBtn, originalPrice;
        ImageView productCartImage;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.nameTextView);
            singleProductPrice = itemView.findViewById(R.id.priceTextView1);
            productPrice = itemView.findViewById(R.id.priceTextView);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            productQuantity = itemView.findViewById(R.id.quantityTextView);
            productCartImage = itemView.findViewById(R.id.productImageCart);
            minusBtn = itemView.findViewById(R.id.minusBtn);
            plusBtn = itemView.findViewById(R.id.plusBtn);
        }
    }
}