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

public class ChatFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageEditText;
    private ImageButton sendButton;
    private String roomId;
    private static final String TAG = "ChatFragment";

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
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
            return view;
        }

        // Lấy roomId từ Bundle (nếu có, từ thông báo đẩy) hoặc dùng userId
        if (getArguments() != null && getArguments().containsKey("roomId")) {
            roomId = getArguments().getString("roomId");
        } else {
            roomId = userId; // Mặc định roomId là userId
        }
        Log.d(TAG, "Initializing chat with roomId: " + roomId);

        setupChatRecyclerView(view);
        shimmerFrameLayout.startShimmer();

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            } else {
                Log.w(TAG, "Empty message not sent");
            }
        });

        // Lấy tên người dùng từ FirebaseAuth
        String userName = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : userId;
        if (userName == null) userName = userId; // Fallback nếu không có displayName
        FirebaseUtil.getChatRooms().document(roomId).set(
                        new com.example.newEcom.model.ChatRoomModel(roomId, userId, userName, "", Timestamp.now()),
                        com.google.firebase.firestore.SetOptions.merge()
                ).addOnSuccessListener(aVoid -> Log.d(TAG, "Chat room initialized for roomId: " + roomId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to initialize chat room: ", e));

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
        Log.d(TAG, "Chat RecyclerView set up for roomId: " + roomId);
    }

    private void sendMessage(String message) {
        String userId = FirebaseUtil.getCurrentUserId();
        if (userId != null && !userId.equals(FirebaseUtil.ADMIN_USER_ID)) {
            String userName = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getDisplayName() : userId;
            String messageId = FirebaseUtil.getChatMessages(roomId).document().getId(); // Auto-generated ID
            MessageModel messageModel = new MessageModel(userId, message, Timestamp.now(), false);
            Log.d(TAG, "Sending message: " + message + " to roomId: " + roomId + " with messageId: " + messageId);
            FirebaseUtil.getChatMessages(roomId).document(messageId).set(messageModel)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Message saved successfully for messageId: " + messageId);
                        FirebaseUtil.getChatRooms().document(roomId).update(
                                        "lastMessage", message,
                                        "lastMessageTimestamp", Timestamp.now(),
                                        "userName", userName != null ? userName : userId
                                ).addOnSuccessListener(aVoid -> Log.d(TAG, "Chat room updated for roomId: " + roomId))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update chat room: ", e));
                        Toast.makeText(getContext(), "Message sent to Admin", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to send message: ", e));
        } else {
            Log.e(TAG, "User ID is null or is Admin");
        }
    }

    private void setupMessageListener() {
        FirebaseUtil.getChatMessages(roomId).addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }
            Log.d(TAG, "Message listener updated for roomId: " + roomId);
            // Cloud Functions sẽ xử lý thông báo
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (chatAdapter != null) {
            chatAdapter.startListening();
            Log.d(TAG, "Chat adapter started listening for roomId: " + roomId);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (chatAdapter != null) {
            chatAdapter.stopListening();
            Log.d(TAG, "Chat adapter stopped listening for roomId: " + roomId);
        }
    }
}