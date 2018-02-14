package edu.neu.entingwu.facebook;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Auth";
    private static final String INFO = "info";
    private static final String PERMISSION_DENIED = "Permission Denied";
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int RESULT_OK = -1;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private ImageView ivUserImage;

    @VisibleForTesting
    private ProgressDialog mProgressDialog;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;
    private SaveSettings mSaveSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etName = (EditText) findViewById(R.id.etName);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        ivUserImage = (ImageView) findViewById(R.id.ivUserImage);

        /** Part I: Select Image from Gallery */
        ivUserImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkUserPermissions();
            }
        });

        /** Part II: Authentication and Load user image */
        // [START declare_auth]
        mAuth = FirebaseAuth.getInstance();
        // [START declare_auth_listener]
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.i(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    Log.i(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };
    }

    /** Part I.1. Request permission if permission hasn't been granted. */
    private void checkUserPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE },
                        REQUEST_CODE_ASK_PERMISSIONS);
                return;
            }
        }
        loadImage();
    }

    /** I.2. Request Permission */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch(requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadImage();
                } else {
                    // Permission denied
                    Toast.makeText(this, PERMISSION_DENIED, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /** I.3. Send intent to the gallery to get image */
    private void loadImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Operations.RESULT_LOAD_IMAGE);
    }

    /** I.4. Get Image */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // 456, -1, Intent { dat=content://media/external/images/media/50 flg=0x1 }
        Log.i(TAG, requestCode + ", " + resultCode + ", " + intent);

        if (requestCode == Operations.RESULT_LOAD_IMAGE && resultCode == RESULT_OK && intent != null) {
            Uri selectedImage = intent.getData();
            // _data : Path to the file on disk
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            // Uri, projection, selection, selectionArgs, sortOrder
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            // Move to first row
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            // /storage/emulated/0/DCIM/Camera/IMG_20161113_161451.jpg
            String img_path = cursor.getString(columnIndex);
            cursor.close();
            // Get Image from the phone
            ivUserImage.setImageBitmap(BitmapFactory.decodeFile(img_path));
        }
    }

    /** Part II.1 START on_start_add_listener */
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        signInAnonymously();
    }

    /** II.2 */
    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
                // If sign in fails, display a message to the user.
                // If sign in succeeds, the auth state listener will be notified and logic to handle the
                // signed in user can be handled in the listener.
                if (!task.isSuccessful()) {
                    Log.d(TAG, "signInAnonymously", task.getException());
                }
            }
        });
    }

    /** II.3 START on_stop_remove_listener */
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        hideProgressDialog();
    }

    /** II.4 Loading Display */
    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(Operations.LOADING);
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /** Part III. Only Authorized user can upload the image */
    public void buLogin(View view) {
        showProgressDialog();
        mStorage = FirebaseStorage.getInstance();
        // III.1. Create a storage reference from our app
        mStorageRef = mStorage.getReferenceFromUrl("gs://facebook-4c6fc.appspot.com");//url to upload
        DateFormat mDateFormat = new SimpleDateFormat("ddMMyyHHmmss");
        Date date = new Date();

        // III.2. Create a reference to "profile.jpg"
        final String img_path = mDateFormat.format(date) + ".jpg";
        // Manually create images/ directory in firebase console
        StorageReference imageRef = mStorageRef.child("images/" + img_path);
        ivUserImage.setDrawingCacheEnabled(true);
        ivUserImage.buildDrawingCache();

        BitmapDrawable drawable = (BitmapDrawable) ivUserImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);//compress
        byte[] imageData = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                String downloadUrl = taskSnapshot.getDownloadUrl().toString();
                String name = "";
                String email = etEmail.getText().toString();
                String password = etPassword.getText().toString();
                try {
                    name = java.net.URLEncoder.encode(etName.getText().toString(), Operations.UTF_8);
                    downloadUrl = java.net.URLEncoder.encode(downloadUrl, Operations.UTF_8);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                /** Part IV: Login and Register */
                String url = "http://10.0.0.16/TwitterServer/Register.php?first_name=" + name +
                        "&email=" + email + "&password=" + password + "&picture_path=" + downloadUrl;
                new AsyncTaskGetNews().execute(url);
            }
        });
    }

    /** Part IV. Get news from user */
    public class AsyncTaskGetNews extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String newsData;
                // IV.1 Define the url we have to connect with
                URL url = new URL(params[0]);
                // IV.2 Make connect with url and send request
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                // IV.3 Waiting for 7000ms for response
                urlConnection.setConnectTimeout(7000);

                try {
                    // IV.4 Getting the response data
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    // IV.5 Convert the stream to string
                    newsData = Operations.convertStreamToString(in);
                    // IV.6 Send to display data
                    publishProgress(newsData);
                    // IV.7 End connection
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            try {
                JSONObject json = new JSONObject(progress[0]);
                // Display response data
                if (json.getString(Operations.MSG) == null) {
                    Log.i(TAG, "null");
                    return;
                }
                // STEP1: Register ->
                if (json.getString(Operations.MSG).equalsIgnoreCase("user is added")) {
                    Log.i(TAG, "user is added");
                    Toast.makeText(getApplicationContext(), json.getString(Operations.MSG), Toast.LENGTH_LONG).show();
                    String email = etEmail.getText().toString();
                    String password = etPassword.getText().toString();
                    String url = "http://10.0.0.16/TwitterServer/Login.php?email=" + email + "&password=" + password;
                    new AsyncTaskGetNews().execute(url);// go to doInBackground
                }

                if (json.getString(Operations.MSG).equalsIgnoreCase("Pass Login")) {
                    Log.i(TAG, "Pass Login");
                    JSONArray userArray = new JSONArray(json.getString(INFO));
                    JSONObject userInfo = userArray.getJSONObject(0);
                    Toast.makeText(getApplicationContext(), userInfo.getString("user_id"), Toast.LENGTH_LONG).show();
                    hideProgressDialog();
                    mSaveSettings = new SaveSettings(getApplicationContext());
                    mSaveSettings.saveData(userInfo.getString("user_id"));
                    finish();
                }

            } catch (JSONException e) {
                Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
