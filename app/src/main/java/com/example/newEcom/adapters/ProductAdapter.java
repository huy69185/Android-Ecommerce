package com.example.newEcom.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.fragments.ProductFragment;
import com.example.newEcom.model.ProductModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Picasso;

public class ProductAdapter extends FirestoreRecyclerAdapter<ProductModel, ProductAdapter.ProductViewHolder> {
    private final Context context;
    private AppCompatActivity activity;

    public ProductAdapter(@NonNull FirestoreRecyclerOptions<ProductModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_adapter, parent, false);
        activity = (AppCompatActivity) view.getContext();
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull ProductModel product) {
        Picasso.get().load(product.getImage()).into(holder.productImage);

        holder.productLabel.setText(product.getName());
        holder.productPrice.setText(String.format("$%.2f", product.getPrice()));
        holder.originalPrice.setText(String.format("$%.2f", product.getOriginalPrice()));
        holder.originalPrice.setPaintFlags(holder.originalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        double originalPrice = product.getOriginalPrice() > 0 ? product.getOriginalPrice() : 1.0;
        double discount = product.getDiscount() > 0 ? product.getDiscount() : 0.0;
        int discountPerc = (int) ((discount / originalPrice) * 100);
        holder.discountPercentage.setText(String.format("%d%% OFF", discountPerc));

        holder.itemView.setOnClickListener(v -> {
            Fragment fragment = ProductFragment.newInstance(product);
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame_layout, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productLabel, productPrice, originalPrice, discountPercentage;
        ImageView productImage;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productListImage);
            productLabel = itemView.findViewById(R.id.productLabel);
            productPrice = itemView.findViewById(R.id.productPrice);
            originalPrice = itemView.findViewById(R.id.originalPrice);
            discountPercentage = itemView.findViewById(R.id.discountPercentage);
        }
    }
}