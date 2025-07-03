package com.example.newEcom.fragments;

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
import com.example.newEcom.adapters.ChatAdapter;
import com.example.newEcom.model.MessageModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class ChatFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageEditText;
    private ImageButton sendButton;
    private String roomId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageEditText = view.findViewById(R.id.messageEditText);
        sendButton = view.findViewById(R.id.sendButton);
        ShimmerFrameLayout shimmerFrameLayout = view.findViewById(R.id.shimmer_chat);

        String userId = FirebaseUtil.getCurrentUserId();
        if (userId == null || userId.equals(FirebaseUtil.ADMIN_USER_ID)) {
            Toast.makeText(getContext(), "Admin cannot use this chat", Toast.LENGTH_SHORT).show();
            getActivity().onBackPressed();
            return view;
        }

        roomId = userId; // roomId is userId for simplicity
        setupChatRecyclerView(view);
        shimmerFrameLayout.startShimmer();

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        // Initialize chat room
        FirebaseUtil.getUserProfile(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                String userName = document.getString("name") != null ? document.getString("name") : "User";
                FirebaseUtil.getChatRooms().document(roomId).set(
                        new com.example.newEcom.model.ChatRoomModel(roomId, userId, userName, "", Timestamp.now())
                );
            }
        });

        return view;
    }

    private void setupChatRecyclerView(View view) {
        FirestoreRecyclerOptions<MessageModel> options = new FirestoreRecyclerOptions.Builder<MessageModel>()
                .setQuery(FirebaseUtil.getChatMessages(roomId).orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING), MessageModel.class)
                .build();

        chatAdapter = new ChatAdapter(options, getContext());
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);
        chatAdapter.startListening();
        ShimmerFrameLayout shimmerContainer = view.findViewById(R.id.shimmer_chat);
        shimmerContainer.stopShimmer();
        shimmerContainer.setVisibility(View.GONE);
        chatRecyclerView.setVisibility(View.VISIBLE);
    }

    private void sendMessage(String message) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId != null && !userId.equals(FirebaseUtil.ADMIN_USER_ID)) {
            MessageModel messageModel = new MessageModel(userId, message, Timestamp.now(), false);
            FirebaseUtil.getChatMessages(roomId).add(messageModel).addOnSuccessListener(documentReference -> {
                FirebaseUtil.getUserProfile(userId).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String userName = task.getResult().getString("name") != null ? task.getResult().getString("name") : "User";
                        FirebaseUtil.getChatRooms().document(roomId).update(
                                "lastMessage", message,
                                "lastMessageTimestamp", Timestamp.now(),
                                "userName", userName
                        );
                    }
                });
                Toast.makeText(getContext(), "Message sent to Admin", Toast.LENGTH_SHORT).show();
            });
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