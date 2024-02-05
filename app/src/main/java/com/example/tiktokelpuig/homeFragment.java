package com.example.tiktokelpuig;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class homeFragment extends Fragment {

    NavController navController;
    public AppViewModel appViewModel;
    private Button publishButton;
    private static final String TAG = "homeFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        navController = Navigation.findNavController(view);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
        publishButton = view.findViewById(R.id.publishButton);


        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });
        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);

        Query query = FirebaseFirestore.getInstance().collection("posts").limit(50);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class)
                .setLifecycleOwner(this)
                .build();

        postsRecyclerView.setAdapter(new PostsAdapter(options, currentUserId));
        new ViewModelProvider(requireActivity()).get(AppViewModel.class);



    }

    class PostsAdapter extends FirestoreRecyclerAdapter<Post, PostsAdapter.PostViewHolder> {
        private Post currentPost;
        CommentsAdapter commentsAdapter;
        private String currenUserId;


        public PostsAdapter(@NonNull FirestoreRecyclerOptions<Post> options, String currenUserId) {
            super(options);
            this.currenUserId = currenUserId;
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //Crear el ViewHolder con el diseño viewholder_post.xml
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false);

            return new PostViewHolder(view);
        }

        @Override
        protected void onBindViewHolder(@NonNull PostViewHolder holder, int position,
                                        @NonNull final Post post) {

            Date creationTime = post.creationTime;
            if (creationTime != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
                String formattedCreationTime = sdf.format(creationTime);
                holder.timeTextView.setText(formattedCreationTime);
            } else {
                holder.timeTextView.setText("Fecha no disponible");
            }

            // Gestion de likes
            final String postKey = getSnapshots().getSnapshot(position).getId();
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Glide.with(getContext()).load(post.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
            holder.authorTextView.setText(post.author);
            holder.contentTextView.setText(post.content);
            if (post.likes.containsKey(uid))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(post.likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                FirebaseFirestore.getInstance().collection("posts")
                        .document(postKey)
                        .update("likes." + uid, post.likes.containsKey(uid) ?
                                FieldValue.delete() : true);
            });
            //Miniatura de media
            if (post.mediaUrl != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.mediaType)) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.mediaUrl).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {

                holder.mediaImageView.setVisibility(View.GONE);
            }
            //BOTON DE ELIMINAR
            holder.deleteButton.setOnClickListener(view -> {
                // Obtener el ID del documento a eliminar
                String documentId = getSnapshots().getSnapshot(position).getId();

                // Obtener el ID del autor del post
                String postAuthorId = post.getUid();

                // Verificar si el usuario actual es el autor del post
                if (currenUserId.equals(postAuthorId)) {
                    // Eliminar el documento de Firestore
                    FirebaseFirestore.getInstance().collection("posts")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Éxito al eliminar
                            })
                            .addOnFailureListener(e -> {
                                // Manejar fallo en la eliminación
                            });
                } else {
                    // El usuario actual no es el autor del post, mostrar mensaje o tomar otra acción
                    // Por ejemplo, puedes mostrar un mensaje Toast o un AlertDialog
                    Toast.makeText(requireContext(), "No tienes permisos para eliminar este post", Toast.LENGTH_SHORT).show();
                }
            });

            //Para los comentarios
            holder.postCommentButton.setOnClickListener(view -> {
                String commentContent = holder.commentEditText.getText().toString().trim();

                if (!commentContent.isEmpty()) {
                    // Crear un nuevo comentario
                    Comment newComment = new Comment(
                            FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                            commentContent,
                            new Date(),
                            null // Deja que Firestore genere un ID único
                    );

                    // Verificar si el comentario es válido
                    if (newCommentIsValid(newComment)) {
                        // Guardar el comentario en Firestore
                        holder.commentEditText.setText(""); // Limpiar el campo de texto

                        FirebaseFirestore.getInstance()
                                .collection("posts")
                                .document(postKey)
                                .collection("comments")
                                .add(newComment)
                                .addOnSuccessListener(documentReference -> {
                                    // Éxito al publicar el comentario
                                    Log.d(TAG, "Comentario publicado con éxito");

                                    String commentId = documentReference.getId();

                                    loadCommentsAndNotifyAdapter(holder, post);
                                })
                                .addOnFailureListener(e -> {
                                    // Manejar el fallo en la publicación del comentario
                                    Log.e(TAG, "Error al publicar comentario: " + e.getMessage());
                                });
                    } else {
                        Log.e(TAG, "Error: el comentario no es válido");
                        // Manejar el caso en el que el comentario no es válido
                    }
                }
            });

            holder.commentsAdapter = new CommentsAdapter();
            holder.commentsRecyclerView.setAdapter(holder.commentsAdapter);
        }

        private boolean newCommentIsValid(Comment newComment) {
            // Agrega las verificaciones necesarias aquí
            // Devuelve true si el comentario es válido, false en caso contrario
            return newComment != null && newComment.getAuthor() != null && newComment.getContent() != null;
        }

        private void loadCommentsAndNotifyAdapter(PostViewHolder holder, Post post) {
            if (post != null && post.getUid() != null) {
                FirebaseFirestore.getInstance()
                        .collection("posts")
                        .document(post.getUid())
                        .collection("comments")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            // Éxito al cargar los comentarios
                            List<Comment> updatedComments = queryDocumentSnapshots.toObjects(Comment.class);
                            holder.bindComments(updatedComments);
                        })
                        .addOnFailureListener(e -> {
                            // Manejar el fallo en la publicación
                            Log.e(TAG, "Error al cargar comentarios: " + e.getMessage());
                        });
            } else {
                Log.e(TAG, "Error: post o su UID es nulo");
                // Manejar el caso en el que post o su UID es nulo
            }

        }






        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView authorPhotoImageView, likeImageView, mediaImageView;
            TextView authorTextView, contentTextView, numLikesTextView, timeTextView;
            ImageButton deleteButton, postCommentButton;
            RecyclerView commentsRecyclerView;
            CommentsAdapter commentsAdapter; //
            EditText commentEditText;


            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                authorPhotoImageView = itemView.findViewById(R.id.photoImageView);
                authorTextView = itemView.findViewById(R.id.authorTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
                numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
                likeImageView = itemView.findViewById(R.id.likeImageView);
                mediaImageView = itemView.findViewById(R.id.mediaImage);
                deleteButton = itemView.findViewById(R.id.deleteButton);
                commentsRecyclerView = itemView.findViewById(R.id.commentsRecyclerView);
                commentsAdapter = new CommentsAdapter();
                commentsRecyclerView.setAdapter(commentsAdapter);
                postCommentButton = itemView.findViewById(R.id.postCommentButton);
                commentEditText = itemView.findViewById(R.id.commentEditText);
                timeTextView = itemView.findViewById(R.id.timeTextView);
                commentsRecyclerView = itemView.findViewById(R.id.commentsRecyclerView);


            }
            void bindComments(List<Comment> comments) {
                if (commentsAdapter == null) {
                    Log.e(TAG, "Error: commentsAdapter es nulo en bindComments");
                    return;
                }

                commentsAdapter.setComments(comments);

                if (comments != null && !comments.isEmpty()) {
                    commentsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    commentsRecyclerView.setVisibility(View.GONE);
                }

                // Notificar al adaptador de comentarios después de establecer los comentarios
                commentsAdapter.notifyDataSetChanged();
            }

        }
    }

}