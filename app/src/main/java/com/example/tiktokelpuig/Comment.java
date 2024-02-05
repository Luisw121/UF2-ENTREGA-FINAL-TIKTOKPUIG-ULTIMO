package com.example.tiktokelpuig;

import java.util.Date;
public class Comment {
    public String commentId;
    private String author;
    private String content;
    private Date timestamp;

    public Comment() {}

    public Comment(String author, String content, Date timestamp, String commentId) {
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.commentId = commentId;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }
}

