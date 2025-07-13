package com.example.newEcom.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.adapters.ChatAdapter;
import com.example.newEcom.model.MessageModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class AdminChatActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private String userId;
    private String userName;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton backButton;
    private TextView userNameTextView;
    private static final String TAG = "AdminChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat);

        if (!FirebaseUtil.getCurrentUserId().equals(FirebaseUtil.ADMIN_USER_ID)) {
            Toast.makeText(this, "Only admin can access this chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = getIntent().getStringExtra("userId");
        userName = getIntent().getStringExtra("userName");
        if (userId == null) {
            finish();
            return;
        }

        userNameTextView = findViewById(R.id.userNameTextView);
        userNameTextView.setText("Chatting with " + userName);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);

        setupChatRecyclerView();
        setupMessageListener();

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void setupChatRecyclerView() {
        FirestoreRecyclerOptions<MessageModel> options = new FirestoreRecyclerOptions.Builder<MessageModel>()
                .setQuery(FirebaseUtil.getChatMessages(userId).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING), MessageModel.class)
                .build();

        chatAdapter = new ChatAdapter(options, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        chatAdapter.startListening();
    }

    private void sendMessage(String message) {
        String adminId = FirebaseUtil.ADMIN_USER_ID;
        if (adminId != null) {
            String messageId = FirebaseUtil.getChatMessages(userId).document().getId(); // ID duy nhất
            MessageModel messageModel = new MessageModel(adminId, message, Timestamp.now(), true);
            Log.d(TAG, "Sending message: " + message + " to userId: " + userId + " with messageId: " + messageId);
            FirebaseUtil.getChatMessages(userId).document(messageId + "Admin").set(messageModel)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Message saved successfully for messageId: " + messageId);
                        FirebaseUtil.getChatRooms().document(userId).update(
                                "lastMessage", message,
                                "lastMessageTimestamp", Timestamp.now()
                        );
                        Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();

                        // Gửi thông báo cho người dùng
                        Log.d(TAG, "Calling sendChatNotification for receiverId: " + userId);
                        FirebaseUtil.sendChatNotification(userId, userId, message);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to send message: ", e));
        } else {
            Log.e(TAG, "Admin ID is null");
        }
    }

    private void setupMessageListener() {
        FirebaseUtil.getChatMessages(userId).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }
            if (value != null && !value.isEmpty()) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    MessageModel message = doc.toObject(MessageModel.class);
                    if (message != null && !message.isAdmin() && !message.getSenderId().equals(FirebaseUtil.ADMIN_USER_ID)) {
                        // Gửi thông báo đến admin
                        FirebaseUtil.sendChatNotification(FirebaseUtil.ADMIN_USER_ID, userId, message.getMessage());
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (chatAdapter != null) {
            chatAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatAdapter != null) {
            chatAdapter.stopListening();
        }
    }
}