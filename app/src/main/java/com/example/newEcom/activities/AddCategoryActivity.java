package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.model.BannerModel;
import com.example.newEcom.model.CategoryModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AddCategoryActivity extends AppCompatActivity {
    TextInputEditText idEditText, nameEditText, descEditText, colorEditText;
    Button imageBtn, addCategoryBtn;
    ImageView backBtn, categoryImageView;
    TextView removeImageBtn;

    String categoryImage;
    String productName;
    int categoryId;
    Context context = this;
    boolean imageUploaded = false;

    SweetAlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        idEditText = findViewById(R.id.idEditText);
        nameEditText = findViewById(R.id.nameEditText);
        descEditText = findViewById(R.id.descriptionEditText);
        colorEditText = findViewById(R.id.colorEditText);
        categoryImageView = findViewById(R.id.categoryImageView);

        imageBtn = findViewById(R.id.imageBtn);
        addCategoryBtn = findViewById(R.id.addCategoryBtn);
        backBtn = findViewById(R.id.backBtn);
        removeImageBtn = findViewById(R.id.removeImageBtn);

        // Lấy ID lớn nhất từ Firestore dựa trên categoryId
        FirebaseUtil.getCategories()
                .orderBy("categoryId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                Integer maxId = document.getLong("categoryId") != null ? document.getLong("categoryId").intValue() : 0;
                                categoryId = maxId + 1;
                            } else {
                                categoryId = 1; // Nếu không có dữ liệu, bắt đầu từ 1
                            }
                            idEditText.setText(String.valueOf(categoryId));
                            idEditText.setEnabled(false); // Không cho sửa ID
                        } else {
                            Toast.makeText(context, "Failed to fetch max ID: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            categoryId = 1;
                            idEditText.setText(String.valueOf(categoryId));
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

        addCategoryBtn.setOnClickListener(v -> {
            addToFirebase();
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

    private void addToFirebase() {
        if (!validate())
            return;

        categoryId = Integer.parseInt(idEditText.getText().toString());
        String name = nameEditText.getText().toString();
        String desc = descEditText.getText().toString();
        String color = colorEditText.getText().toString();

        // Tạo ID document từ tên category: viết thường và thay dấu cách bằng _
        String documentId = name.toLowerCase().replaceAll(" ", "_");
        CategoryModel category = new CategoryModel(name, categoryImage, color, desc, categoryId);

        FirebaseUtil.getCategories().document(documentId)
                .set(category)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(AddCategoryActivity.this, "Category has been added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to add category: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeImage() {
        SweetAlertDialog alertDialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        alertDialog
                .setTitleText("Are you sure?")
                .setContentText("Do you want to remove this image?")
                .setConfirmText("Yes")
                .setCancelText("No")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        imageUploaded = false;
                        categoryImageView.setImageDrawable(null);
                        categoryImageView.setVisibility(View.GONE);
                        removeImageBtn.setVisibility(View.GONE);

                        FirebaseUtil.getCategoryImageReference(categoryId + "").delete();
                        alertDialog.dismiss();
                    }
                }).show();
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
        if (descEditText.getText().toString().trim().length() == 0) {
            descEditText.setError("Description is required");
            isValid = false;
        }
        if (colorEditText.getText().toString().trim().length() == 0) {
            colorEditText.setError("Color is required");
            isValid = false;
        }
        if (colorEditText.getText().toString().charAt(0) != '#') {
            colorEditText.setError("Color should be HEX value");
            isValid = false;
        }

        if (!imageUploaded) {
            Toast.makeText(context, "Image is not selected", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        return isValid;
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

                categoryId = Integer.parseInt(idEditText.getText().toString());
                FirebaseUtil.getCategoryImageReference(categoryId + "").putFile(imageUri)
                        .addOnCompleteListener(t -> {
                            if (t.isSuccessful()) {
                                imageUploaded = true;
                                FirebaseUtil.getCategoryImageReference(categoryId + "").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        categoryImage = uri.toString();
                                        Picasso.get().load(uri).into(categoryImageView, new Callback() {
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
                                        categoryImageView.setVisibility(View.VISIBLE);
                                        removeImageBtn.setVisibility(View.VISIBLE);
                                    }
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

    public void onBackPressed() {
        super.onBackPressed();
        FirebaseUtil.getCategoryImageReference(categoryId + "").delete();
    }
}