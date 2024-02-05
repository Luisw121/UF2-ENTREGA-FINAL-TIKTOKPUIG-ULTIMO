package com.example.tiktokelpuig;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.collection.BuildConfig;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class NewPostFragment extends Fragment {
    Button publishButton;
    EditText postConentEditText;
    NavController navController;
    public AppViewModel appViewModel;
    String mediaUrl;
    Uri mediaUri;
    String mediaTipo;
    private CommentsAdapter commentsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_post, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        publishButton = view.findViewById(R.id.publishButton);
        postConentEditText = view.findViewById(R.id.postContentEditText);
        commentsAdapter = new CommentsAdapter();


        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publicar();
            }
        });
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        view.findViewById(R.id.camara_fotos).setOnClickListener(v -> tomarFoto());
        view.findViewById(R.id.camara_video).setOnClickListener(v -> tomarVideo());
        view.findViewById(R.id.grabar_audio).setOnClickListener(v -> grabarAudio());
        view.findViewById(R.id.imagen_galeria).setOnClickListener(v -> seleccionarImagen());
        view.findViewById(R.id.video_galeria).setOnClickListener(v -> seleccionarVideo());
        view.findViewById(R.id.audio_galeria).setOnClickListener(v -> seleccionarAudio());
        appViewModel.mediaSeleccionado.observe(getViewLifecycleOwner(), media -> {
            this.mediaUri = media.uri;
            this.mediaTipo = media.tipo;
            Glide.with(this).load(media.uri).into((ImageView) view.findViewById(R.id.previsualizacion));
        });
    }
    private final ActivityResultLauncher<String> galeria =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                appViewModel.setMediaSeleccionado(uri, mediaTipo);
            });
    private final ActivityResultLauncher<Uri> camaraFotos =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    isSuccess -> {
                        appViewModel.setMediaSeleccionado(mediaUri, "image");
                    });
    private final ActivityResultLauncher<Uri> camaraVideos =
            registerForActivityResult(new ActivityResultContracts.TakeVideo(), isSuccess
                    -> {
                appViewModel.setMediaSeleccionado(mediaUri, "video");
            });
    private final ActivityResultLauncher<Intent> grabadoraAudio =
            registerForActivityResult(new
                    ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    appViewModel.setMediaSeleccionado(result.getData().getData(),
                            "audio");
                }
            });
    private void seleccionarImagen() {
        mediaTipo = "image";
        galeria.launch("image/*");
    }
    private void seleccionarVideo() {
        mediaTipo = "video";
        galeria.launch("video/*");
    }
    private void seleccionarAudio() {
        mediaTipo = "audio";
        galeria.launch("audio/*");
    }
    private void tomarFoto() {
        try {
            mediaUri = FileProvider.getUriForFile(requireContext(),
                    BuildConfig.APPLICATION_ID + ".fileprovider", File.createTempFile("img",
                            ".jpg",
                            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)));
            camaraFotos.launch(mediaUri);
        } catch (IOException e) {}
    }
    private void tomarVideo() {
        try {
            mediaUri = FileProvider.getUriForFile(requireContext(),
                    BuildConfig.APPLICATION_ID + ".fileprovider", File.createTempFile("vid",
                            ".mp4",
                            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)));
            camaraVideos.launch(mediaUri);
        } catch (IOException e) {}
    }
    private void grabarAudio() {
        grabadoraAudio.launch(new
                Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION));
    }

    private void publicar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String postContent = postConentEditText.getText().toString();
        if (TextUtils.isEmpty(postContent)) {
            postConentEditText.setError("Required");
            return;
        }

        publishButton.setEnabled(false);

        if (mediaTipo == null) {
            guardarEnFirestore(postContent, null);
        }
        else
        {
            pujaIguardarEnFirestore(postContent);
        }

        // Verificar si hay un usuario autenticado
        if (user != null && user.isEmailVerified()) {
            Date currentCreationTime = Calendar.getInstance().getTime();

            // Crea una nueva instancia de Post con la información y el creationTime
            Post newPost = new Post(
                    user.getUid(),
                    user.getDisplayName(),
                    (user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "R.drawable.user"),
                    postContent,
                    mediaUrl,  // Asegúrate de tener mediaUrl definido antes de usarlo aquí
                    mediaTipo,
                    currentCreationTime);

            // Guardar newPost en Firestore
            FirebaseFirestore.getInstance().collection("posts")
                    .add(newPost)
                    .addOnSuccessListener(documentReference -> {
                        // Éxito al publicar
                        navController.navigate(R.id.homeFragment);
                        appViewModel.setMediaSeleccionado(null, null);
                    })
                    .addOnFailureListener(e -> {
                        // Manejar el fallo en la publicación
                        publishButton.setEnabled(true); // Vuelve a habilitar el botón en caso de fallo
                    });
        } else {
            // Manejar el caso en el que el usuario no está autenticado
            publishButton.setEnabled(true);
            // Vuelve a habilitar el botón si no hay usuario autenticado
        }
    }

    private void guardarEnFirestore(String postContent, String mediaUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Post post = new Post(
                user.getUid(),
                user.getDisplayName(),
                (user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "R.drawable.user"),
                postContent,
                mediaUrl,
                mediaTipo
        );
        FirebaseFirestore.getInstance().collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    navController.popBackStack();
                    appViewModel.setMediaSeleccionado(null, null);
                });
    }

    private void pujaIguardarEnFirestore(final String postText) {
        FirebaseStorage.getInstance().getReference(mediaTipo + "/" +
                        UUID.randomUUID())
                .putFile(mediaUri)
                .continueWithTask(task ->
                        task.getResult().getStorage().getDownloadUrl())
                .addOnSuccessListener(url -> {
                    // Asignar el valor de la URL después de cargar el archivo
                    mediaUrl = url.toString();
                    guardarEnFirestore(postText, mediaUrl);
                });
    }
    private void publicarComentario(String postKey, Comment newComment) {
        // Guardar el comentario en Firestore
        FirebaseFirestore.getInstance()
                .collection("posts")
                .document(postKey)
                .collection("comments")
                .add(newComment)
                .addOnSuccessListener(documentReference -> {
                    // Éxito al publicar el comentario
                    Log.d(TAG, "Comentario publicado con éxito");
                    String commentId = documentReference.getId();
                    cargarYNotificarComentarios(postKey, commentId);
                })
                .addOnFailureListener(e -> {
                    // Manejar el fallo en la publicación del comentario
                    Log.e(TAG, "Error al publicar comentario: " + e.getMessage());
                });
    }

    private void cargarYNotificarComentarios(String postKey, String commentId) {
        FirebaseFirestore.getInstance()
                .collection("posts")
                .document(postKey)
                .collection("comments")
                .document(commentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Éxito al cargar el comentario recién creado
                    Comment newComment = queryDocumentSnapshots.toObject(Comment.class);
                    if (newComment != null) {
                        // Añadir el comentario al adaptador de comentarios
                        commentsAdapter.addComment(newComment);

                    }
                })
                .addOnFailureListener(e -> {
                    // Manejar el fallo en la carga de comentarios
                    Log.e(TAG, "Error al cargar comentarios: " + e.getMessage());
                });
    }

}