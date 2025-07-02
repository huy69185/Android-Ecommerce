package com.example.newEcom.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newEcom.R;
import com.example.newEcom.adapters.ChatListAdapter;
import com.example.newEcom.model.ChatListModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ChatListActivity extends AppCompatActivity {
    private ChatListAdapter adapter;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_chat_list);

        RecyclerView rv = findViewById(R.id.chatListRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        Query q = FirebaseFirestore.getInstance()
                .collection("admin_chats")
                .orderBy("timestamp", Query.Direction.DESCENDING);
        q.get()
                .addOnSuccessListener(snap -> {
                    Log.d("ChatListActivity", "Found " + snap.size() + " chats");
                    for (DocumentSnapshot d : snap) {
                        Log.d("ChatListActivity", "  doc = " + d.getId() + " -> " + d.getData());
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("ChatListActivity", "Fetch failed", e)
                );
        FirestoreRecyclerOptions<ChatListModel> opts =
                new FirestoreRecyclerOptions.Builder<ChatListModel>()
                        .setQuery(q, ChatListModel.class)
                        .build();

        adapter = new ChatListAdapter(opts, this);
        rv.setAdapter(adapter);
    }

    @Override protected void onStart() {
        super.onStart();
        adapter.startListening();
    }
    @Override protected void onStop() {
        adapter.stopListening();
        super.onStop();
    }
}

