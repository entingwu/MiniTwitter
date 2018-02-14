package edu.neu.entingwu.facebook;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Operations {

    private static final String TAG = "Operations";
    public static final String LOADING = "Loading";
    public static final String MSG = "msg";
    public static final String UTF_8 = "UTF-8";
    public static final int RESULT_LOAD_IMAGE = 456;
    private static final int RESULT_OK = -1;

    public static DateFormat mDateFormat;
    private Context context;

    public Operations(Context context) {
        this.context = context;
    }

    /** Convert stream to string */
    public static String convertStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String result = "";
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                result += line;
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
