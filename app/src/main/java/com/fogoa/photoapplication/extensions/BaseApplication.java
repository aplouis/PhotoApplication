package com.fogoa.photoapplication.extensions;


import android.app.Application;
import android.content.res.Resources;
import android.os.Build;

import com.fogoa.photoapplication.data.models.UserItem;

import java.util.Date;
import java.util.Locale;

public class BaseApplication extends Application {
    private static final String TAG = BaseApplication.class.getSimpleName();

    public UserItem userItem = null;

    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        // Required initialization logic here!

    }

    /* application extension information from here https://github.com/codepath/android_guides/wiki/Understanding-the-Android-Application-Class
    // Currently not using the methods below...
    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
    */

    public Locale getLocale() {
        Resources resources = getResources();
        Locale locale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = resources.getConfiguration().getLocales().getFirstMatch(resources.getAssets().getLocales());
            if (locale == null) {
                locale = resources.getConfiguration().getLocales().get(0);
            }
        }
        else {
            locale = resources.getConfiguration().locale;
        }
        return locale;
    }


}
