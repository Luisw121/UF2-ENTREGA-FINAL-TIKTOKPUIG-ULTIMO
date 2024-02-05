package com.example.tiktokelpuig;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
    private List<Comment> comments = new ArrayList<>();

    public void setComments(List<Comment> comments) {
        this.comments = comments;

    }
    public void addComment(Comment comment) {
        comments.add(comment);
        notifyItemInserted(comments.size() - 1);
    }


    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView authorTextViewComment;
        private final TextView commentEditText;
        private final ImageView commentAuthorPhotoImageView;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextViewComment = itemView.findViewById(R.id.authorTextViewComment);
            commentEditText = itemView.findViewById(R.id.commentEditText);
            commentAuthorPhotoImageView = itemView.findViewById(R.id.commentAuthorPhotoImageView);
        }

        void bind(Comment comment) {
            authorTextViewComment.setText(comment.getAuthor());
            commentEditText.setText(comment.getContent());

            if (comment.getAuthor() != null) {
                Glide.with(itemView.getContext())
                        .load(comment.getAuthor())
                        .circleCrop()
                        .into(commentAuthorPhotoImageView);
            }else {
                commentAuthorPhotoImageView.setImageResource(R.drawable.baseline_person_24);
            }
        }
    }
}



