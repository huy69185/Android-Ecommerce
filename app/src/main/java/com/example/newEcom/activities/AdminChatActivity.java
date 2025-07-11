package com.example.newEcom.activities;

import android.os.Bundle;
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
            MessageModel messageModel = new MessageModel(adminId, message, Timestamp.now(), true);
            FirebaseUtil.getChatMessages(userId).document("Admin").set(messageModel).addOnSuccessListener(documentReference -> {
                FirebaseUtil.getChatRooms().document(userId).update(
                        "lastMessage", message,
                        "lastMessageTimestamp", Timestamp.now()
                );
                Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
            });
        }
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
    @Override
    protected void onResume() {
        super.onResume();
    }
}