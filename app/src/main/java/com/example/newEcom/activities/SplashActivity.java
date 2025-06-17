package com.example.newEcom.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.newEcom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private LottieAnimationView lottieAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        lottieAnimation = findViewById(R.id.lottieAnimationView);
        lottieAnimation.playAnimation();

        new Handler().postDelayed(() -> {
            FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currUser == null) {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            } else {
                String userId = currUser.getUid();
                Log.d(TAG, "Auto login with UserID: " + userId);
                if (userId == null) {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                } else if (currUser.getEmail() != null && currUser.getEmail().equals("huyh69185@gmail.com")) {
                    startActivity(new Intent(SplashActivity.this, AdminActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }
            }
            finish();
        }, 3000);
    }
}