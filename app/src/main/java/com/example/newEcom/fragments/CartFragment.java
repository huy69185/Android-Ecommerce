package com.example.newEcom.fragments;

import android.app.Activity;
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
import androidx.annotation.Nullable;
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
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.model.ProductModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

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
                // Tạo DocumentReference cho "orders/{userId}"
                DocumentReference userOrderRef = FirebaseFirestore.getInstance().collection("orders").document(userId);
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("createdAt", FieldValue.serverTimestamp()); // Thêm trường mặc định
                orderData.put("userId", userId); // Thêm thông tin userId
                userOrderRef.set(orderData, SetOptions.merge()) // Gộp để không ghi đè sub-collection
                        .addOnSuccessListener(aVoid -> {
                            CollectionReference orderItemsRef = FirebaseFirestore.getInstance().collection("orders").document(userId).collection("items");
                            for (CartItemModel cartItem : cartAdapter.getSnapshots()) {
                                // Lấy thông tin sản phẩm từ Firestore để cập nhật stock
                                FirebaseUtil.getProducts().whereEqualTo("productId", cartItem.getProductId())
                                        .get().addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    ProductModel product = document.toObject(ProductModel.class);
                                                    int currentStock = product.getStock();
                                                    int orderedQuantity = cartItem.getQuantity();
                                                    if (currentStock >= orderedQuantity) {
                                                        // Cập nhật stock mới
                                                        int newStock = currentStock - orderedQuantity;
                                                        FirebaseUtil.getProducts().document(document.getId())
                                                                .update("stock", newStock)
                                                                .addOnSuccessListener(aVoidInner -> {
                                                                    // Tạo OrderItemModel
                                                                    OrderItemModel orderItem = new OrderItemModel(
                                                                            (int) System.currentTimeMillis(), // orderId tạm thời
                                                                            cartItem.getProductId(),
                                                                            cartItem.getName(),
                                                                            cartItem.getImage(),
                                                                            cartItem.getPrice(),
                                                                            cartItem.getQuantity(),
                                                                            Timestamp.now(),
                                                                            "Customer Name", // Nên lấy từ profile
                                                                            "customer@email.com", // Nên lấy từ profile
                                                                            "123456789", // Nên lấy từ profile
                                                                            "Customer Address", // Nên lấy từ profile
                                                                            "",
                                                                            "Pending" // Trạng thái mặc định là Pending
                                                                    );
                                                                    orderItemsRef.add(orderItem)
                                                                            .addOnSuccessListener(documentReference -> {
                                                                                Log.d(TAG, "Order saved with ID: " + documentReference.getId() + " under user: " + userId);
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                Log.e(TAG, "Failed to save order item: ", e);
                                                                            });
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.e(TAG, "Failed to update stock for " + cartItem.getName() + ": ", e);
                                                                    Toast.makeText(getActivity(), "Failed to update stock", Toast.LENGTH_SHORT).show();
                                                                });
                                                    } else {
                                                        Log.w(TAG, "Not enough stock for " + cartItem.getName());
                                                        Toast.makeText(getActivity(), "Not enough stock for " + cartItem.getName(), Toast.LENGTH_SHORT).show();
                                                        return; // Thoát nếu không đủ hàng
                                                    }
                                                }
                                            } else {
                                                Log.e(TAG, "Failed to fetch product data: ", task.getException());
                                            }
                                        });
                            }
                            // Xóa giỏ hàng sau khi đặt hàng thành công
                            FirebaseUtil.getCartItems().get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot doc : task.getResult()) {
                                        FirebaseUtil.getCartItems().document(doc.getId()).delete()
                                                .addOnSuccessListener(aVoidInner -> {
                                                    Log.d(TAG, "Cart item deleted with ID: " + doc.getId());
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "Failed to delete cart item: ", e);
                                                });
                                    }
                                    Intent intent = new Intent(getActivity(), CheckoutActivity.class);
                                    intent.putExtra("price", totalPrice);
                                    startActivity(intent);
                                } else {
                                    Log.e(TAG, "Failed to fetch cart items for deletion: ", task.getException());
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create order document: ", e);
                            Toast.makeText(getActivity(), "Failed to place order", Toast.LENGTH_SHORT).show();
                        });
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