package edu.neu.madcourse.stashbusters.presenters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import edu.neu.madcourse.stashbusters.StashPanelActivity;
import edu.neu.madcourse.stashbusters.contracts.LoginContract;
import edu.neu.madcourse.stashbusters.contracts.NewPanelContract;
import edu.neu.madcourse.stashbusters.model.StashPanelPost;
import edu.neu.madcourse.stashbusters.views.NewAccountActivity;
import edu.neu.madcourse.stashbusters.views.NewPanelActivity;
import edu.neu.madcourse.stashbusters.views.PersonalProfileActivity;

import static android.app.Activity.RESULT_OK;

/**
 * This class is responsible for handling actions from the View and updating the UI as required.
 */
public class NewPanelPresenter implements NewPanelContract.Presenter{
    private static final String TAG = NewPanelActivity.class.getSimpleName();
    private NewPanelContract.MvpView mView;
    private Context mContext;

    private DatabaseReference mDatabase;
    private StorageReference storageRef;
    private DatabaseReference userPostsRef;
    private FirebaseAuth mAuth;

    private String userId; // owner of the profile

    public NewPanelPresenter(Context context) {
        this.mContext = context;
        this.mView = (NewPanelContract.MvpView) context;

        storageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = mAuth.getCurrentUser().getUid();
        userPostsRef = mDatabase.child("posts").child(userId);
    }

    /**
     * Function that attempts to upload the post when the postButton is clicked.
     */
    @Override
    public void postButton(String title, String description, int material, Uri uri) {
        if (validateText(title)
                && validateText(description)
                && validateRadio(material)
                && validateUri(uri)) {
            uploadPhotoToStorage(title, description, material, uri);
        } else {
            mView.showToastMessage("Please fill all fields.");
        }
    }

    /**
     * Function that tells the View what to do when the imageButton is clicked.
     */
    @Override
    public void imageButton() {
        mView.takePhoto();
    }

    /**
     * Function to upload post photo Firebase storage and get the url.
     */
    private void uploadPhotoToStorage(final String title, final String description, final int material, Uri photoUri) {
        final StorageReference ref = storageRef.child("images/" + photoUri.getLastPathSegment());
        final UploadTask uploadTask = ref.putFile(photoUri);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.i(TAG, "uploadPostPhotoToStorage:success");

                // Save url
                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        // Continue with the task to get the download URL
                        return ref.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            String userPhotoUrl = task.getResult().toString();

                            uploadPost(title, description, material, userPhotoUrl);
                        } else {
                            // Handle failures
                            // ...
                        }
                    }
                });
            }
        });
    }

    /**
     * Function to save post to Firebase.
     */
    private void uploadPost(final String title, final String description, int material, final String photoUrl) {
        DatabaseReference newUserPostRef = userPostsRef.push(); // push used to generate unique id
        newUserPostRef.setValue(new StashPanelPost(title, description, photoUrl));

        startStashPanelActivity();
    }

    /**
     * Function that switches to StashPanelActivity.
     */
    public void startStashPanelActivity() {
        // TODO: Should eventually display the post that was just created.
        Intent intent = new Intent(mContext, StashPanelActivity.class);
        mContext.startActivity(intent);
    }

    /**
     * Helper function to check text is not empty.
     */
    private boolean validateText(String text) {
        if (text == "" || text == null) {
            return false;
        }
        return true;
    }

    /**
     * Helper function to check Uri is not empty.
     */
    private boolean validateUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        return true;
    }

    /**
     * Helper function to check int is not -1.
     */
    private boolean validateRadio(int number) {
        if (number == -1) {
            return false;
        }
        return true;
    }

}
