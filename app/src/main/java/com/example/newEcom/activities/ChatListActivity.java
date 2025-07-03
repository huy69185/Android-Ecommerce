package com.example.newEcom.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.adapters.ChatRoomAdapter;
import com.example.newEcom.model.ChatRoomModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ChatListActivity extends AppCompatActivity {
    private RecyclerView chatRoomRecyclerView;
    private ChatRoomAdapter chatRoomAdapter;
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        if (!FirebaseUtil.getCurrentUserId().equals(FirebaseUtil.ADMIN_USER_ID)) {
            Toast.makeText(this, "Only admin can access this page", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatRoomRecyclerView = findViewById(R.id.chatRoomRecyclerView);
        backButton = findViewById(R.id.backButton);

        setupChatRoomRecyclerView();

        backButton.setOnClickListener(v -> finish());
    }

    private void setupChatRoomRecyclerView() {
        FirestoreRecyclerOptions<ChatRoomModel> options = new FirestoreRecyclerOptions.Builder<ChatRoomModel>()
                .setQuery(FirebaseUtil.getChatRooms().orderBy("lastMessageTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING), ChatRoomModel.class)
                .build();

        chatRoomAdapter = new ChatRoomAdapter(options, this);
        chatRoomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRoomRecyclerView.setAdapter(chatRoomAdapter);
        chatRoomAdapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (chatRoomAdapter != null) {
            chatRoomAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatRoomAdapter != null) {
            chatRoomAdapter.stopListening();
        }
    }
}