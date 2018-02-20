package com.fogoa.photoapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.fogoa.photoapplication.extensions.BaseActivity;
import com.fogoa.photoapplication.extensions.crop.CropView;
import com.fogoa.photoapplication.misc.Constants;
import com.fogoa.photoapplication.misc.ImageDownloaderCache;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

public class CropMultiActivity extends BaseActivity {
    private static final String TAG = CropMultiActivity.class.getSimpleName();
    private ActionBar actionBar;

    //UI vars
    FrameLayout flCropImage;

    String masterImageList[] = null;
    int miScreenWidth;
    int miScreenHeight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_multi);

        actionBar = getSupportActionBar();
        if (actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.title_activity_cropmulti);
            actionBar.setSubtitle(R.string.title_sub_activity_cropmulti);
        }

        flCropImage = (FrameLayout) findViewById(R.id.flCropImage);

        Bundle extras = getIntent().getExtras();
        if(extras != null ) {
            if (extras.containsKey(Constants.INTENT_KEY_CROP_IMGPATHLIST)) {
                masterImageList = extras.getStringArray(Constants.INTENT_KEY_CROP_IMGPATHLIST);
            }
        }


        if (masterImageList != null && masterImageList.length > 0) {
            ViewTreeObserver vto3 = flCropImage.getViewTreeObserver();
            vto3.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    if (flCropImage.getWidth() > 0) {
                        miScreenWidth = flCropImage.getWidth();
                    }
                    if (flCropImage.getHeight() > 0) {
                        miScreenHeight = flCropImage.getHeight();
                    }

                    //add the CropViews to the frame
                    int borderSize = getActivity().getResources().getDimensionPixelSize(R.dimen.merge_photo_border);
                    int count = masterImageList.length;
                    //don't allow more than 6 images
                    if (count > 6) {
                        count = 6;
                    }
                    int xPos = 0;
                    int yPos = 0;
                    int xPosImg1 = 0;
                    for (int i = 0; i < count; i++) {
                        //calculate the view size
                        int imgW = Math.min(miScreenWidth, miScreenHeight) - borderSize;
                        int imgH = imgW;
                        if (count == 1) {
                            imgW = miScreenWidth;
                            imgH = miScreenWidth;
                        }
                        else if (count <= 3) {
                            imgW = (miScreenWidth - ((count - 1) * borderSize)) / count;
                            imgH = miScreenWidth;
                        }
                        else if (count == 4) {
                            imgW = ((miScreenWidth - borderSize) / 2);
                            imgH = imgW;
                        }
                        else if (count == 5) {
                            if (i < 2) {
                                imgW = ((miScreenWidth - borderSize) / 2);
                                imgH = imgW;
                            }
                            else {
                                imgW = ((miScreenWidth - borderSize) / 2);
                                //imgH = (imgDownloader.maxImageSize - (2 * borderSize)) / 3;
                                //note this formula returns the ceil value instead of the floor - from https://stackoverflow.com/questions/7139382/java-rounding-up-to-an-int-using-math-ceil
                                int a = (miScreenWidth - (2 * borderSize));
                                int b = 3;
                                imgH = (a - 1) / b + 1;
                            }
                        }
                        else if (count == 6) {
                            if (i < 1) {
                                int img3d = ((miScreenWidth - borderSize) / 3);
                                imgW = (img3d*2); //size is 2/3
                                //calculate the differnce facter and add it to the large image
                                int imgW1 = (miScreenWidth - (2 * borderSize)) / 3;
                                int dif1 = miScreenWidth - (imgW+borderSize+imgW1);
                                //int dif2 = imgDownloader.maxImageSize - ((imgW1*3)+(borderSize*2));
                                //add the extra to the single large image
                                imgW += dif1;
                                imgH = imgW;
                                xPosImg1 = imgW;
                            }
                            else {
                                imgW = (miScreenWidth - (2 * borderSize)) / 3;
                                if (i==5) {
                                    int dif2 = miScreenWidth - ((imgW*3)+(borderSize*2));
                                    if (dif2>0) {
                                        //add extra into last image
                                        imgW+=dif2;
                                    }
                                }
                                imgH = imgW;
                            }
                        }

                        //if (Constants.DEBUG) {
                        //    Log.d(TAG, "iv width: "+imgW);
                        //    Log.d(TAG, "iv width: "+imgH);
                        //}

                        //add the crop view
                        CropView cvCropImage = new CropView(getActivity());
                        //ImageView cvCropImage = new ImageView(getActivity());
                        if(Constants.DEBUG) Log.d(TAG, "set crop view shape full");
                        cvCropImage.setCropShape(CropView.Shape.FULL);
                        //if(Constants.DEBUG) Log.d(TAG, "set crop view ratio 0");
                        //cvCropImage.setViewportRatio(0);
                        //cvCropImage.setViewportRatio(3);
                        //cvCropImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        //cvCropImage.setAdjustViewBounds(true);
                        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(imgW, imgH);
                        params.leftMargin = xPos;
                        params.topMargin  = yPos;
                        flCropImage.addView(cvCropImage, params);
                        cvCropImage.setImageBitmap(ImageDownloaderCache.decodeSampledBitmapFromPath(masterImageList[i], ImageDownloaderCache.maxImageSize, ImageDownloaderCache.maxImageSize, false));

                        //increment the position
                        if (count <= 3) {
                            xPos += imgW+borderSize;
                        }
                        else if (count == 4) {
                            if (i % 2 == 0) {
                                //move next row
                                yPos += imgH + borderSize;
                            } else {
                                //move to next column
                                yPos = 0;
                                xPos += imgW + borderSize;
                            }
                        }
                        else if (count == 5) {
                            if (i == 1) {
                                //move to next column
                                yPos = 0;
                                xPos += imgW+borderSize;
                            }
                            else {
                                //move next row
                                yPos += imgH+borderSize;
                            }
                        }
                        else if (count == 6) {
                            if (i == 1) {
                                //move to next column in the same row
                                xPos += imgW+borderSize;
                            }
                            else if (i == 2) {
                                //move to next column
                                yPos = 0;
                                xPos += imgW+borderSize;
                                int xPos2 = xPosImg1+borderSize;
                                if (xPos2 > xPos) {
                                    xPos = xPos2;
                                }
                            }
                            else  {
                                //move next row
                                yPos += imgH+borderSize;
                            }
                        }

                    }


                    //GetBackgroundBitmap();


                    ViewTreeObserver obs = flCropImage.getViewTreeObserver();
                    obs.removeOnGlobalLayoutListener(this);
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

        for(int index = 0; index<((ViewGroup)flCropImage).getChildCount(); ++index) {
            View nextChild = ((ViewGroup)flCropImage).getChildAt(index);
            if (nextChild instanceof CropView) {
                ((CropView)nextChild).setImageBitmap(null);
            }
        }

    }

    public void CropImage() {
        flCropImage.setDrawingCacheEnabled(true);
        Bitmap bmImage = Bitmap.createBitmap(flCropImage.getDrawingCache(true));
        if (bmImage != null) {
            try {
                //Works but image get degraded overtime
                FileOutputStream out = openFileOutput(Constants.IMAGE_WORKING_FILE, Context.MODE_PRIVATE);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bmImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                out.write(bytes.toByteArray());
                out.close();

            } catch (Exception e) {
                //Log.d("Saving Bitmap", e.getMessage());
            }
        }

        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();

    }



}
