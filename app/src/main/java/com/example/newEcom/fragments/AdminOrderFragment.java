package com.example.newEcom.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.adapters.OrderAdminAdapter;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminOrderFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrderAdminAdapter adapter;
    private EditText searchEditText;
    private Button searchButton;
    private String currentFilter = "All";
    private String currentSearchTerm = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_order, container, false);
        recyclerView = view.findViewById(R.id.orderRecyclerView);
        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        setupRecyclerView();
        setupFilterButton(view);
        setupSearchButton();
        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        FirebaseUtil.getAllOrderItems(new FirebaseUtil.OnOrderItemsLoadedListener() {
            @Override
            public void onItemsLoaded(List<Map<String, Object>> orderItemsData) {
                if (orderItemsData == null || orderItemsData.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Không có đơn hàng", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                for (Map<String, Object> data : orderItemsData) {
                    CollectionReference itemsCollection = (CollectionReference) data.get("itemsCollection");
                    tasks.add(itemsCollection.get());
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    List<OrderItemModel> allItems = new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        QuerySnapshot snapshot = (QuerySnapshot) results.get(i);
                        String orderParentId = (String) orderItemsData.get(i).get("orderParentId");
                        Log.d("AdminOrderFragment", "Processing orderParentId: " + orderParentId);
                        for (QueryDocumentSnapshot document : snapshot) {
                            OrderItemModel item = document.toObject(OrderItemModel.class);
                            item.setOrderParentId(orderParentId);
                            item.setItemId(document.getId());
                            Log.d("AdminOrderFragment", "Item added: orderParentId=" + item.getOrderParentId() + ", itemId=" + item.getItemId() + ", orderId=" + item.getOrderId());
                            allItems.add(item);
                        }
                    }

                    // Cập nhật adapter với dữ liệu ban đầu
                    updateAdapter(allItems);

                    if (allItems.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Không có đơn hàng", Toast.LENGTH_SHORT).show();
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    Log.d("AdminOrderFragment", "Adapter listening with " + allItems.size() + " items");
                }).addOnFailureListener(e -> Log.e("AdminOrderFragment", "Error fetching items: ", e));
            }
        });
    }

    private void updateAdapter(List<OrderItemModel> allItems) {
        // Lọc thủ công
        List<OrderItemModel> filteredItems = new ArrayList<>();
        for (OrderItemModel item : allItems) {
            boolean match = true;
            if (!"All".equals(currentFilter) && !currentFilter.equalsIgnoreCase(item.getStatus())) {
                match = false;
            }
            if (!currentSearchTerm.isEmpty()) {
                String searchLower = currentSearchTerm.toLowerCase();
                if (!(item.getName() != null && item.getName().toLowerCase().contains(searchLower)) &&
                        !(item.getFullName() != null && item.getFullName().toLowerCase().contains(searchLower)) &&
                        !(item.getPhoneNumber() != null && item.getPhoneNumber().toLowerCase().contains(searchLower))) {
                    match = false;
                }
            }
            if (match) filteredItems.add(item);
        }

        // Tạo adapter mới với dữ liệu đã lọc
        adapter = new OrderAdminAdapter(null, getContext());
        adapter.setItems(filteredItems); // Cập nhật danh sách cho adapter

        recyclerView.setAdapter(adapter);

        if (filteredItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Không có đơn hàng", Toast.LENGTH_SHORT).show();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupFilterButton(View view) {
        view.findViewById(R.id.filterButton).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getContext(), v);
            popup.getMenuInflater().inflate(R.menu.filter_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(this::onFilterMenuItemClick);
            popup.show();
        });
    }

    private boolean onFilterMenuItemClick(MenuItem item) {
        currentFilter = item.getTitle().toString();
        updateQuery();
        return true;
    }

    private void setupSearchButton() {
        searchButton.setOnClickListener(v -> {
            currentSearchTerm = searchEditText.getText().toString().trim().toLowerCase();
            Log.d("AdminOrderFragment", "Search triggered with term: " + currentSearchTerm);
            updateQuery();
        });
    }

    private void updateQuery() {
        if (adapter != null) {
            recyclerView.setAdapter(null); // Xóa adapter cũ
        }

        FirebaseUtil.getAllOrderItems(new FirebaseUtil.OnOrderItemsLoadedListener() {
            @Override
            public void onItemsLoaded(List<Map<String, Object>> orderItemsData) {
                if (orderItemsData == null || orderItemsData.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Không có đơn hàng", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                for (Map<String, Object> data : orderItemsData) {
                    CollectionReference itemsCollection = (CollectionReference) data.get("itemsCollection");
                    tasks.add(itemsCollection.get());
                }

                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    List<OrderItemModel> allItems = new ArrayList<>();
                    for (int i = 0; i < results.size(); i++) {
                        QuerySnapshot snapshot = (QuerySnapshot) results.get(i);
                        String orderParentId = (String) orderItemsData.get(i).get("orderParentId");
                        Log.d("AdminOrderFragment", "Processing orderParentId: " + orderParentId);
                        for (QueryDocumentSnapshot document : snapshot) {
                            OrderItemModel item = document.toObject(OrderItemModel.class);
                            item.setOrderParentId(orderParentId);
                            item.setItemId(document.getId());
                            Log.d("AdminOrderFragment", "Item added: orderParentId=" + item.getOrderParentId() + ", itemId=" + item.getItemId() + ", orderId=" + item.getOrderId());
                            allItems.add(item);
                        }
                    }

                    updateAdapter(allItems); // Cập nhật adapter với dữ liệu đã lọc
                }).addOnFailureListener(e -> Log.e("AdminOrderFragment", "Error fetching items: ", e));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Không cần startListening
    }

    @Override
    public void onStop() {
        super.onStop();
        // Không cần stopListening
    }
}