package com.example.newEcom.fragments;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;
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
        setupMessageListener();
        shimmerFrameLayout.startShimmer();

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        // Lấy tên người dùng từ FirebaseAuth
        String userName = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : userId;
        if (userName == null) userName = userId; // Fallback nếu không có displayName
        FirebaseUtil.getChatRooms().document(roomId).set(
                new com.example.newEcom.model.ChatRoomModel(roomId, userId, userName, "", Timestamp.now())
        );

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
            String userName = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : userId;
            String messageId = FirebaseUtil.getChatMessages(roomId).document().getId(); // Auto-generated ID
            MessageModel messageModel = new MessageModel(userId, message, Timestamp.now(), false);
            Log.d("ChatFragment", "Sending message: " + message + " to roomId: " + roomId + " with messageId: " + messageId);
            FirebaseUtil.getChatMessages(roomId).document(messageId + userName).set(messageModel)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("ChatFragment", "Message saved successfully for messageId: " + messageId);
                        FirebaseUtil.getChatRooms().document(roomId).update(
                                "lastMessage", message,
                                "lastMessageTimestamp", Timestamp.now(),
                                "userName", userName != null ? userName : userId
                        );
                        Toast.makeText(getContext(), "Message sent to Admin", Toast.LENGTH_SHORT).show();

                        // Send notification to admin
                        Log.d("ChatFragment", "Calling sendChatNotification for receiverId: " + FirebaseUtil.ADMIN_USER_ID);
                        FirebaseUtil.sendChatNotification(FirebaseUtil.ADMIN_USER_ID, roomId, message);
                    })
                    .addOnFailureListener(e -> Log.e("ChatFragment", "Failed to send message: ", e));
        } else {
            Log.e("ChatFragment", "User ID is null or is Admin");
        }
    }
    private void setupMessageListener() {
        FirebaseUtil.getChatMessages(roomId).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("ChatFragment", "Listen failed.", error);
                return;
            }
            if (value != null && !value.isEmpty()) {
                for (DocumentSnapshot doc : value.getDocuments()) {
                    MessageModel message = doc.toObject(MessageModel.class);
                    if (message != null && message.isAdmin() && !message.getSenderId().equals(FirebaseUtil.getCurrentUserId())) {
                        // Gửi thông báo đến user (nếu cần, nhưng thường admin gửi trước)
                        FirebaseUtil.sendChatNotification(FirebaseUtil.getCurrentUserId(), roomId, message.getMessage());
                    }
                }
            }
        });
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