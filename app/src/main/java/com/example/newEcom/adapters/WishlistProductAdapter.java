package com.example.newEcom.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.fragments.ProductFragment;
import com.example.newEcom.model.CartItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import android.util.Log;

public class WishlistProductAdapter extends FirestoreRecyclerAdapter<CartItemModel, WishlistProductAdapter.WishlistProductViewHolder> {
    private final Context context;
    private AppCompatActivity activity;

    public WishlistProductAdapter(@NonNull FirestoreRecyclerOptions<CartItemModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public WishlistProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wishlist_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new WishlistProductViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull WishlistProductAdapter.WishlistProductViewHolder holder, int position, @NonNull CartItemModel product) {
        holder.productNameTextView.setText(product.getName());
        Picasso.get().load(product.getImage()).into(holder.productImageView);
        holder.productPriceTextView.setText(getStringWithPrice(R.string.price_format, product.getPrice()));
        holder.originalPrice.setText(getStringWithPrice(R.string.original_price_format, product.getOriginalPrice()));
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        int discountPerc = (int) ((product.getOriginalPrice() - product.getPrice()) * 100 / product.getOriginalPrice());
        holder.discountPercentage.setText(getStringWithDiscount(R.string.discount_format, discountPerc));

        holder.productLinearLayout.setOnClickListener(v -> {
            long productId = product.getProductId(); // Giả sử getProductId() trả về long
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_frame_layout, ProductFragment.newInstance((int) productId)) // Ép sang int nếu cần
                    .addToBackStack(null)
                    .commit();
        });

        holder.addToCartBtn.setOnClickListener(v -> addToCart(product, stock -> {
            if (FirebaseUtil.getCartItems() != null) {
                FirebaseUtil.getCartItems().whereEqualTo("productId", product.getProductId())
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                boolean documentExists = false;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    documentExists = true;
                                    String docId = document.getId();
                                    Long quantityLong = (Long) document.getData().get("quantity");
                                    int quantity = quantityLong != null ? quantityLong.intValue() : 0;
                                    if (quantity < stock) {
                                        FirebaseUtil.getCartItems().document(docId).update("quantity", quantity + 1);
                                        Toast.makeText(context, R.string.added_to_cart, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, getStringWithStock(R.string.max_stock_available, stock), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                if (!documentExists) {
                                    CartItemModel cartItem = new CartItemModel(product.getProductId(), product.getName(), product.getImage(), 1, product.getPrice(), product.getOriginalPrice(), Timestamp.now());
                                    FirebaseUtil.getCartItems().add(cartItem);
                                    Toast.makeText(context, R.string.added_to_cart, Toast.LENGTH_SHORT).show();
                                }
                                MainActivity activity = (MainActivity) context;
                                activity.addOrRemoveBadge();
                            }
                        });
            }
        }));

        holder.removeWishlistBtn.setOnClickListener(v -> {
            if (FirebaseUtil.getWishlistItems() != null) {
                FirebaseUtil.getWishlistItems().whereEqualTo("productId", product.getProductId())
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String docId = document.getId();
                                    FirebaseUtil.getWishlistItems().document(docId).delete()
                                            .addOnCompleteListener(deleteTask -> {
                                            });
                                }
                            }
                        });
            }
        });
    }

    private void addToCart(CartItemModel product, MyCallback myCallback) {
        if (FirebaseUtil.getProducts() != null) {
            FirebaseUtil.getProducts().whereEqualTo("productId", product.getProductId())
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Long stockLong = (Long) document.getData().get("stock");
                                if (stockLong != null) {
                                    Log.d("StockValue", "Stock value: " + stockLong);
                                    long stock = stockLong.longValue();
                                    myCallback.onCallback(stock);
                                } else {
                                    Log.d("StockValue", "Stock is null");
                                    myCallback.onCallback(0);
                                }
                            }
                        }
                    });
        }
    }

    private String getStringWithPrice(int resId, double price) {
        return context.getString(resId, price);
    }

    private String getStringWithDiscount(int resId, int discount) {
        return context.getString(resId, discount);
    }

    private String getStringWithStock(int resId, long stock) { // Thay int thành long
        return context.getString(resId, stock);
    }

    public static class WishlistProductViewHolder extends RecyclerView.ViewHolder {
        TextView productNameTextView, productPriceTextView, originalPrice, discountPercentage;
        ImageView productImageView;
        LinearLayout productLinearLayout;
        Button addToCartBtn, removeWishlistBtn;

        public WishlistProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImage);
            productNameTextView = itemView.findViewById(R.id.productName);
            productPriceTextView = itemView.findViewById(R.id.productPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            discountPercentage = itemView.findViewById(R.id.discountPercentage);
            productLinearLayout = itemView.findViewById(R.id.productLinearLayout);
            addToCartBtn = itemView.findViewById(R.id.addToCartBtn);
            removeWishlistBtn = itemView.findViewById(R.id.removeWishlistBtn);
        }
    }

    public interface MyCallback {
        void onCallback(long stock); // Thay int thành long
    }
}