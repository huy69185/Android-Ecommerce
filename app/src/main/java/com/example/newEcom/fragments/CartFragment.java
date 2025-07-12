package com.example.newEcom.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.CheckoutActivity;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.adapters.CartAdapter;
import com.example.newEcom.model.CartItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

public class CartFragment extends Fragment {
    private static final String TAG = "CartFragment";
    private TextView cartPriceTextView;
    private RecyclerView cartRecyclerView;
    private Button continueBtn;
    private ImageView backBtn, emptyCartImageView;
    private CartAdapter cartAdapter;
    private double totalPrice = 0;
    private ShimmerFrameLayout shimmerFrameLayout;
    private LinearLayout mainLinearLayout;

    public CartFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);
        cartPriceTextView = view.findViewById(R.id.cartPriceTextView);
        cartRecyclerView = view.findViewById(R.id.cartRecyclerView);
        continueBtn = view.findViewById(R.id.continueBtn);
        backBtn = view.findViewById(R.id.backBtn);
        emptyCartImageView = view.findViewById(R.id.emptyCartImageView);
        shimmerFrameLayout = view.findViewById(R.id.shimmerLayout);
        mainLinearLayout = view.findViewById(R.id.mainLinearLayout);

        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.hideSearchBar();
        }
        shimmerFrameLayout.startShimmer();
        emptyCartImageView.setVisibility(View.INVISIBLE);

        setupCartRecyclerView();

        continueBtn.setOnClickListener(v -> {
            if (totalPrice == 0) {
                Toast.makeText(getActivity(), R.string.cart_is_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = FirebaseUtil.getCurrentUserId();
            if (userId != null) {
                Intent intent = new Intent(getActivity(), CheckoutActivity.class);
                intent.putExtra("price", totalPrice);
                startActivity(intent);
            } else {
                Toast.makeText(getActivity(), R.string.please_login_to_view_cart, Toast.LENGTH_SHORT).show();
            }
        });

        backBtn.setOnClickListener(v -> {
            if (activity != null) {
                activity.onBackPressed();
            }
        });

        return view;
    }

    private void setupCartRecyclerView() {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(getActivity(), R.string.please_login_to_view_cart, Toast.LENGTH_SHORT).show();
            return;
        }

        Query query = FirebaseUtil.getCartItems().orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<CartItemModel> options = new FirestoreRecyclerOptions.Builder<CartItemModel>()
                .setQuery(query, CartItemModel.class)
                .build();

        cartAdapter = new CartAdapter(options, getActivity());
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        cartRecyclerView.setAdapter(cartAdapter);
        cartRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        cartAdapter.startListening();
        Log.d(TAG, "Adapter started with query: " + query.toString() + ", item count: " + cartAdapter.getItemCount());
    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            totalPrice = intent.getDoubleExtra("totalPrice", 0.0);
            cartPriceTextView.setText(String.format("$%.2f", totalPrice));
            Log.d(TAG, "Received total price: " + totalPrice);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver, new IntentFilter("price"));
        if (cartAdapter != null) {
            cartAdapter.startListening();
            Log.d(TAG, "Adapter resumed, item count: " + cartAdapter.getItemCount());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cartAdapter != null) {
            cartAdapter.stopListening();
            Log.d(TAG, "Adapter stopped");
        }
    }
}