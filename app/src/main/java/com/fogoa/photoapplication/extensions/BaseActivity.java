package com.fogoa.photoapplication.extensions;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


import com.fogoa.photoapplication.extensions.BaseApplication;
import com.fogoa.photoapplication.misc.Constants;

import java.net.HttpURLConnection;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG =BaseActivity.class.getSimpleName();

    //vars used by all activities
    public BaseApplication appContext;
    public boolean isSaveInstanceState = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appContext = ((BaseApplication) getApplicationContext());

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (!isSaveInstance()) {
                    onBackPressed();
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        isSaveInstanceState = false;
    }

    @Override
    public void onSaveInstanceState(Bundle stateBundle) {
        isSaveInstanceState = true;
        super.onSaveInstanceState(stateBundle);
    }


    //add getActivity() so the same methods can be used in fragments and activities
    public BaseActivity getActivity() {
        return this;
    }

    public boolean isSaveInstance() {
        //not sure whu something like this doesn't already exist but there are many ui items that should not be done after the activity onSaveInstanceState has been called
        if (isFinishing() || isDestroyed()) {
            isSaveInstanceState = true;
        }
        return isSaveInstanceState;
    }



    public void showAlert(String message, boolean bSnack) {
        if (!isSaveInstance()) {
            // && !Constants.DEBUG
            if (bSnack) {
                Snackbar.make(getActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
            }
            else {
                AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
                bld.setMessage(message);
                bld.setNeutralButton("OK", null);
                bld.create().show();
            }
        }
    }

    public void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(getActivity());
        }
        if (view != null) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm!=null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }


}
