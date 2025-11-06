package com.example.atlasevents;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

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

    public void uploadImage(ContentResolver resolver, Uri imageUri, UploadCallback callback) {
        Bitmap compressed_image = null;
        try {
            compressed_image = MediaStore.Images.Media.getBitmap(resolver, imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        compressed_image.compress(Bitmap.CompressFormat.JPEG, 25, baos);
        byte[] fileInBytes = baos.toByteArray();

        String filename = "IMG_" + System.currentTimeMillis();
        StorageReference imageRef = storageRef.child("images/"+filename);
        UploadTask uploadTask = imageRef.putBytes(fileInBytes);

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
