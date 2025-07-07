package com.example.newEcom.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.model.MessageModel;
import com.example.newEcom.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.text.SimpleDateFormat;

public class ChatAdapter extends FirestoreRecyclerAdapter<MessageModel, ChatAdapter.ChatViewHolder> {
    private final Context context;
    private final String currentUserId;

    public ChatAdapter(@NonNull FirestoreRecyclerOptions<MessageModel> options, Context context) {
        super(options);
        this.context = context;
        this.currentUserId = FirebaseUtil.getCurrentUserId();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull MessageModel model) {
        if (model.getSenderId().equals(currentUserId)) {
            holder.leftMessageLayout.setVisibility(View.GONE);
            holder.rightMessageLayout.setVisibility(View.VISIBLE);
            holder.messageTextViewRight.setText(model.getMessage());
            String time = new SimpleDateFormat("hh:mm a").format(model.getTimestamp().toDate());
            holder.timeTextViewRight.setText(time);
            holder.messageTextViewRight.setTextColor(context.getResources().getColor(android.R.color.white));
        } else {
            holder.leftMessageLayout.setVisibility(View.VISIBLE);
            holder.rightMessageLayout.setVisibility(View.GONE);
            holder.messageTextViewLeft.setText(model.getMessage());
            String time = new SimpleDateFormat("hh:mm a").format(model.getTimestamp().toDate());
            holder.timeTextViewLeft.setText(time);
            holder.messageTextViewLeft.setTextColor(context.getResources().getColor(android.R.color.black));
        }
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftMessageLayout, rightMessageLayout;
        TextView messageTextViewLeft, timeTextViewLeft, messageTextViewRight, timeTextViewRight;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            leftMessageLayout = itemView.findViewById(R.id.left_message_layout);
            rightMessageLayout = itemView.findViewById(R.id.right_message_layout);
            messageTextViewLeft = itemView.findViewById(R.id.messageTextViewLeft);
            timeTextViewLeft = itemView.findViewById(R.id.timeTextViewLeft);
            messageTextViewRight = itemView.findViewById(R.id.messageTextViewRight);
            timeTextViewRight = itemView.findViewById(R.id.timeTextViewRight);
        }
    }
}