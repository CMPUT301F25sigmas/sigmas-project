package com.example.atlasevents.utils;

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
import java.io.IOException;

/**
 * Utility class for handling image uploads and deletions in Firebase Storage.
 * <p>
 * Compresses images before uploading to minimize storage usage and improve performance.
 * </p>
 */
public class ImageUploader {

    /** Reference to Firebase Storage instance. */
    private final FirebaseStorage storage;

    /** Root reference in Firebase Storage. */
    private final StorageReference storageRef;

    /**
     * Initializes the {@link ImageUploader} with a Firebase Storage reference.
     */
    public ImageUploader() {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    /**
     * Callback interface for handling the result of an image upload.
     */
    public interface UploadCallback {
        /**
         * Called when an image is successfully uploaded to Firebase Storage.
         *
         * @param downloadUrl The public download URL of the uploaded image.
         */
        void onSuccess(String downloadUrl);

        /**
         * Called when the image upload fails.
         *
         * @param error A string describing the failure reason.
         */
        void onFailure(String error);
    }

    /**
     * Callback interface for handling the result of an image deletion.
     */
    public interface DeleteCallback {
        /**
         * Called when an image is successfully deleted from Firebase Storage.
         */
        void onSuccess();

        /**
         * Called when the image deletion fails.
         *
         * @param error A string describing the failure reason.
         */
        void onFailure(String error);
    }

    /**
     * Uploads an image to Firebase Storage.
     * <p>
     * The image is compressed to 25% quality in JPEG format before upload to reduce file size.
     * </p>
     *
     * @param resolver  The {@link ContentResolver} used to retrieve the image bitmap.
     * @param imageUri  The URI of the image to upload.
     * @param callback  The callback invoked upon success or failure.
     */
    public void uploadImage(ContentResolver resolver, Uri imageUri, UploadCallback callback) {
        Bitmap compressed_image = null;
        try {
            compressed_image = MediaStore.Images.Media.getBitmap(resolver, imageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (compressed_image != null) {
            compressed_image.compress(Bitmap.CompressFormat.JPEG, 25, baos);
        }
        byte[] fileInBytes = baos.toByteArray();

        String filename = "IMG_" + System.currentTimeMillis();
        StorageReference imageRef = storageRef.child("images/" + filename);
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

    /**
     * Deletes an image from Firebase Storage using its download URL.
     *
     * @param downloadUrl The download URL of the image to delete.
     * @param callback    The callback invoked upon success or failure.
     */
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
