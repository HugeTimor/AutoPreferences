package me.zeyuan.lib.autopreferences;

import android.app.Application;
import android.util.Log;

import com.facebook.stetho.Stetho;

import static android.content.ContentValues.TAG;

/**
 * Created by timor on 2017/4/21.
 */

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
        Log.i(TAG, "on app created! ");
    }
}
