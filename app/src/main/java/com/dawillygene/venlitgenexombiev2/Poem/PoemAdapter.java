package com.dawillygene.venlitgenexombiev2.Poem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.dawillygene.venlitgenexombiev2.R;

import java.util.List;

public class PoemAdapter extends RecyclerView.Adapter<PoemAdapter.PoemViewHolder> {

    private List<Poem> poems;
    private AppCompatActivity activity;

    public PoemAdapter(List<Poem> poems, AppCompatActivity activity) {
        this.poems = poems;
        this.activity = activity;
    }

    @NonNull
    @Override
    public PoemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.poem_item, parent, false);
        return new PoemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PoemViewHolder holder, int position) {
        Poem poem = poems.get(position);
        holder.titleTextView.setText(poem.getTitle());
        holder.contentTextView.setText(poem.getContent());
        holder.authorTextView.setText(poem.getAuthor());
        holder.dateTextView.setText(poem.getCreatedAt());

        // Set click listener for delete button
        holder.deleteButton.setOnClickListener(v -> {
            // Confirm deletion
            new AlertDialog.Builder(activity)
                    .setTitle("Delete Poem")
                    .setMessage("Are you sure you want to delete this poem?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Call the delete method
                        ServerCommunication.deletePoem(poem.getId(), new ServerCommunication.PoemCallback() {
                            @Override
                            public void onSuccess(List<Poem> poems) {
                                // Refresh the list
                                ((PomeMainActivity) activity).loadPoems();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(activity, "Error deleting poem: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return poems.size();
    }

    public static class PoemViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, contentTextView, authorTextView, dateTextView;
        Button deleteButton;

        public PoemViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}