package com.example.tiktokelpuig;

import com.example.tiktokelpuig.Comment;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Post {
    public String uid;
    public String author;
    public String authorPhotoUrl;
    public String content;
    public String mediaUrl;
    public String mediaType;
    @ServerTimestamp
    public Date creationTime;
    public Map<String, Boolean> likes = new HashMap<>();
    public List<Comment> comments = new ArrayList<>();

    // Constructor vacio requerido por Firestore
    public Post() {}
    public Post(String uid, String author, String authorPhotoUrl, String content, String mediaUrl, String mediaType, Date creationTime) {
        // Inicializa las propiedades de la clase con los parámetros proporcionados
    }

    public Post(String uid, String author, String authorPhotoUrl, String content, String mediaUrl, String mediaType) {
        this.uid = uid;
        this.author = author;
        this.authorPhotoUrl = authorPhotoUrl;
        this.content = content;
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
    }
    public List<Comment> getComments() {
        return comments;
    }
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public String getUid() {
        return uid;
    }
}