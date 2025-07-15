package com.example.newEcom.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.newEcom.R;
import com.example.newEcom.activities.MainActivity;
import com.example.newEcom.activities.OrderDetailsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "ShopEaseChannel";
    private static final String CHANNEL_NAME = "ShopEase Notifications";
    private static final int NOTIFICATION_ID_BASE = 100;
    private static final String TAG = "MyFCMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived called: " + remoteMessage.toString());
        String title = null;
        String body = null;
        Intent intent = null;

        // Xử lý notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification payload: Title = " + title + ", Body = " + body);
        }

        // Xử lý data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Data payload: " + remoteMessage.getData().toString());
            String type = remoteMessage.getData().get("type");
            if (type != null) {
                if (type.equals("chat")) {
                    String roomId = remoteMessage.getData().get("roomId");
                    intent = new Intent(this, MainActivity.class);
                    intent.putExtra("navigateToChat", true);
                    intent.putExtra("roomId", roomId);
                    Log.d(TAG, "Chat notification: roomId = " + roomId);
                } else if (type.equals("order")) {
                    String orderParentId = remoteMessage.getData().get("orderParentId");
                    String itemId = remoteMessage.getData().get("itemId");
                    intent = new Intent(this, OrderDetailsActivity.class);
                    intent.putExtra("orderParentId", orderParentId);
                    intent.putExtra("itemId", itemId);
                    Log.d(TAG, "Order notification: orderParentId = " + orderParentId + ", itemId = " + itemId);
                }
            }
            // Lấy title/body từ data nếu notification không có
            if (title == null) title = remoteMessage.getData().get("title");
            if (body == null) body = remoteMessage.getData().get("body");
        }

        // Hiển thị thông báo nếu có title và body
        if (title != null && body != null) {
            showNotification(title, body, intent);
        } else {
            Log.w(TAG, "No valid title or body to show notification: title=" + title + ", body=" + body);
        }
    }

    private void showNotification(String title, String body, Intent intent) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setShowBadge(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
            }
        }

        PendingIntent pendingIntent = null;
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(
                    this,
                    new Random().nextInt(1000),
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );
            Log.d(TAG, "PendingIntent created for intent: " + intent.toString());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        int notificationId = NOTIFICATION_ID_BASE + new Random().nextInt(1000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot show notification: POST_NOTIFICATIONS permission not granted");
            return;
        }

        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification shown with ID: " + notificationId);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "New token received: " + token);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            saveTokenToFirestore(token);
        } else {
            Log.w(TAG, "No authenticated user when receiving new token");
            auth.addAuthStateListener(firebaseAuth -> {
                if (firebaseAuth.getCurrentUser() != null) {
                    saveTokenToFirestore(token);
                }
            });
        }
    }

    private void saveTokenToFirestore(String token) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        String userName = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : userId;
        if (userId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> userData = new HashMap<>();
            userData.put("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token));
            userData.put("userName", userName != null ? userName : "Unknown");
            db.collection("users").document(userId)
                    .set(userData, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved successfully for userId: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save token for userId: " + userId + ", Error: " + e.getMessage()));
        } else {
            Log.e(TAG, "No userId available to save token");
        }
    }
}