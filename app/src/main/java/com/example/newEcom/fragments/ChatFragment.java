package com.example.newEcom.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.AdminChatActivity;
import com.example.newEcom.adapters.ChatAdapter;
import com.example.newEcom.model.MessageModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;

public class ChatFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton adminChatButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageEditText = view.findViewById(R.id.messageEditText);
        sendButton = view.findViewById(R.id.sendButton);
        adminChatButton = view.findViewById(R.id.adminChatButton);
        ShimmerFrameLayout shimmerFrameLayout = view.findViewById(R.id.shimmer_chat);

        setupChatRecyclerView(view);
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.startShimmer();
        }

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        adminChatButton.setOnClickListener(v -> {
            String currentUserId = FirebaseUtil.getCurrentUserId();
            if (currentUserId != null && !currentUserId.equals(FirebaseUtil.ADMIN_USER_ID)) {
                Intent intent = new Intent(getActivity(), AdminChatActivity.class);
                intent.putExtra("userId", currentUserId);
                startActivity(intent);
            }
        });

        return view;
    }

    private void setupChatRecyclerView(View view) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId != null && !userId.equals(FirebaseUtil.ADMIN_USER_ID)) {
            FirestoreRecyclerOptions<MessageModel> options = new FirestoreRecyclerOptions.Builder<MessageModel>()
                    .setQuery(FirebaseUtil.getUserChatMessages().orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING), MessageModel.class)
                    .build();

            chatAdapter = new ChatAdapter(options, getContext());
            chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            chatRecyclerView.setAdapter(chatAdapter);
            chatAdapter.startListening();
            ShimmerFrameLayout shimmerContainer = view.findViewById(R.id.shimmer_chat);
            if (shimmerContainer != null) {
                shimmerContainer.stopShimmer();
                shimmerContainer.setVisibility(View.GONE);
                chatRecyclerView.setVisibility(View.VISIBLE);
            } else {
                chatRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void sendMessage(String message) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId != null && !userId.equals(FirebaseUtil.ADMIN_USER_ID)) {
            MessageModel messageModel = new MessageModel(userId, message, Timestamp.now(), false);
            FirebaseUtil.getUserChatMessages().add(messageModel); // Gửi đến admin
            Toast.makeText(getContext(), "Message sent to Admin", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Admin cannot send messages here", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (chatAdapter != null) {
            chatAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (chatAdapter != null) {
            chatAdapter.stopListening();
        }
    }
}