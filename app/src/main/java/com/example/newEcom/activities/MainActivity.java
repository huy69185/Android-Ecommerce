package com.example.newEcom.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.newEcom.R;
import com.example.newEcom.fragments.CartFragment;
import com.example.newEcom.fragments.ChatFragment;
import com.example.newEcom.fragments.HomeFragment;
import com.example.newEcom.fragments.ProductFragment;
import com.example.newEcom.fragments.ProfileFragment;
import com.example.newEcom.fragments.SearchFragment;
import com.example.newEcom.fragments.WishlistFragment;
import com.example.newEcom.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mancj.materialsearchbar.SimpleOnSearchActionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    HomeFragment homeFragment;
    CartFragment cartFragment;
    SearchFragment searchFragment;
    WishlistFragment wishlistFragment;
    ProfileFragment profileFragment;
    ChatFragment chatFragment;
    LinearLayout searchLinearLayout;
    MaterialSearchBar searchBar;

    FragmentManager fm;
    FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchLinearLayout = findViewById(R.id.linearLayout);
        searchBar = findViewById(R.id.searchBar);

        homeFragment = new HomeFragment();
        cartFragment = new CartFragment();
        wishlistFragment = new WishlistFragment();
        profileFragment = new ProfileFragment();
        searchFragment = new SearchFragment();
        chatFragment = new ChatFragment();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                fm = getSupportFragmentManager();
                transaction = fm.beginTransaction();

                if (item.getItemId() == R.id.home) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    transaction.replace(R.id.main_frame_layout, homeFragment, "home");
                } else if (item.getItemId() == R.id.cart) {
                    if (!cartFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, cartFragment, "cart");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.wishlist) {
                    if (!wishlistFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, wishlistFragment, "wishlist");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.profile) {
                    if (!profileFragment.isAdded()) {
                        transaction.replace(R.id.main_frame_layout, profileFragment, "profile");
                        transaction.addToBackStack(null);
                    }
                } else if (item.getItemId() == R.id.chat) {
                    String currentUserId = FirebaseUtil.getCurrentUserId();
                    if (currentUserId != null && !currentUserId.equals(FirebaseUtil.ADMIN_USER_ID)) {
                        if (!chatFragment.isAdded()) {
                            transaction.replace(R.id.main_frame_layout, chatFragment, "chat");
                            transaction.addToBackStack(null);
                        }
                    } else {
                        Intent intent = new Intent(MainActivity.this, ChatListActivity.class);
                        startActivity(intent);
                        return true;
                    }
                }
                transaction.commit();
                return true;
            }
        });

        // Xử lý thông báo từ Intent
        handleNotification(getIntent());
        bottomNavigationView.setSelectedItemId(R.id.home);
        addOrRemoveBadge();

        getSupportFragmentManager().addOnBackStackChangedListener(() -> updateBottomNavigationSelectedItem());

        searchBar.setOnSearchActionListener(new SimpleOnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                super.onSearchStateChanged(enabled);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                if (!searchFragment.isAdded()) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_frame_layout, searchFragment, "search")
                            .addToBackStack(null)
                            .commit();
                }
                super.onSearchConfirmed(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                super.onButtonClicked(buttonCode);
            }
        });

        handleDeepLink();

        if (getIntent().getBooleanExtra("orderPlaced", false)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame_layout, profileFragment, "profile")
                    .addToBackStack(null)
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.profile);
        }

        // Kiểm tra quyền thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Lấy và lưu token không đồng bộ
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.addAuthStateListener(firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                String userId = firebaseAuth.getCurrentUser().getUid();
                new Thread(() -> {
                    try {
                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String token = task.getResult();
                                Log.d("FCM", "FCM Token fetched for userId: " + userId + ", Token: " + token);
                                saveTokenToFirestore(userId, token);
                            } else {
                                Log.e("FCM", "Failed to get FCM token", task.getException());
                            }
                        }).addOnFailureListener(e -> Log.e("FCM", "Error getting token", e));
                    } catch (Exception e) {
                        Log.e("FCM", "Exception in token retrieval", e);
                    }
                }).start();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotification(intent);
    }

    private void handleNotification(Intent intent) {
        if (intent.getBooleanExtra("navigateToChat", false)) {
            String roomId = intent.getStringExtra("roomId");
            if (roomId != null) {
                Bundle args = new Bundle();
                args.putString("roomId", roomId);
                chatFragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_frame_layout, chatFragment, "chat")
                        .addToBackStack(null)
                        .commit();
                bottomNavigationView.setSelectedItemId(R.id.chat);
            }
        } else if (intent.getStringExtra("orderParentId") != null) {
            String orderParentId = intent.getStringExtra("orderParentId");
            String itemId = intent.getStringExtra("itemId");
            Intent orderIntent = new Intent(this, OrderDetailsActivity.class);
            orderIntent.putExtra("orderParentId", orderParentId);
            orderIntent.putExtra("itemId", itemId);
            startActivity(orderIntent);
        }
    }

    private void saveTokenToFirestore(String userId, String token) {
        runOnUiThread(() -> {
            FirebaseFirestore db = FirebaseUtil.getFirestore();
            Map<String, Object> userData = new HashMap<>();
            userData.put("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token));
            userData.put("userName", FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : userId);
            db.collection("users").document(userId)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "Token saved successfully for userId: " + userId + ", Token: " + token))
                    .addOnFailureListener(e -> Log.e("FCM", "Failed to save token for userId: " + userId + ", Error: " + e.getMessage()));
        });
    }

    public void showSearchBar() {
        searchLinearLayout.setVisibility(View.VISIBLE);
    }

    public void hideSearchBar() {
        searchLinearLayout.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void updateBottomNavigationSelectedItem() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);

        if (currentFragment instanceof HomeFragment) {
            bottomNavigationView.setSelectedItemId(R.id.home);
        } else if (currentFragment instanceof CartFragment) {
            bottomNavigationView.setSelectedItemId(R.id.cart);
        } else if (currentFragment instanceof WishlistFragment) {
            bottomNavigationView.setSelectedItemId(R.id.wishlist);
        } else if (currentFragment instanceof ProfileFragment) {
            bottomNavigationView.setSelectedItemId(R.id.profile);
        } else if (currentFragment instanceof ChatFragment) {
            bottomNavigationView.setSelectedItemId(R.id.chat);
        }
    }

    public void addOrRemoveBadge() {
        FirebaseUtil.getCartItems().get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int n = task.getResult().size();
                        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.cart);
                        badge.setBackgroundColor(Color.parseColor("#FFF44336"));
                        if (n > 0) {
                            badge.setVisible(true);
                            badge.setNumber(n);
                        } else {
                            badge.setVisible(false);
                            badge.clearNumber();
                        }
                    }
                });
    }

    private void handleDeepLink() {
        FirebaseDynamicLinks.getInstance().getDynamicLink(getIntent())
                .addOnSuccessListener(pendingDynamicLinkData -> {
                    Uri deepLink = null;
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.getLink();
                    }
                    if (deepLink != null) {
                        Log.i("DeepLink", deepLink.toString());
                        String productId = deepLink.getQueryParameter("product_id");
                        Fragment fragment = ProductFragment.newInstance(Integer.parseInt(productId));
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_frame_layout, fragment)
                                .addToBackStack(null)
                                .commit();
                    }
                })
                .addOnFailureListener(e -> Log.i("Error123", e.toString()));
    }

    public void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_frame_layout, fragment)
                .addToBackStack(null)
                .commit();
    }
}