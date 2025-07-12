package com.example.newEcom.activities;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.OrderItemModel;
import com.example.newEcom.utils.EmailSender;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class CheckoutActivity extends AppCompatActivity {
    private TextView subtotalTextView, deliveryTextView, totalTextView, stockErrorTextView;
    private Button checkoutBtn;
    private ImageView backBtn;
    private SweetAlertDialog dialog;
    private double subTotal;
    private EditText nameEditText, emailEditText, phoneEditText, addressEditText, commentEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Initialize views
        subtotalTextView = findViewById(R.id.subtotalTextView);
        deliveryTextView = findViewById(R.id.deliveryTextView);
        totalTextView = findViewById(R.id.totalTextView);
        stockErrorTextView = findViewById(R.id.stockErrorTextView);
        checkoutBtn = findViewById(R.id.checkoutBtn);
        backBtn = findViewById(R.id.backBtn);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        commentEditText = findViewById(R.id.commentEditText);

        // Calculate total price
        subTotal = getIntent().getDoubleExtra("price", 10000.00);
        subtotalTextView.setText(String.format("%.2f USD", subTotal));
        if (subTotal >= 5000) {
            deliveryTextView.setText("0.00 USD");
            totalTextView.setText(String.format("%.2f USD", subTotal));
        } else {
            deliveryTextView.setText("500.00 USD");
            totalTextView.setText(String.format("%.2f USD", subTotal + 500));
        }

        // Handle events
        checkoutBtn.setOnClickListener(v -> {
            if (validate()) {
                processOrder();
            }
        });
        backBtn.setOnClickListener(v -> onBackPressed());

        // Initialize dialog
        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);
    }

    private void processOrder() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String comment = commentEditText.getText().toString().trim();
        Log.d("CheckoutActivity", "Values - Name: " + name + ", Email: " + email + ", Comment: " + comment);

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, "Please log in to place an order", Toast.LENGTH_SHORT).show();
            return;
        }

        dialog.show();

        // Fetch previous order details
        FirebaseUtil.getDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot detailsDoc = task.getResult();
                int lastOrderId = detailsDoc.getLong("lastOrderId") != null ? detailsDoc.getLong("lastOrderId").intValue() : 0;
                int itemCount = detailsDoc.getLong("countOfOrderedItems") != null ? detailsDoc.getLong("countOfOrderedItems").intValue() : 0;
                double totalPrice = detailsDoc.getDouble("priceOfOrders") != null ? detailsDoc.getDouble("priceOfOrders") : 0.0;

                // Fetch cart items
                CollectionReference cartItems = FirebaseUtil.getCartItems();
                if (cartItems == null) {
                    dialog.dismiss();
                    Toast.makeText(this, "Error loading cart", Toast.LENGTH_SHORT).show();
                    return;
                }

                cartItems.get().addOnCompleteListener(cartTask -> {
                    if (cartTask.isSuccessful() && !cartTask.getResult().isEmpty()) {
                        List<OrderItemModel> orderItems = new ArrayList<>();
                        List<String> lessStockItems = new ArrayList<>();
                        List<Task<QuerySnapshot>> stockTasks = new ArrayList<>();
                        Map<Integer, String> cartItemDocIds = new HashMap<>(); // Store document IDs of cart items

                        for (QueryDocumentSnapshot doc : cartTask.getResult()) {
                            int productId = doc.getLong("productId").intValue();
                            String nameProduct = doc.getString("name");
                            String image = doc.getString("image");
                            double price = doc.getDouble("price");
                            int quantity = doc.getLong("quantity").intValue();
                            cartItemDocIds.put(productId, doc.getId()); // Store document ID

                            // Check stock
                            Task<QuerySnapshot> stockTask = FirebaseUtil.getProducts()
                                    .whereEqualTo("productId", productId)
                                    .get();
                            stockTasks.add(stockTask);
                        }

                        // Wait for all stock queries to complete
                        Tasks.whenAllComplete(stockTasks).addOnCompleteListener(tasks -> {
                            for (Task<QuerySnapshot> stockTask : stockTasks) {
                                if (stockTask.isSuccessful() && !stockTask.getResult().isEmpty()) {
                                    DocumentSnapshot productDoc = stockTask.getResult().getDocuments().get(0);
                                    int productId = productDoc.getLong("productId").intValue();
                                    int stock = productDoc.getLong("stock").intValue();
                                    QueryDocumentSnapshot cartDoc = null;
                                    for (QueryDocumentSnapshot doc : cartTask.getResult()) {
                                        if (doc.getLong("productId").intValue() == productId) {
                                            cartDoc = doc;
                                            break;
                                        }
                                    }
                                    if (cartDoc != null) {
                                        String nameProduct = cartDoc.getString("name");
                                        String image = cartDoc.getString("image");
                                        double price = cartDoc.getDouble("price");
                                        int quantity = cartDoc.getLong("quantity").intValue();

                                        if (stock < quantity) {
                                            lessStockItems.add(nameProduct + " (Stock: " + stock + ")");
                                        } else {
                                            OrderItemModel item = new OrderItemModel(
                                                    lastOrderId + 1,
                                                    productId,
                                                    nameProduct,
                                                    image,
                                                    price,
                                                    quantity,
                                                    Timestamp.now(),
                                                    name,
                                                    email,
                                                    phone,
                                                    address,
                                                    comment,
                                                    "Pending",
                                                    userId,
                                                    Timestamp.now(),
                                                    userId,
                                                    null
                                            );
                                            orderItems.add(item);
                                        }
                                    }
                                } else {
                                    Log.e("CheckoutActivity", "Error checking stock: ", stockTask.getException());
                                }
                            }

                            if (!lessStockItems.isEmpty()) {
                                String errorText = "*The following products have insufficient stock:\n" + String.join("\n", lessStockItems);
                                stockErrorTextView.setText(errorText);
                                stockErrorTextView.setVisibility(View.VISIBLE);
                                dialog.dismiss();
                                Toast.makeText(this, "Insufficient stock", Toast.LENGTH_SHORT).show();
                            } else if (orderItems.isEmpty()) {
                                dialog.dismiss();
                                Toast.makeText(this, "No valid products to process", Toast.LENGTH_SHORT).show();
                            } else {
                                saveOrder(orderItems, lastOrderId, itemCount, totalPrice + subTotal, userId, cartItemDocIds);
                            }
                        });
                    } else {
                        Log.e("CheckoutActivity", "Error loading cart: ", cartTask.getException());
                        dialog.dismiss();
                        Toast.makeText(this, "Error loading cart", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Log.e("CheckoutActivity", "Error loading order details: ", task.getException());
                dialog.dismiss();
                Toast.makeText(this, "Error loading order details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveOrder(List<OrderItemModel> orderItems, int lastOrderId, int itemCount, double totalPrice, String userId, Map<Integer, String> cartItemDocIds) {
        WriteBatch batch = FirebaseUtil.getFirestore().batch();

        // Update dashboard
        Map<String, Object> dashboardUpdate = new HashMap<>();
        dashboardUpdate.put("lastOrderId", lastOrderId + 1);
        dashboardUpdate.put("countOfOrderedItems", itemCount + orderItems.size());
        dashboardUpdate.put("priceOfOrders", totalPrice);
        batch.update(FirebaseUtil.getDetails(), dashboardUpdate);

        // Create orders/{userId} document if not exists
        DocumentReference userOrderRef = FirebaseUtil.getFirestore().collection("orders").document(userId);
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("createdAt", Timestamp.now());
        orderData.put("userId", userId);
        batch.set(userOrderRef, orderData);

        // Save order items
        CollectionReference orderItemsRef = userOrderRef.collection("items");
        AtomicInteger saveCount = new AtomicInteger(0);
        for (OrderItemModel item : orderItems) {
            DocumentReference orderItemRef = orderItemsRef.document();
            batch.set(orderItemRef, item);
        }

        // Commit batch
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                updateStockAndCart(orderItems, userId, cartItemDocIds);
            } else {
                Log.e("CheckoutActivity", "Error saving order: ", task.getException());
                dialog.dismiss();
                Toast.makeText(this, "Error placing order", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStockAndCart(List<OrderItemModel> orderItems, String userId, Map<Integer, String> cartItemDocIds) {
        WriteBatch batch = FirebaseUtil.getFirestore().batch();

        AtomicInteger updateCount = new AtomicInteger(0);
        for (OrderItemModel item : orderItems) {
            FirebaseUtil.getProducts().whereEqualTo("productId", item.getProductId())
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot productDoc = task.getResult().getDocuments().get(0);
                            String docId = productDoc.getId();
                            int currentStock = productDoc.getLong("stock").intValue();
                            batch.update(FirebaseUtil.getProducts().document(docId), "stock", currentStock - item.getQuantity());

                            String cartDocId = cartItemDocIds.get(item.getProductId());
                            if (cartDocId != null) {
                                batch.delete(FirebaseUtil.getCartItems().document(cartDocId));
                            }

                            if (updateCount.incrementAndGet() == orderItems.size()) {
                                batch.commit().addOnSuccessListener(aVoid -> {
                                    sendConfirmationEmail(orderItems, userId);
                                }).addOnFailureListener(e -> {
                                    Log.e("CheckoutActivity", "Error updating stock and cart: ", e);
                                    dialog.dismiss();
                                    Toast.makeText(this, "Error placing order", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            Log.e("CheckoutActivity", "Error loading product for stock update: ", task.getException());
                            dialog.dismiss();
                            Toast.makeText(this, "Error placing order", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void sendConfirmationEmail(List<OrderItemModel> orderItems, String userId) {
        String name = orderItems.get(0).getFullName();
        String email = orderItems.get(0).getEmail();
        String subject = "Your order has been successfully placed at ShopEase!";
        StringBuilder messageBody = new StringBuilder("Dear " + name + ",\n\n" +
                "Thank you for shopping with ShopEase. We are pleased to inform you that your order has been successfully placed.\n\n" +
                "Order details:\n" +
                "-----------------------------------------------------------------------------------\n" +
                String.format("%-50s %-10s %-10s\n", "Product Name", "Quantity", "Price") +
                "-----------------------------------------------------------------------------------\n");

        double total = 0;
        for (OrderItemModel item : orderItems) {
            messageBody.append(String.format("%-50s %-10d $%.2f\n", item.getName(), item.getQuantity(), item.getPrice()));
            total += item.getPrice() * item.getQuantity();
        }
        messageBody.append("-----------------------------------------------------------------------------\n" +
                String.format("%-73s $%.2f\n", "Total:", total) +
                "-----------------------------------------------------------------------------\n\n" +
                "Thank you for choosing our services. If you have any questions or concerns, please contact our customer support team.\n\n" +
                "Best regards,\n" +
                "The ShopEase Team");

        EmailSender emailSender = new EmailSender(subject, messageBody.toString(), email);
        emailSender.sendEmail();

        dialog.dismiss();
        new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Order Placed Successfully!")
                .setContentText("You will soon receive a confirmation email with order details.")
                .setConfirmClickListener(sweetAlertDialog -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("orderPlaced", true);
                    startActivity(intent);
                    finish();
                }).show();
    }

    private boolean validate() {
        boolean isValid = true;
        if (nameEditText.getText().toString().trim().isEmpty()) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (emailEditText.getText().toString().trim().isEmpty()) {
            emailEditText.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailEditText.getText().toString().trim()).matches()) {
            emailEditText.setError("Invalid email");
            isValid = false;
        }
        if (phoneEditText.getText().toString().trim().isEmpty()) {
            phoneEditText.setError("Phone number is required");
            isValid = false;
        } else if (phoneEditText.getText().toString().trim().length() != 10) {
            phoneEditText.setError("Phone number must be 10 digits");
            isValid = false;
        }
        if (addressEditText.getText().toString().trim().isEmpty()) {
            addressEditText.setError("Address is required");
            isValid = false;
        }
        return isValid;
    }
}