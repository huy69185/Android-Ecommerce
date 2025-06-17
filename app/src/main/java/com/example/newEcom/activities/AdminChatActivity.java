package com.example.newEcom.activities;

import android.os.Bundle;
import android.view.View;
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
    private String userId; // userId của khách hàng đang chat
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            finish(); // Kết thúc nếu không có userId
            return;
        }
        TextView userNameTextView = findViewById(R.id.userNameTextView);
        userNameTextView.setText("Chatting with Admin"); // Cập nhật tiêu đề cố định

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

        backButton.setOnClickListener(v -> onBackPressed()); // Xử lý nút Back
    }

    private void setupChatRecyclerView() {
        FirestoreRecyclerOptions<MessageModel> options = new FirestoreRecyclerOptions.Builder<MessageModel>()
                .setQuery(FirebaseUtil.getAdminChatMessages(userId).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING), MessageModel.class)
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
            FirebaseUtil.getAdminChatMessages(userId).add(messageModel);
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
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
}