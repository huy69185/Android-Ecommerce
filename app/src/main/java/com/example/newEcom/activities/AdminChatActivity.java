// AdminChatActivity.java (phía admin)
package com.example.newEcom.activities;

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

    private ChatAdapter chatAdapter;
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton, backButton;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat);

//        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        userId = getIntent().getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User is not identified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView)findViewById(R.id.userNameTextView))
                .setText("Chat với: " + userId);

        chatRecyclerView   = findViewById(R.id.chatRecyclerView);
        messageEditText    = findViewById(R.id.messageEditText);
        sendButton         = findViewById(R.id.sendButton);
        backButton         = findViewById(R.id.backButton);

        setupChatRecyclerView();
        sendButton.setOnClickListener(v -> {
            String txt = messageEditText.getText().toString().trim();
            if (!txt.isEmpty()) {
                sendMessage(txt);
                messageEditText.setText("");
            }
        });
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupChatRecyclerView() {
        Query q = FirebaseUtil.getChatMessages(userId)
                .orderBy("timestamp", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<MessageModel> opts =
                new FirestoreRecyclerOptions.Builder<MessageModel>()
                        .setQuery(q, MessageModel.class)
                        .build();

        chatAdapter = new ChatAdapter(opts, this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        chatAdapter.startListening();
    }

    private void sendMessage(@NonNull String message) {
        MessageModel m = new MessageModel(
                FirebaseUtil.ADMIN_USER_ID,
                message,
                Timestamp.now(),
                true
        );
        FirebaseUtil.getChatMessages(userId)
                .add(m)
                .addOnSuccessListener(d ->
                        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount()-1)
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fail to send: "+e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override protected void onStart() {
        super.onStart();
        if (chatAdapter != null) chatAdapter.startListening();
    }
    @Override protected void onStop() {
        if (chatAdapter != null) chatAdapter.stopListening();
        super.onStop();
    }
}
