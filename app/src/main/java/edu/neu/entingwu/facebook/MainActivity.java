package edu.neu.entingwu.facebook;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int RESULT_LOAD_IMAGE = 456;
    private static final String ADD = "add";
    private static final String INFO = "info";
    private static final String LOADING = "loading";
    private static final String NOTWEET = "notweet";
    private static final String TWEET_ID = "tweet_id";
    private static final String TWEET_TEXT = "tweet_text";
    private static final String TWEET_PICTURE = "tweet_picture";
    private static final String TWEET_DATE = "tweet_date";
    private static final String USER_ID = "user_id";
    private static final String FIRST_NAME = "first_name";
    private static final String PICTURE_PATH = "picture_path";
    private static final String UNFOLLOW = "Un Follow";
    private static final String FOLLOW = "Follow";

    @VisibleForTesting
    private ProgressDialog mProgressDialog;
    private LinearLayout mChannelInfo;
    private TextView mTxtNameFollowers;
    private Button mBtnFollow;
    private ListView mListView;
    private SearchView mSearchView;
    private Menu mMenu;
    private SaveSettings mSaveSettings;
    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;

    // Adapter class
    private ArrayList<AdapterItems> listTweetItems = new ArrayList<>();
    private TweetAdapter mTweetAdapter;
    private String tweetPicUrl;
    private String searchQuery;

    // 1- my followers post, 2- specifc user post, 3- search post
    private int userOperation = SearchType.MyFollowing;
    private int startFrom = 0;
    private int selectedUserID = 0;
    private int totalItemCountVisible = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mChannelInfo = (LinearLayout) findViewById(R.id.channelInfo);
        mChannelInfo.setVisibility(View.GONE);
        mTxtNameFollowers = (TextView)findViewById(R.id.txtnamefollowers);// main page
        mBtnFollow = (Button)findViewById(R.id.buFollow);
        mListView = (ListView) findViewById(R.id.LVNews);

        // Load user data setting
        mSaveSettings = new SaveSettings(getApplicationContext());
        mSaveSettings.loadData();

        /** PART 1. Load Different Items in ListView */
        //listTweetItems.add(new AdapterItems(null, null, null, ADD, null, null, null));
        mTweetAdapter = new TweetAdapter(this, listTweetItems);
        mListView.setAdapter(mTweetAdapter);
        loadTweets(0, SearchType.MyFollowing);
    }

    public void buFollowers(View view) {
        // 1- subscribe 2- unsubscribe
        int operation;
        String currUserId = SaveSettings.userId;
        String followStatus = mBtnFollow.getText().toString();

        if (followStatus.equals(FOLLOW)) {
            operation = 1;// add
            mBtnFollow.setText(UNFOLLOW);
        } else {
            operation = 2;// delete
            mBtnFollow.setText(FOLLOW);
        }

        //$query = "insert into following(user_id, following_user_id) values ("
        //        . $_GET['user_id'] . "," . $_GET['following_user_id']. ")";// op==1
        String url = "http://10.0.0.16/TwitterServer/UserFollowing.php?user_id=" + currUserId +
                "&following_user_id=" + selectedUserID + "&op=" + operation;
        Log.i(TAG, url);
        new AsyncTaskGetTweet().execute(url);
    }

    /** Part IV. Search View */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 1. Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        this.mMenu = menu;

        // 2. Associate searchable configuration with SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        final Context context = this;
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Toast.makeText(context, query, Toast.LENGTH_LONG).show();
                searchQuery = "";
                try {
                    // For space with name
                    searchQuery = java.net.URLEncoder.encode(query, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                // Search in posts
                loadTweets(0, SearchType.SearchPost); // search
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

        });
        // searchView.setOnCloseListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch(item.getItemId()) {
            case R.id.menu_home:
                loadTweets(0, SearchType.MyFollowing);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Part I. Load different items into ListView */
    private class TweetAdapter extends BaseAdapter {
        public ArrayList<AdapterItems> listDataAdapter;
        private Context context;

        public TweetAdapter(Context context, ArrayList<AdapterItems> listDataAdapter) {
            this.listDataAdapter = listDataAdapter;
            this.context = context;
        }

        @Override
        public int getCount() {
            return listDataAdapter.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        /** Part II. Add Tweet */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            /** Per Tweet */
            final AdapterItems items = listDataAdapter.get(position);

            /** I.1 WRITE NEW Tweet */
            if (items.tweet_date.equals(ADD)) {
                LayoutInflater mInflater = getLayoutInflater();
                View mView = mInflater.inflate(R.layout.tweet_add, null);
                final EditText etPost = (EditText) mView.findViewById(R.id.etPost);
                ImageView btnPost = (ImageView) mView.findViewById(R.id.iv_post);
                ImageView btnAttach = (ImageView) mView.findViewById(R.id.iv_attach);

                /** II.1 Attach Image */
                btnAttach.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadImage();
                    }
                });

                btnPost.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String tweets;
                        try {
                            // Encode for space with name.
                            tweets = java.net.URLEncoder.encode(etPost.getText().toString(), Operations.UTF_8);
                            tweetPicUrl = java.net.URLEncoder.encode(tweetPicUrl, Operations.UTF_8);
                        } catch (UnsupportedEncodingException e) {
                            tweets = ".";
                            e.printStackTrace();
                        }

                        /** III. AsyncTask write tweet */
                        //$query = "insert into tweets(user_id, tweet_text, tweet_picture) values (" . $_GET['user_id']
                        // . ",'" . $_GET['tweet_text'] . "','" . $_GET['tweet_picture'] . "')";
                        String user_id = SaveSettings.userId;
                        String url = "http://10.0.0.16/TwitterServer/TweetAdd.php?user_id=" + user_id +
                                "&tweet_text=" + tweets + "&tweet_picture=" + tweetPicUrl;
                        new AsyncTaskGetTweet().execute(url);
                        etPost.setText("");
                    }
                });
                return mView;

            } else if (items.tweet_date.equals(LOADING)) {
                LayoutInflater mInflater = getLayoutInflater();
                View mView = mInflater.inflate(R.layout.tweet_loading, null);
                return mView;

            } else if (items.tweet_date.equals(NOTWEET)) {
                LayoutInflater mInflater = getLayoutInflater();
                View mView = mInflater.inflate(R.layout.tweet_msg, null);
                return mView;

            } else {// Normal
                /** I.4 Read Tweet */
                LayoutInflater mInflater = getLayoutInflater();
                View mView = mInflater.inflate(R.layout.tweet_item, null);

                TextView txtUserName = (TextView) mView.findViewById(R.id.txt_user_name);
                txtUserName.setText(items.first_name);
                txtUserName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String currUserID = SaveSettings.userId;
                        selectedUserID = Integer.parseInt(items.user_id);
                        // IV. Search for specific person
                        loadTweets(0, SearchType.OnePerson);
                        mTxtNameFollowers.setText(items.first_name);

                        // $query = "select * from following where user_id =" . $_GET['user_id']
                        // . " and following_user_id = " . $_GET['following_user_id'];
                        String url = "http://10.0.0.16/TwitterServer/IsFollowing.php?user_id=" + currUserID +
                                "&following_user_id=" + selectedUserID;
                        new AsyncTaskGetTweet().execute(url);
                    }
                });

                TextView txtTweet = (TextView) mView.findViewById(R.id.txt_tweet);
                txtTweet.setText(items.tweet_text);

                TextView txtTweetDate = (TextView) mView.findViewById(R.id.txt_tweet_date);
                txtTweetDate.setText(items.tweet_date);

                ImageView tweetPicture = (ImageView) mView.findViewById(R.id.tweet_picture);
                // load picture url into imageview
                Picasso.with(context).load(items.tweet_picture).into(tweetPicture);

                ImageView userPicture = (ImageView) mView.findViewById(R.id.user_pic_path);
                Picasso.with(context).load(items.picture_path).into(userPicture);
                return mView;
            }
        }
    }

    /**  II.2 Load Image */
    private void loadImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && intent != null) {
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
            uploadImage(BitmapFactory.decodeFile(img_path));
        }
    }

    private void uploadImage(Bitmap bitmap) {
        showProgressDialog();
        mStorage = FirebaseStorage.getInstance();
        // II.3. Create a storage reference from our app
        mStorageRef = mStorage.getReferenceFromUrl("gs://facebook-4c6fc.appspot.com");
        DateFormat mDateFormat = new SimpleDateFormat("ddMMyyHHmmss");
        Date date = new Date();

        // II.4. Create a reference to "profile.jpg"
        String img_path = SaveSettings.userId + "_" + mDateFormat.format(date) + ".jpg";
        StorageReference imageRef = mStorageRef.child("images/" + img_path);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
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
                tweetPicUrl = taskSnapshot.getDownloadUrl().toString();
                Log.i(TAG, tweetPicUrl);
                hideProgressDialog();
            }
        });
    }

    /** PartIII. Write and Read Tweet from Server */
    public class AsyncTaskGetTweet extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String newData;
                // III.1 Define the url we have to connect with
                URL url = new URL(params[0]);
                // III.2 Make connect with url and send request
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                // III.3 Waiting for 7000ms for response
                urlConnection.setConnectTimeout(7000);

                try {
                    // III.4 Getting the response data
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    // III.5 Convert the stream to string
                    newData = Operations.convertStreamToString(in);
                    // III.6 Send to display data
                    publishProgress(newData);
                } finally {
                    // III.7 End connection
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... progress) {
            try {
                JSONObject json = new JSONObject(progress[0]);
                // Display response data
                String response = json.getString(Operations.MSG);
                if (response == null) {
                    return;
                }
                if (response.equals("tweet is added")) {
                    // IV. 1: My Followers Post, startFrom=0
                    loadTweets(0, userOperation);
                } else if (response.equals("has tweet")) {
                    // IV. 2: Return followers tweets
                    if (startFrom == 0) {
                        listTweetItems.clear();
                        listTweetItems.add(new AdapterItems(null, null, null, ADD, null, null, null));
                    } else {
                        // Remove what we are loading now
                        listTweetItems.remove(listTweetItems.size() - 1);
                    }
                    // Add followers' tweets
                    JSONArray tweets = new JSONArray(json.getString(INFO));
                    for (int i = 0; i < tweets.length(); i++) {
                        JSONObject js = tweets.getJSONObject(i);
                        // Add data to view
                        AdapterItems item = new AdapterItems(js.getString(TWEET_ID), js.getString(TWEET_TEXT),
                                js.getString(TWEET_PICTURE), js.getString(TWEET_DATE), js.getString(USER_ID),
                                js.getString(FIRST_NAME), js.getString(PICTURE_PATH));
                        listTweetItems.add(item);
                    }
                    mTweetAdapter.notifyDataSetChanged();

                } else if (response.equals("no tweet")) {
                    // Remove what we are loading now
                    if (startFrom == 0) {
                        listTweetItems.clear();
                        listTweetItems.add(new AdapterItems(null, null, null, ADD, null, null, null));
                    } else {
                        listTweetItems.remove(listTweetItems.size() -  1);
                    }

                    listTweetItems.add(new AdapterItems(null, null, null, NOTWEET, null, null, null));

                } else if (response.equals("is subscriber")) {
                    mBtnFollow.setText(UNFOLLOW);
                } else if (response.equals("is not subscriber")) {
                    mBtnFollow.setText(FOLLOW);
                }
            } catch (JSONException e) {
                Log.i(TAG, e.getMessage());
                // first time
                listTweetItems.clear();
                listTweetItems.add(new AdapterItems(null, null, null, ADD, null, null, null));
            }
            mTweetAdapter.notifyDataSetChanged();
        }
    }

    /** IV. 1 Display Loading */
    private void loadTweets(int startFrom, int userOperation) {
        this.startFrom = startFrom;
        this.userOperation = userOperation;
        String currUserId = SaveSettings.userId;

        /** 1. Add loading at the beginning */
        AdapterItems adapterItem = new AdapterItems(null, null, null, Operations.LOADING, null, null, null);
        if (startFrom == 0) {
            listTweetItems.add(0, adapterItem);
        } else {
            /** 2. Add loading at the end */
            listTweetItems.add(adapterItem);
        }
        mTweetAdapter.notifyDataSetChanged();

        /* 1- my followers post */
        String url = "http://10.0.0.16/TwitterServer/TweetList.php?user_id=" + currUserId +
                "&startFrom=" + startFrom + "&op=" + userOperation;

        /* 2- one user post */
        if (userOperation == SearchType.OnePerson) {
            // selectedUserID: the user you want to search for
            url = "http://10.0.0.16/TwitterServer/TweetList.php?user_id=" + selectedUserID +
                    "&startFrom=" + startFrom + "&op=" + userOperation;
        } else if (userOperation == SearchType.SearchPost) {
            /* 3- search post based on tweet_text */
            //$query = "select * from user_tweets where user_id = " . $_GET['user_id'] . " order by tweet_date DESC"
            //        . " LIMIT 20 OFFSET " . $_GET['startFrom'];
            url = "http://10.0.0.16/TwitterServer/TweetList.php?user_id=" + currUserId +
                    "&startFrom=" + startFrom + "&op=" + userOperation + "&query=" + searchQuery;
        }
        // IV.2 Return followers tweets
        new AsyncTaskGetTweet().execute(url);

        if (userOperation == SearchType.OnePerson) {
            mChannelInfo.setVisibility(View.VISIBLE);
        } else {
            mChannelInfo.setVisibility(View.GONE);
        }
    }

    /** Loading Display */
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
}
