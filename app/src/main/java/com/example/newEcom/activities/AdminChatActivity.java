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
            Log.e(TAG, "No userId provided in intent. Finishing activity.");
            finish();
            return;
        }
        Log.d(TAG, "Initializing chat with userId: " + userId + ", userName: " + userName);

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
            } else {
                Log.w(TAG, "Empty message not sent");
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
        Log.d(TAG, "Chat RecyclerView set up for userId: " + userId);
    }

    private void sendMessage(String message) {
        String adminId = FirebaseUtil.ADMIN_USER_ID;
        if (adminId != null) {
            String messageId = FirebaseUtil.getChatMessages(userId).document().getId();
            MessageModel messageModel = new MessageModel(adminId, message, Timestamp.now(), true);
            Log.d(TAG, "Sending message: " + message + " to userId: " + userId + " with messageId: " + messageId);
            FirebaseUtil.getChatMessages(userId).document(messageId).set(messageModel)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Message saved successfully for messageId: " + messageId);
                        FirebaseUtil.getChatRooms().document(userId).update(
                                        "lastMessage", message,
                                        "lastMessageTimestamp", Timestamp.now()
                                ).addOnSuccessListener(aVoid -> Log.d(TAG, "Chat room updated for userId: " + userId))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update chat room: ", e));
                        Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
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
            Log.d(TAG, "Message listener updated for userId: " + userId);
            // Cloud Functions sẽ xử lý thông báo
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (chatAdapter != null) {
            chatAdapter.startListening();
            Log.d(TAG, "Chat adapter started listening for userId: " + userId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatAdapter != null) {
            chatAdapter.stopListening();
            Log.d(TAG, "Chat adapter stopped listening for userId: " + userId);
        }
    }
}