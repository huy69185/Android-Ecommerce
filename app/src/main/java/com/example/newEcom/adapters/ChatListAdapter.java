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
import com.example.newEcom.model.ChatListModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.text.SimpleDateFormat;

public class ChatListAdapter
        extends FirestoreRecyclerAdapter<ChatListModel, ChatListAdapter.Holder> {

    private Context context;

    public ChatListAdapter(@NonNull FirestoreRecyclerOptions<ChatListModel> opts,
                           Context ctx) {
        super(opts);
        this.context = ctx;
    }

    @Override
    protected void onBindViewHolder(@NonNull Holder h, int pos, @NonNull ChatListModel m) {
        h.userIdText.setText(m.getUserId());
        h.lastMsgText.setText(m.getLastMessage());
        String time = new SimpleDateFormat("hh:mm a").format(m.getTimestamp().toDate());
        h.timeText.setText(time);

        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, AdminChatActivity.class);
            i.putExtra("USER_ID", m.getUserId());
            context.startActivity(i);
        });
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup p, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_chat_list, p, false);
        return new Holder(v);
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView userIdText, lastMsgText, timeText;
        public Holder(@NonNull View iv) {
            super(iv);
            userIdText   = iv.findViewById(R.id.userIdTextView);
            lastMsgText  = iv.findViewById(R.id.lastMessageTextView);
            timeText     = iv.findViewById(R.id.timeTextView);
        }
    }
}

