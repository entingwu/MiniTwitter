package edu.neu.entingwu.facebook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SaveSettings {

    private static final String MYPREFERENCES = "MyPref";
    private static final String USERID = "UserID";
    private static final String DEFAULT_USERID = "0";
    private SharedPreferences sharedRef;
    private Context context;
    public static String userId;

    public SaveSettings(Context context) {
        this.context = context;
        /** 1. In order to user shared preferences, you have to call a method getSharedPreferences() that
        *   that returns a SharedPreference instance pointing to the file that contains the values of preferences. */
        this.sharedRef = context.getSharedPreferences(MYPREFERENCES, Context.MODE_PRIVATE);
    }

    /** 2. Save something in the sharedpreferences by using SharedPreferences.Editor class.
     *     You will call the edit method of SharedPreference instance and will receive it in an editor object. */
    public void saveData(String userId) {
        SharedPreferences.Editor editor = sharedRef.edit();
        editor.putString(USERID, userId);
        editor.commit();
        loadData();
    }

    public void loadData() {
        userId = sharedRef.getString(USERID, DEFAULT_USERID);// default value "0"
        // Haven't login before. userId == "0"
        if (userId.equals(DEFAULT_USERID)) {
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
