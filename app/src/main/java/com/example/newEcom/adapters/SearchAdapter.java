package com.example.newEcom.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.fragments.ProductFragment;
import com.example.newEcom.model.CartItemModel;
import com.example.newEcom.model.ProductModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;

public class SearchAdapter extends FirestoreRecyclerAdapter<ProductModel, SearchAdapter.SearchViewHolder> {
    private final Context context; // Đánh dấu final
    private AppCompatActivity activity;

    public SearchAdapter(@NonNull FirestoreRecyclerOptions<ProductModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new SearchViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull SearchAdapter.SearchViewHolder holder, int position, @NonNull ProductModel product) {
        holder.productNameTextView.setText(product.getName());
        Picasso.get().load(product.getImage()).into(holder.productImageView);
        holder.productPriceTextView.setText(getStringWithPrice(R.string.price_format, product.getPrice()));
        holder.originalPrice.setText(getStringWithPrice(R.string.original_price_format, product.getOriginalPrice()));
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        double originalPrice = product.getOriginalPrice() > 0 ? product.getOriginalPrice() : 1.0;
        int discountPerc = 0;
        if (originalPrice > 0) {
            discountPerc = (int) ((product.getDiscount() / originalPrice) * 100);
        } else if (product.getDiscount() > 0) {
            discountPerc = 100;
        }
        holder.discountPercentage.setText(getStringWithDiscount(R.string.discount_format, discountPerc));

        DecimalFormat df = new DecimalFormat("#.#");
        float rating = Float.parseFloat(df.format(product.getRating()));
        holder.ratingBar.setRating(rating);
        holder.ratingTextView.setText(String.valueOf(rating)); // Sử dụng String.valueOf thay vì nối chuỗi
        holder.noOfRatingTextView.setText(getStringWithRating(R.string.rating_count_format, product.getNoOfRating()));

        holder.productLinearLayout.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(product);
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame_layout, fragment)
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
    }

    private void addToCart(ProductModel product, MyCallback myCallback) {
        if (FirebaseUtil.getProducts() != null) {
            FirebaseUtil.getProducts().whereEqualTo("productId", product.getProductId())
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Long stockLong = (Long) document.getData().get("stock");
                                if (stockLong != null) {
                                    long stock = stockLong.longValue();
                                    myCallback.onCallback(stock);
                                } else {
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

    private String getStringWithStock(int resId, long stock) {
        return context.getString(resId, stock);
    }

    private String getStringWithRating(int resId, int ratingCount) {
        return context.getString(resId, ratingCount);
    }

    public class SearchViewHolder extends RecyclerView.ViewHolder {
        TextView productNameTextView, productPriceTextView, originalPrice, discountPercentage;
        ImageView productImageView;
        LinearLayout productLinearLayout;
        Button addToCartBtn;
        RatingBar ratingBar;
        TextView ratingTextView, noOfRatingTextView;

        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImage);
            productNameTextView = itemView.findViewById(R.id.productName);
            productPriceTextView = itemView.findViewById(R.id.productPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            discountPercentage = itemView.findViewById(R.id.discountPercentage);
            productLinearLayout = itemView.findViewById(R.id.productLinearLayout);
            addToCartBtn = itemView.findViewById(R.id.addToCartBtn);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
            noOfRatingTextView = itemView.findViewById(R.id.noOfRatingTextView);
        }
    }

    public interface MyCallback {
        void onCallback(long stock); // Sử dụng long thay vì int
    }
}