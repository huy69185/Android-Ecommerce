package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class CheckoutActivity extends AppCompatActivity {
    TextView subtotalTextView, deliveryTextView, totalTextView, stockErrorTextView;
    Button checkoutBtn;
    ImageView backBtn;

    SweetAlertDialog dialog;

    double subTotal;
    int count = 0;
    volatile boolean adequateStock = true, done = false;

    EditText nameEditText, emailEditText, phoneEditText, addressEditText, commentEditText;
    String name, email, phone, address, comment;

    final int[] prevOrderId = new int[1];
    final int[] countOfOrderedItems = new int[1];
    final double[] priceOfOrders = new double[1];

    final List<String>[] productDocId = new ArrayList[1];
    final List<Integer>[] oldStock = new ArrayList[1];
    final List<Integer>[] quan = new ArrayList[1];
    final List<String>[] lessStock = new ArrayList[1];

    final List<String>[] cartDocument = new ArrayList[1];
    final List<String>[] productName = new ArrayList[1];
    final List<Double>[] productPrice = new ArrayList[1];
    final List<Integer>[] productQuantity = new ArrayList[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
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

        subTotal = getIntent().getDoubleExtra("price", 10000.00);
        subtotalTextView.setText(String.format("₹ %.2f", subTotal));
        if (subTotal >= 5000) {
            deliveryTextView.setText("₹ 0.00");
            totalTextView.setText(String.format("₹ %.2f", subTotal));
        } else {
            deliveryTextView.setText("₹ 500.00");
            totalTextView.setText(String.format("₹ %.2f", (subTotal + 500)));
        }

        checkoutBtn.setOnClickListener(v -> {
            processOrder(new FirestoreCallback() {
                @Override
                public void onCallback(QueryDocumentSnapshot document, int quantity) {
                    FirebaseUtil.getProducts().whereEqualTo("productId", document.get("productId"))
                            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        String docId = task.getResult().getDocuments().get(0).getId();
                                        int stock = task.getResult().getDocuments().get(0).getLong("stock").intValue();
                                        productDocId[0].add(docId);
                                        oldStock[0].add(stock);
                                        quan[0].add(quantity);

                                        if (stock < quantity) {
                                            adequateStock = false;
                                            lessStock[0].add(document.getString("name"));
                                        }

                                        done = true;
                                        Log.i("done", "check");
                                    } else {
                                        Toast.makeText(CheckoutActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }

                @Override
                public void onCallback(boolean adequateStock) {
                    if (!adequateStock) {
                        String errorText = "*The following product(s) have less stock left:";
                        for (int i = 0; i < lessStock[0].size(); i++) {
                            errorText += "\n\t\t\t• " + lessStock[0].get(i) + " has only " + oldStock[0].get(i) + " stock left";
                        }
                        stockErrorTextView.setText(errorText);
                        stockErrorTextView.setVisibility(View.VISIBLE);
                        Toast.makeText(CheckoutActivity.this, "One of the products has got less stock left :(", Toast.LENGTH_SHORT).show();
                    } else {
                        changeToFirebase();
                    }
                }
            });
        });

        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });

        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Loading...");
        dialog.setCancelable(false);
    }

    private void changeToFirebase() {
        Map<String, Object> map = new HashMap<>();
        map.put("lastOrderId", prevOrderId[0] + 1);
        map.put("countOfOrderedItems", countOfOrderedItems[0] + count);
        map.put("priceOfOrders", priceOfOrders[0] + subTotal);

        FirebaseUtil.getDetails().update(map);

        for (int i = 0; i < productDocId[0].size(); i++) {
            FirebaseUtil.getProducts().document(productDocId[0].get(i)).update("stock", oldStock[0].get(i) - quan[0].get(i));
        }

        for (String docId : cartDocument[0]) {
            FirebaseUtil.getCartItems().document(docId)
                    .delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                        }
                    });
        }

        String subject = "Your Order is successfully placed with ShopEase!";
        String messageBody = "Dear " + name + ",\n\n" +
                "Thank you for placing your order with ShopEase. We are excited to inform you that your order has been successfully placed.\n\n" +
                "Order Details:\n" +
                "-----------------------------------------------------------------------------------\n" +
                String.format("%-50s %-10s %-10s\n", "Product Name", "Quantity", "Price") +
                "-----------------------------------------------------------------------------------\n";
        for (int i = 0; i < productName[0].size(); i++) {
            messageBody += String.format("%-50s %-10d ₹%.2f\n", productName[0].get(i), productQuantity[0].get(i), productPrice[0].get(i));
        }
        messageBody += "-----------------------------------------------------------------------------\n" +
                String.format("%-73s ₹%.2f\n", "Total:", subTotal) +
                "-----------------------------------------------------------------------------\n\n" +
                "Thank you for choosing our service. If you have any questions or concerns, feel free to contact our customer support.\n\n" +
                "Best Regards,\n" +
                "ShopEase Team";
        EmailSender emailSender = new EmailSender(subject, messageBody, email);
        emailSender.sendEmail();

        new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Order placed Successfully!")
                .setContentText("You will shortly receive an email confirming the order details.")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                        intent.putExtra("orderPlaced", true);
                        startActivity(intent);
                        finish();
                    }
                }).show();
    }

    private void processOrder(FirestoreCallback callback) {
        if (!validate())
            return;

        name = nameEditText.getText().toString();
        email = emailEditText.getText().toString();
        phone = phoneEditText.getText().toString();
        address = addressEditText.getText().toString();
        comment = commentEditText.getText().toString();

        productDocId[0] = new ArrayList<>();
        oldStock[0] = new ArrayList<>();
        quan[0] = new ArrayList<>();
        lessStock[0] = new ArrayList<>();

        cartDocument[0] = new ArrayList<>();
        productName[0] = new ArrayList<>();
        productPrice[0] = new ArrayList<>();
        productQuantity[0] = new ArrayList<>();

        FirebaseUtil.getDetails().get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        prevOrderId[0] = task.getResult().getLong("lastOrderId").intValue();
                        countOfOrderedItems[0] = task.getResult().getLong("countOfOrderedItems").intValue();
                        priceOfOrders[0] = task.getResult().getDouble("priceOfOrders");
                    }
                });

        FirebaseUtil.getCartItems()
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                count++;
                                cartDocument[0].add(document.getId());
                                productName[0].add(document.getString("name"));
                                productPrice[0].add(document.getDouble("price"));
                                productQuantity[0].add(document.getLong("quantity").intValue());

                                OrderItemModel item = new OrderItemModel(prevOrderId[0] + 1, document.getLong("productId").intValue(),
                                        document.getString("name"), document.getString("image"),
                                        document.getDouble("price"), document.getLong("quantity").intValue(),
                                        Timestamp.now(), name, email, phone, address, comment);

                                FirebaseFirestore.getInstance().collection("orders")
                                        .document(FirebaseAuth.getInstance().getUid()).collection("items").add(item);
                                int quantity = document.getLong("quantity").intValue();

                                callback.onCallback(document, quantity);
                            }

                            dialog.show();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                    callback.onCallback(adequateStock);
                                }
                            }, 2000);
                        } else {
                            new SweetAlertDialog(CheckoutActivity.this, SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText("Order Failed!")
                                    .setContentText("Something went wrong, please try again.")
                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sweetAlertDialog) {
                                            Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    }).show();
                        }
                    }
                });
    }

    public interface FirestoreCallback {
        void onCallback(QueryDocumentSnapshot document, int quantity);
        void onCallback(boolean adequateStock);
    }

    private boolean validate() {
        boolean isValid = true;
        if (nameEditText.getText().toString().trim().length() == 0) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (emailEditText.getText().toString().trim().length() == 0) {
            emailEditText.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailEditText.getText().toString().trim()).matches()) {
            emailEditText.setError("Email is not valid");
            isValid = false;
        }
        if (phoneEditText.getText().toString().trim().length() == 0) {
            phoneEditText.setError("Phone Number is required");
            isValid = false;
        } else if (phoneEditText.getText().toString().trim().length() != 10) {
            phoneEditText.setError("Phone number is not valid");
            isValid = false;
        }
        if (addressEditText.getText().toString().trim().length() == 0) {
            addressEditText.setError("Address is required");
            isValid = false;
        }
        return isValid;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}