package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.ProductModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.Arrays;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class AddProductActivity extends AppCompatActivity {
    TextInputEditText idEditText, nameEditText, descEditText, specEditText, stockEditText, priceEditText, discountEditText;
    Button imageBtn, addProductBtn;
    ImageView backBtn, productImageView;
    TextView removeImageBtn;

    AutoCompleteTextView categoryDropDown;
    ArrayAdapter<String> arrayAdapter;
    String[] categories;
    String category, productImage, shareLink;
    String productName;
    int productId;
    Context context = this;
    boolean imageUploaded = false;

    SweetAlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        idEditText = findViewById(R.id.idEditText);
        nameEditText = findViewById(R.id.nameEditText);
        categoryDropDown = findViewById(R.id.categoryDropDown);
        descEditText = findViewById(R.id.descriptionEditText);
        specEditText = findViewById(R.id.specificationEditText);
        stockEditText = findViewById(R.id.stockEditText);
        priceEditText = findViewById(R.id.priceEditText);
        discountEditText = findViewById(R.id.discountEditText);
        productImageView = findViewById(R.id.productImageView);

        imageBtn = findViewById(R.id.imageBtn);
        addProductBtn = findViewById(R.id.addProductBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        // Lấy ID lớn nhất từ Firestore dựa trên productId
        FirebaseUtil.getProducts()
                .orderBy("productId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                Integer maxId = document.getLong("productId") != null ? document.getLong("productId").intValue() : 0;
                                productId = maxId + 1;
                            } else {
                                productId = 1; // Nếu không có dữ liệu, bắt đầu từ 1
                            }
                            idEditText.setText(String.valueOf(productId));
                            idEditText.setEnabled(false); // Không cho sửa ID
                        } else {
                            Toast.makeText(context, "Failed to fetch max ID: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            productId = 1;
                            idEditText.setText(String.valueOf(productId));
                            idEditText.setEnabled(false);
                        }
                    }
                });

        imageBtn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        addProductBtn.setOnClickListener(v -> {
            generateDynamicLink();
        });

        backBtn.setOnClickListener(v -> {
            onBackPressed();
        });

        removeImageBtn.setOnClickListener(v -> {
            removeImage();
        });

        dialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        dialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        dialog.setTitleText("Uploading image...");
        dialog.setCancelable(false);
    }

    private void getCategories(MyCallback myCallback) {
        int size[] = new int[1];

        FirebaseUtil.getCategories().orderBy("name")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            size[0] = task.getResult().size();
                        }
                        myCallback.onCallback(size);
                    }
                });
        categories = new String[size[0]];

        FirebaseUtil.getCategories().orderBy("name")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int i = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                categories[i] = document.getString("name");
                                Log.i("Category", categories[i]);
                                i++;
                            }
                            myCallback.onCallback(categories);
                        }
                    }
                });
    }

    private void addToFirebase() {
        productName = nameEditText.getText().toString();
        List<String> sk = Arrays.asList(productName.trim().toLowerCase().split(" "));
        String desc = descEditText.getText().toString();
        String spec = specEditText.getText().toString();
        double price = Double.parseDouble(priceEditText.getText().toString());
        double discount = Double.parseDouble(discountEditText.getText().toString());
        int stock = Integer.parseInt(stockEditText.getText().toString());

        ProductModel model = new ProductModel(productName, sk, productImage, category, desc, spec, price, discount, price - discount, productId, stock, shareLink, 0f, 0);
        // Sử dụng set() với ID cố định là "product" + productId
        FirebaseUtil.getProducts().document("product" + productId)
                .set(model)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddProductActivity.this, "Product has been added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to add product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void generateDynamicLink() {
        if (!validate())
            return;

        productName = nameEditText.getText().toString();
        if (productName.isEmpty()) {
            Toast.makeText(context, "Product name is required for Dynamic Link", Toast.LENGTH_SHORT).show();
            return;
        }

        // Thay bằng domain thực tế từ Firebase Console (ví dụ: https://myecomapp.page.link)
        String domainUriPrefix = "https://mynewshop.page.link"; // Cập nhật domain của bạn
        // Thay bằng deep link thực tế
        String deepLink = "https://mynewshop.page.link/product?id=" + productId; // Cập nhật domain thực tế

        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(deepLink))
                .setDomainUriPrefix(domainUriPrefix)
                .setAndroidParameters(new DynamicLink.AndroidParameters.Builder("com.example.newEcom").build())
                .setSocialMetaTagParameters(new DynamicLink.SocialMetaTagParameters.Builder()
                        .setTitle(productName)
                        .setImageUrl(Uri.parse(productImage != null ? productImage : ""))
                        .build())
                .buildShortDynamicLink(ShortDynamicLink.Suffix.SHORT)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Uri shortLink = task.getResult().getShortLink();
                        shareLink = shortLink.toString();
                        addToFirebase();
                    } else {
                        Log.e("DynamicLinkError", "Failed to create Dynamic Link: " + task.getException().getMessage());
                        Toast.makeText(context, "Failed to create Dynamic Link: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validate() {
        boolean isValid = true;
        if (idEditText.getText().toString().trim().length() == 0) {
            idEditText.setError("Id is required");
            isValid = false;
        }
        if (nameEditText.getText().toString().trim().length() == 0) {
            nameEditText.setError("Name is required");
            isValid = false;
        }
        if (categoryDropDown.getText().toString().trim().length() == 0) {
            categoryDropDown.setError("Category is required");
            isValid = false;
        }
        if (descEditText.getText().toString().trim().length() == 0) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        if (stockEditText.getText().toString().trim().length() == 0) {
            stockEditText.setError("Stock is required");
            isValid = false;
        }
        if (priceEditText.getText().toString().trim().length() == 0) {
            priceEditText.setError("Price is required");
            isValid = false;
        }
        if (!imageUploaded) {
            Toast.makeText(context, "Image is not selected", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
    }

    private void removeImage() {
        SweetAlertDialog alertDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        alertDialog
                .setTitleText("Are you sure?")
                .setContentText("Do you want to remove this image?")
                .setConfirmText("Yes")
                .setCancelText("No")
                .setConfirmClickListener(sweetAlertDialog -> {
                    imageUploaded = false;
                    productImageView.setImageDrawable(null);
                    productImageView.setVisibility(View.GONE);
                    removeImageBtn.setVisibility(View.GONE);

                    FirebaseUtil.getProductImageReference(productId + "").delete();
                    alertDialog.dismiss();
                }).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                if (idEditText.getText().toString().trim().length() == 0) {
                    Toast.makeText(this, "Please fill the id first", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.show();

                productId = Integer.parseInt(idEditText.getText().toString());
                FirebaseUtil.getProductImageReference(productId + "").putFile(imageUri)
                        .addOnCompleteListener(t -> {
                            if (t.isSuccessful()) {
                                imageUploaded = true;
                                FirebaseUtil.getProductImageReference(productId + "").getDownloadUrl().addOnSuccessListener(uri -> {
                                    productImage = uri.toString();
                                    Picasso.get().load(uri).into(productImageView, new Callback() {
                                        @Override
                                        public void onSuccess() {
                                            dialog.dismiss();
                                        }
                                        @Override
                                        public void onError(Exception e) {
                                            dialog.dismiss();
                                            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    productImageView.setVisibility(View.VISIBLE);
                                    removeImageBtn.setVisibility(View.VISIBLE);
                                }).addOnFailureListener(e -> {
                                    dialog.dismiss();
                                    Toast.makeText(context, "Error getting download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                dialog.dismiss();
                                Toast.makeText(context, "Error uploading image: " + t.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    public interface MyCallback {
        void onCallback(String[] categories);
        void onCallback(int[] size);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        FirebaseUtil.getProductImageReference(productId + "").delete();
    }

    @Override
    protected void onResume() {
        super.onResume();

        getCategories(new MyCallback() {
            @Override
            public void onCallback(String[] cate) {
                arrayAdapter = new ArrayAdapter<>(context, R.layout.dropdown_item, cate);
                categoryDropDown.setAdapter(arrayAdapter);
                categoryDropDown.setOnItemClickListener((parent, view, position, id) -> {
                    category = parent.getItemAtPosition(position).toString();
                });
            }

            @Override
            public void onCallback(int[] size) {
                categories = new String[size[0]];
            }
        });
    }
}