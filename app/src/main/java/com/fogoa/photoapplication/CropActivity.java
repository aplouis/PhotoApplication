package com.fogoa.photoapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;

import com.fogoa.photoapplication.extensions.BaseActivity;
import com.fogoa.photoapplication.extensions.crop.CropView;
import com.fogoa.photoapplication.misc.Constants;
import com.fogoa.photoapplication.misc.ImageDownloaderCache;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

public class CropActivity extends BaseActivity {
    private static final String TAG = CropActivity.class.getSimpleName();
    private ActionBar actionBar;

    //UI vars
    CropView cvCropImage;

    String mstrPicturePath;
    int miScreenWidth;
    int miScreenHeight;
    //protected Bitmap mBM;
    Bitmap mSelectedBM;

    //hold the rotation value of the original so we can rotatate before sending back
    int rotation = 0;
    int rotationInDegrees = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        actionBar = getSupportActionBar();
        if (actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_activity_crop);
            actionBar.setSubtitle(R.string.title_sub_activity_crop);
        }

        //get the ui vars
        cvCropImage = (CropView) findViewById(R.id.cvCropImage);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey(Constants.INTENT_KEY_CROP_IMGPATH)) {
            mstrPicturePath = extras.getString(Constants.INTENT_KEY_CROP_IMGPATH);

            try {
                //get the rotation of the original image
                ExifInterface exif = new ExifInterface(mstrPicturePath);
                rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                //Log.d(TAG, "EXIF Orientation: " + Integer.toString(rotation));
                rotationInDegrees = ImageDownloaderCache.exifToDegrees(rotation);
                //Log.d(TAG, "EXIF degrees: " + rotationInDegrees);
            } catch (Exception e) {

            }

        }

        if(Constants.DEBUG) Log.d(TAG, "image path: "+mstrPicturePath);

        if (mstrPicturePath != null && !mstrPicturePath.equals("")) {
            ViewTreeObserver vto3 = cvCropImage.getViewTreeObserver();
            vto3.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    if (cvCropImage.getWidth() > 0) {
                        miScreenWidth = cvCropImage.getWidth();
                    }
                    if (cvCropImage.getHeight() > 0) {
                        miScreenHeight = cvCropImage.getHeight();
                    }

                    //GetBackgroundBitmap();
                    cvCropImage.setImageBitmap(ImageDownloaderCache.decodeSampledBitmapFromPath(mstrPicturePath, miScreenWidth, miScreenHeight, false));
                    //cvCropImage.setImageBitmap(BitmapFactory.decodeFile(mstrPicturePath));

                    ViewTreeObserver obs = cvCropImage.getViewTreeObserver();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        obs.removeOnGlobalLayoutListener(this);
                    } else {
                        obs.removeGlobalOnLayoutListener(this);
                    }
                }

            });

        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_crop, menu);


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //onBackPressed();
                finish();
                return true;
            case R.id.action_crop_photo:
                CropImage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cvCropImage.setImageBitmap(null);
        //if (mBM != null && !mBM.isRecycled()) {
        //    mBM.recycle();
        //    mBM = null;
        //}
        if (mSelectedBM != null && !mSelectedBM.isRecycled()) {
            mSelectedBM.recycle();
            mSelectedBM = null;
        }

    }

    public void CropImage() {
        mSelectedBM = cvCropImage.crop();
        if (mSelectedBM != null) {
            try {

                FileOutputStream out = openFileOutput(Constants.IMAGE_WORKING_FILE, Context.MODE_PRIVATE);

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                mSelectedBM.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                //bm.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                out.write(bytes.toByteArray());

                out.close();
            }
            catch (Exception exc) {
                //finish on crash????
                finish();
            }

        }

        //send result back and finish acitvity
        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();
    }



}
