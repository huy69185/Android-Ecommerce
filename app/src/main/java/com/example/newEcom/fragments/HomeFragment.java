package com.example.newEcom.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.adapters.CategoryAdapter;
import com.example.newEcom.adapters.ProductAdapter;
import com.example.newEcom.fragments.CategoryFragment;
import com.example.newEcom.model.CategoryModel;
import com.example.newEcom.model.ProductModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mancj.materialsearchbar.MaterialSearchBar;

import org.imaginativeworld.whynotimagecarousel.ImageCarousel;
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem;

public class HomeFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {
    private static final String TAG = "HomeFragment";
    private RecyclerView categoryRecyclerView, productRecyclerView;
    private MaterialSearchBar searchBar;
    private ImageCarousel carousel;
    private ShimmerFrameLayout shimmerFrameLayout;
    private LinearLayout mainLinearLayout;

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initializeViews(view);
        setupSearchBar();
        startShimmer();
        initCarousel();
        initCategories();
        initProducts();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void initializeViews(View view) {
        searchBar = view.findViewById(R.id.searchBar);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        carousel = view.findViewById(R.id.carousel);
        shimmerFrameLayout = view.findViewById(R.id.shimmerLayout);
        mainLinearLayout = view.findViewById(R.id.mainLinearLayout);
    }

    private void setupSearchBar() {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.showSearchBar();
        } else {
            Log.e(TAG, "Activity is null, cannot show search bar");
        }
    }

    private void startShimmer() {
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.startShimmer();
        }
    }

    private void initCarousel() {
        if (carousel == null) {
            Log.e(TAG, "Carousel is null");
            return;
        }
        FirebaseUtil.getBanner().orderBy("bannerId").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String bannerImage = document.getString("bannerImage");
                    if (bannerImage != null) {
                        carousel.addData(new CarouselItem(bannerImage));
                    } else {
                        Log.w(TAG, "Banner image is null for document: " + document.getId());
                    }
                }
                stopShimmerAndShowContent();
            } else {
                Log.e(TAG, "Error fetching banners", task.getException());
                stopShimmerAndShowContent();
                Toast.makeText(getContext(), "No banners available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initCategories() {
        if (categoryRecyclerView == null || getContext() == null) {
            Log.e(TAG, "categoryRecyclerView or context is null");
            return;
        }
        Query query = FirebaseUtil.getCategories();
        FirestoreRecyclerOptions<CategoryModel> options = new FirestoreRecyclerOptions.Builder<CategoryModel>()
                .setQuery(query, CategoryModel.class)
                .build();

        categoryAdapter = new CategoryAdapter(options, getContext(), this);
        categoryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        categoryRecyclerView.setAdapter(categoryAdapter);
    }

    private void initProducts() {
        if (productRecyclerView == null || getContext() == null) {
            Log.e(TAG, "productRecyclerView or context is null");
            return;
        }
        Query query = FirebaseUtil.getProducts();
        FirestoreRecyclerOptions<ProductModel> options = new FirestoreRecyclerOptions.Builder<ProductModel>()
                .setQuery(query, ProductModel.class)
                .build();

        productAdapter = new ProductAdapter(options, getContext());
        productRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        productRecyclerView.setAdapter(productAdapter);
    }

    private void stopShimmerAndShowContent() {
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
            shimmerFrameLayout.setVisibility(View.GONE);
        }
        if (mainLinearLayout != null) {
            mainLinearLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (categoryAdapter != null) {
            categoryAdapter.startListening();
        }
        if (productAdapter != null) {
            productAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (categoryAdapter != null) {
            categoryAdapter.stopListening();
        }
        if (productAdapter != null) {
            productAdapter.stopListening();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (categoryAdapter != null) {
            categoryAdapter.stopListening();
        }
        if (productAdapter != null) {
            productAdapter.stopListening();
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
        }
    }

    @Override
    public void onCategoryClick(String categoryName) {
        if (getActivity() != null) {
            CategoryFragment fragment = new CategoryFragment();
            Bundle args = new Bundle();
            args.putString("categoryName", categoryName);
            fragment.setArguments(args);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame_layout, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}