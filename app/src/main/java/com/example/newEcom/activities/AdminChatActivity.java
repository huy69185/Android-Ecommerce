package com.example.newEcom.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.adapters.ChatAdapter;
import com.example.newEcom.model.MessageModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

public class AdminChatActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "USER_ID";
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

        //Nhận tiêu đề từ Intent
//        userId = getIntent().getStringExtra("userId");
        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User is not identified", Toast.LENGTH_SHORT).show();
            finish(); // Kết thúc nếu không có userId
            return;
        }

        //Thiết lập tiêu đề
        TextView userNameTextView = findViewById(R.id.userNameTextView);
        userNameTextView.setText("Chatting with Admin"); // Cập nhật tiêu đề cố định

        //Khởi tạo view
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);

        setupChatRecyclerView();

        //Gửi tin nhắn
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        //Quay về
        backButton.setOnClickListener(v -> onBackPressed()); // Xử lý nút Back
    }

    private void setupChatRecyclerView() {
        // Tham chiếu tới sub-collection admin_chats/{userId}/messages
        Query chatQuery = FirebaseUtil.getAdminChatMessages(userId)
                .orderBy("timestamp", Query.Direction.ASCENDING);

//        FirestoreRecyclerOptions<MessageModel> options = new FirestoreRecyclerOptions.Builder<MessageModel>()
//                .setQuery(FirebaseUtil.getAdminChatMessages(userId).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING), MessageModel.class)
//                .build();
        FirestoreRecyclerOptions<MessageModel> options =
                new FirestoreRecyclerOptions.Builder<MessageModel>()
                        .setQuery(chatQuery, MessageModel.class)
                        .build();
        chatAdapter = new ChatAdapter(options, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        chatAdapter.startListening();
    }

//    private void sendMessage(String message) {
//        String adminId = FirebaseUtil.ADMIN_USER_ID;
//        if (adminId != null) {
//            MessageModel messageModel = new MessageModel(adminId, message, Timestamp.now(), true);
//            FirebaseUtil.getAdminChatMessages(userId).add(messageModel);
//            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void sendMessage(@NonNull String message) {
        String adminId = FirebaseUtil.ADMIN_USER_ID;
        // Tạo model tin nhắn, đánh dấu isAdmin = true
        MessageModel msgModel = new MessageModel(adminId, message, Timestamp.now(), true);
        FirebaseUtil.getAdminChatMessages(userId)
                .add(msgModel)
                .addOnSuccessListener(docRef -> {
                    // Sau khi gửi thì scroll xuống cuối
                    if (chatAdapter != null) {
                        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fail to send: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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