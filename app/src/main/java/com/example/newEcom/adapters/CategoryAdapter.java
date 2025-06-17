package com.example.newEcom.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.model.CategoryModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.squareup.picasso.Picasso;

public class CategoryAdapter extends FirestoreRecyclerAdapter<CategoryModel, CategoryAdapter.CategoryViewHolder> {
    private static final String TAG = "CategoryAdapter";
    private final Context context;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName);
    }

    public CategoryAdapter(@NonNull FirestoreRecyclerOptions<CategoryModel> options, Context context, OnCategoryClickListener listener) {
        super(options);
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_adapter, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull CategoryViewHolder holder, int position, @NonNull CategoryModel category) {
        holder.categoryLabel.setText(category.getName());
        if (category.getIcon() != null && !category.getIcon().isEmpty()) {
            Picasso.get().load(category.getIcon()).into(holder.categoryImage);
        } else {
            holder.categoryImage.setImageResource(R.drawable.temp); // Hình ảnh mặc định
        }

        String color = category.getColor();
        try {
            if (color != null && !color.isEmpty() && color.startsWith("#")) {
                holder.categoryImage.setBackgroundColor(Color.parseColor(color));
            } else {
                holder.categoryImage.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid color: " + color, e);
            holder.categoryImage.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category.getName());
            }
        });
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        notifyDataSetChanged(); // Thông báo thay đổi dữ liệu để đồng bộ RecyclerView
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryLabel;
        ImageView categoryImage;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.categoryImage);
            categoryLabel = itemView.findViewById(R.id.categoryLabel);
        }
    }
}