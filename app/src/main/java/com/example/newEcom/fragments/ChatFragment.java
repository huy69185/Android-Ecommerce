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
import com.google.firebase.firestore.Query;

public class ChatFragment extends Fragment {
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private EditText messageEditText;
    private ImageButton sendButton, adminChatButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        chatRecyclerView  = view.findViewById(R.id.chatRecyclerView);
        messageEditText   = view.findViewById(R.id.messageEditText);
        sendButton        = view.findViewById(R.id.sendButton);
        adminChatButton   = view.findViewById(R.id.adminChatButton);
        ShimmerFrameLayout shimmer = view.findViewById(R.id.shimmer_chat);

        setupChatRecyclerView(view);
        if (shimmer != null) {
            shimmer.startShimmer();
        }

        sendButton.setOnClickListener(v -> {
            String msg = messageEditText.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg);
                messageEditText.setText("");
            }
        });

        adminChatButton.setOnClickListener(v -> {
            String uid = FirebaseUtil.getCurrentUserId();
            if (uid != null && !uid.equals(FirebaseUtil.ADMIN_USER_ID)) {
                Intent i = new Intent(getActivity(), AdminChatActivity.class);
                i.putExtra(AdminChatActivity.EXTRA_USER_ID, uid);
                startActivity(i);
            }
        });

        return view;
    }

    private void setupChatRecyclerView(View view) {
        String uid = FirebaseUtil.getCurrentUserId();
        if (uid == null || uid.equals(FirebaseUtil.ADMIN_USER_ID)) return;

        Query q = FirebaseUtil.getChatMessages(uid)
                .orderBy("timestamp", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<MessageModel> opts =
                new FirestoreRecyclerOptions.Builder<MessageModel>()
                        .setQuery(q, MessageModel.class)
                        .build();

        chatAdapter = new ChatAdapter(opts, getContext());
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRecyclerView.setAdapter(chatAdapter);
        chatAdapter.startListening();

        ShimmerFrameLayout shimmer = view.findViewById(R.id.shimmer_chat);
        if (shimmer != null) {
            shimmer.stopShimmer();
            shimmer.setVisibility(View.GONE);
            chatRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void sendMessage(@NonNull String message) {
        String uid = FirebaseUtil.getCurrentUserId();
        if (uid == null || uid.equals(FirebaseUtil.ADMIN_USER_ID)) return;

        MessageModel m = new MessageModel(uid, message, Timestamp.now(), false);
        FirebaseUtil.getChatMessages(uid)
                .add(m)
                .addOnSuccessListener(d ->
                        Toast.makeText(getContext(), "Sent to admin", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onStart() {
        super.onStart();
        if (chatAdapter != null) chatAdapter.startListening();
    }
    @Override
    public void onStop() {
        if (chatAdapter != null) chatAdapter.stopListening();
        super.onStop();
    }
}
