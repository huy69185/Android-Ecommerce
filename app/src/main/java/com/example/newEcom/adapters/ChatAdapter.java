package com.example.newEcom.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.model.MessageModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.text.SimpleDateFormat;

public class ChatAdapter extends FirestoreRecyclerAdapter<MessageModel, ChatAdapter.ChatViewHolder> {
    private Context context;

    public ChatAdapter(@NonNull FirestoreRecyclerOptions<MessageModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel model = getItem(position);
        return model.isAdmin() ? R.layout.item_chat_message_receiver : R.layout.item_chat_message_sender;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(viewType, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull MessageModel model) {
        holder.messageTextView.setText(model.getMessage());
        String time = new SimpleDateFormat("hh:mm a").format(model.getTimestamp().toDate());
        holder.timeTextView.setText(time);

        holder.messageTextView.setBackgroundResource(model.isAdmin() ? R.drawable.bg_admin_message : R.drawable.bg_user_message);
        holder.messageTextView.setTextColor(context.getResources().getColor(android.R.color.black));
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView, timeTextView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
        }
    }
}