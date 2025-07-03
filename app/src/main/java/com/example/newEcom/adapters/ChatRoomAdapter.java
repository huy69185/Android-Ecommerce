package com.example.newEcom.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.activities.AdminChatActivity;
import com.example.newEcom.model.ChatRoomModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.text.SimpleDateFormat;

public class ChatRoomAdapter extends FirestoreRecyclerAdapter<ChatRoomModel, ChatRoomAdapter.ChatRoomViewHolder> {
    private Context context;

    public ChatRoomAdapter(@NonNull FirestoreRecyclerOptions<ChatRoomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position, @NonNull ChatRoomModel model) {
        holder.userNameTextView.setText(model.getUserName());
        holder.lastMessageTextView.setText(model.getLastMessage().isEmpty() ? "No messages yet" : model.getLastMessage());
        String time = model.getLastMessageTimestamp() != null ?
                new SimpleDateFormat("dd/MM/yy hh:mm a").format(model.getLastMessageTimestamp().toDate()) : "";
        holder.timeTextView.setText(time);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminChatActivity.class);
            intent.putExtra("userId", model.getUserId());
            intent.putExtra("userName", model.getUserName());
            context.startActivity(intent);
        });
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView, lastMessageTextView, timeTextView;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            lastMessageTextView = itemView.findViewById(R.id.lastMessageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }
}