package com.example.atlasevents;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class ImageUploader {
    private final FirebaseStorage storage;
    private final StorageReference storageRef;

    public ImageUploader() {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }
    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(String error);
    }
    public interface DeleteCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void uploadImage(Uri imageUri, UploadCallback callback) {
        String filename = "IMG_" + System.currentTimeMillis();
        StorageReference imageRef = storageRef.child("images/"+filename);
        UploadTask uploadTask = imageRef.putFile(imageUri);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                callback.onFailure(exception.getMessage());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        callback.onSuccess(uri.toString());
                    }
                });
            }
        });
    }

    public void deleteImage(String downloadUrl, DeleteCallback callback) {
        StorageReference imageRef = storage.getReferenceFromUrl(downloadUrl);

        imageRef.delete().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                callback.onFailure(exception.getMessage());
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                callback.onSuccess();
            }
        });
    }
}
