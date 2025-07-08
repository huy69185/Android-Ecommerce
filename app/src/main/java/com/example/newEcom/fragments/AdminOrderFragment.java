package com.example.newEcom.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.adapters.OrderAdminAdapter;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminOrderFragment extends Fragment {
    private static final String TAG = "AdminOrderFragment";
    private RecyclerView orderRecyclerView;
    private OrderAdminAdapter orderAdapter;
    private androidx.appcompat.widget.SearchView searchView;
    private String currentFilter = "All";

    public AdminOrderFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_order, container, false);

        orderRecyclerView = view.findViewById(R.id.orderRecyclerView);
        searchView = view.findViewById(R.id.searchView);

        androidx.appcompat.widget.AppCompatSpinner filterSpinner = view.findViewById(R.id.filterSpinner);
        if (filterSpinner != null) {
            List<String> filterOptions = new ArrayList<>();
            filterOptions.add("All");
            filterOptions.add("Pending");
            filterOptions.add("Delivery");
            filterOptions.add("Confirm");
            filterOptions.add("Cancel");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, filterOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            filterSpinner.setAdapter(adapter);
            filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    currentFilter = filterOptions.get(position);
                    setupOrderRecyclerView();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    currentFilter = "All";
                    setupOrderRecyclerView();
                }
            });
        }

        setupOrderRecyclerView();

        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filter(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filter(newText);
                    return false;
                }
            });
        }

        return view;
    }

    private void setupOrderRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView with filter: " + currentFilter);
        FirebaseUtil.getAllOrderItems(itemCollections -> {
            if (itemCollections.isEmpty()) {
                Log.w(TAG, "No order items collections found");
                Toast.makeText(getActivity(), "No orders available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy query từ subcollection
            Query baseQuery = itemCollections.get(0).orderBy("timestamp", Query.Direction.DESCENDING);
            if (!"All".equals(currentFilter)) {
                baseQuery = baseQuery.whereEqualTo("status", currentFilter);
            }
            FirestoreRecyclerOptions<OrderItemModel> options = new FirestoreRecyclerOptions.Builder<OrderItemModel>()
                    .setQuery(baseQuery, OrderItemModel.class)
                    .build();

            try {
                if (orderAdapter == null) {
                    orderAdapter = new OrderAdminAdapter(options, getActivity());
                    orderRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                    orderRecyclerView.setAdapter(orderAdapter);
                    Log.d(TAG, "New adapter created");
                } else {
                    orderAdapter.updateOptions(options);
                    Log.d(TAG, "Adapter options updated");
                }
                if (orderAdapter != null) {
                    orderAdapter.startListening();
                    Log.d(TAG, "Adapter started listening with " + (options.getSnapshots() != null ? options.getSnapshots().size() : 0) + " items");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up RecyclerView: ", e);
                Toast.makeText(getActivity(), "Failed to load orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filter(String text) {
        Log.d(TAG, "Filtering with text: " + text);
        FirebaseUtil.getAllOrderItems(itemCollections -> {
            if (itemCollections.isEmpty()) {
                Log.w(TAG, "No order items collections found");
                return;
            }

            Query baseQuery = itemCollections.get(0)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .whereGreaterThanOrEqualTo("name", text)
                    .whereLessThanOrEqualTo("name", text + "\uf8ff");
            if (!"All".equals(currentFilter)) {
                baseQuery = baseQuery.whereEqualTo("status", currentFilter);
            }
            FirestoreRecyclerOptions<OrderItemModel> options = new FirestoreRecyclerOptions.Builder<OrderItemModel>()
                    .setQuery(baseQuery, OrderItemModel.class)
                    .build();
            if (orderAdapter != null) {
                try {
                    orderAdapter.updateOptions(options);
                    orderAdapter.startListening();
                    Log.d(TAG, "Filter applied, adapter updated with " + (options.getSnapshots() != null ? options.getSnapshots().size() : 0) + " items");
                } catch (Exception e) {
                    Log.e(TAG, "Error filtering orders: ", e);
                    Toast.makeText(getActivity(), "Error filtering orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (orderAdapter != null) {
            orderAdapter.startListening();
            Log.d(TAG, "Fragment onStart, adapter listening");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (orderAdapter != null) {
            orderAdapter.stopListening();
            Log.d(TAG, "Fragment onStop, adapter stopped");
        }
    }
}