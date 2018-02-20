package com.fogoa.photoapplication;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.support.transition.TransitionManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fogoa.photoapplication.extensions.BaseActivity;
import com.fogoa.photoapplication.misc.Constants;
import com.fogoa.photoapplication.misc.ImageDownloaderCache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import static com.fogoa.photoapplication.misc.Constants.INTENT_KEY_GALLERY_SELECTMULTI;
import static com.fogoa.photoapplication.misc.Constants.INTENT_RESULT_GALLERY;

public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    //UI vars
    FrameLayout flCropImage;
    View vSelect;
    ImageView imgView;

    int miScreenWidth;
    int miScreenHeight;

    String masterImagePath;
    String workingImagePath;
    String masterImageList[] = null;
    Bitmap workingBitmap = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get the ui vars
        flCropImage = (FrameLayout) findViewById(R.id.flCropImage);


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

                LayoutInflater inflater = (LayoutInflater)  getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                //add the CropViews to the frame
                //int borderSize = getActivity().getResources().getDimensionPixelSize(R.dimen.merge_photo_border);
                //int overlayHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.progress_photo_overlay_height);
                //width and height the same to make view square
                int imgW = miScreenWidth;
                int imgH = miScreenWidth;
                int xPos = 0;
                int yPos = 0;

                //Add views
                FrameLayout.LayoutParams paramsBefore = new FrameLayout.LayoutParams(imgW, imgH);
                paramsBefore.leftMargin = xPos;
                paramsBefore.topMargin  = yPos;
                vSelect = inflater.inflate(R.layout.include_photo_select, null);
                RelativeLayout rlSelectPhotoBefore = (RelativeLayout) vSelect.findViewById(R.id.rlSelectPhoto);
                rlSelectPhotoBefore.setOnClickListener(btnChangePhoto_OnClickListener);
                flCropImage.addView(vSelect, paramsBefore);
                //add the image view
                imgView = new ImageView(getActivity());
                imgView.setVisibility(View.INVISIBLE);
                //use the image view to change the image - maybe add abutton to do this
                imgView.setOnClickListener(btnChangePhoto_OnClickListener);
                flCropImage.addView(imgView, paramsBefore);

                ViewTreeObserver obs = flCropImage.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
            }

        });


    }

    private View.OnClickListener btnChangePhoto_OnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            //check for read external storage permision
            if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {
                Intent intent = new Intent(getActivity(), GalleryActivity.class);
                intent.putExtra(INTENT_KEY_GALLERY_SELECTMULTI, true);
                startActivityForResult(intent, INTENT_RESULT_GALLERY);
            }
            else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.PERMISSIONS_REQUEST_READ_STORAGE);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        //make sure to delete the working file
        if (workingImagePath != null && !workingImagePath.isEmpty()) {
            getActivity().deleteFile(Constants.IMAGE_WORKING_FILE);
        }

        if (workingBitmap != null && !workingBitmap.isRecycled()) {
            workingBitmap.recycle();
            workingBitmap = null;
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Make sure the request was successful
        if (resultCode == RESULT_OK) {
            if (requestCode == INTENT_RESULT_GALLERY) {
                if (data!=null) {
                    masterImageList = null;
                    masterImagePath = null;
                    String imgpath = null;
                    if (data.hasExtra("image_path")) {
                        imgpath = data.getStringExtra("image_path");
                    }
                    else if (data.hasExtra("image_path_list")) {
                        masterImageList = data.getStringArrayExtra("image_path_list");
                        if (masterImageList!= null && masterImageList.length == 1) {
                            //only one image
                            imgpath = masterImageList[0];
                        }
                    }


                    if (imgpath!= null && !imgpath.isEmpty()) {
                        masterImagePath = imgpath;
                        workingBitmap = ImageDownloaderCache.decodeSampledBitmapFromPath(masterImagePath, ImageDownloaderCache.maxImageSize, ImageDownloaderCache.maxImageSize);
                    }
                    else if (masterImageList!= null && masterImageList.length > 0) {

                        /*  adjust image demisions to fit  */
                        if (!getActivity().isFinishing()) {
                            ProgressDialog progressDialog1 = ProgressDialog.show(getActivity(), "", "Merging Photos...", true, false);

                            //int borderSize = 10;
                            int borderSize = getActivity().getResources().getDimensionPixelSize(R.dimen.merge_photo_border);
                            int count = masterImageList.length;
                            //don't allow more than 6 images
                            if (count > 6) {
                                count = 6;
                            }
                            Bitmap[] bitmapList = new Bitmap[count];
                            for (int i = 0; i < count; i++) {
                                Bitmap bmImage = ImageDownloaderCache.decodeSampledBitmapFromPath(masterImageList[i], ImageDownloaderCache.maxImageSize, ImageDownloaderCache.maxImageSize, false);
                                if (bmImage!=null) {
                                    int imgW = Math.min(bmImage.getWidth(), bmImage.getHeight()) - borderSize;
                                    int imgH = imgW;
                                    if (count <= 3) {
                                        //int dimension = Math.min(bmImage.getWidth(), bmImage.getHeight());
                                        imgW = (ImageDownloaderCache.maxImageSize - ((count - 1) * borderSize)) / count;
                                        imgH = ImageDownloaderCache.maxImageSize;
                                        //bitmapList[i] = ThumbnailUtils.extractThumbnail(bmImage, imgW, imgDownloader.maxImageSize, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                                    } else if (count == 4) {
                                        //int dimension = Math.min(bmImage.getWidth(), bmImage.getHeight()) - borderSize;
                                        imgW = ((ImageDownloaderCache.maxImageSize - borderSize) / 2);
                                        imgH = imgW;
                                        //bitmapList[i] = ThumbnailUtils.extractThumbnail(bmImage, imgWH, imgWH, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                                    } else if (count == 5) {
                                        if (i < 2) {
                                            imgW = ((ImageDownloaderCache.maxImageSize - borderSize) / 2);
                                            imgH = imgW;
                                            //bitmapList[i] = ThumbnailUtils.extractThumbnail(bmImage, imgWH, imgWH, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                                        }
                                        else {
                                            imgW = ((ImageDownloaderCache.maxImageSize - borderSize) / 2);
                                            //imgH = (imgDownloader.maxImageSize - (2 * borderSize)) / 3;
                                            //note this formula returns the ceil value instead of the floor - from https://stackoverflow.com/questions/7139382/java-rounding-up-to-an-int-using-math-ceil
                                            int a = (ImageDownloaderCache.maxImageSize - (2 * borderSize));
                                            int b = 3;
                                            imgH = (a - 1) / b + 1;
                                            //bitmapList[i] = ThumbnailUtils.extractThumbnail(bmImage, imgW, imgH, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                                        }
                                    } else if (count == 6) {
                                        if (i < 1) {
                                            int img3d = ((ImageDownloaderCache.maxImageSize - borderSize) / 3);
                                            //int a = (imgDownloader.maxImageSize - borderSize);
                                            //int b = 3;
                                            //int img3d = (a - 1) / b + 1;
                                            //imgW = (img3d*2) - borderSize; //size is 2/3
                                            imgW = (img3d*2); //size is 2/3
                                            //imgH = imgW + borderSize;
                                            //imgW = (img3d*2) + (borderSize / 2); //size is 2/3
                                            //calculate the differnce facter and add it to the large image
                                            int imgW1 = (ImageDownloaderCache.maxImageSize - (2 * borderSize)) / 3;
                                            int dif1 = ImageDownloaderCache.maxImageSize - (imgW+borderSize+imgW1);
                                            //int dif2 = imgDownloader.maxImageSize - ((imgW1*3)+(borderSize*2));
                                            //add the extra to the single large image
                                            //imgW += dif1+dif2;
                                            imgW += dif1;
                                            imgH = imgW;
                                            //if (Constants.DEBUG) Log.d(TAG, "borderSize : "+borderSize);
                                            //if (Constants.DEBUG) Log.d(TAG, "one third : "+img3d);
                                            //if (Constants.DEBUG) Log.d(TAG, "two thirds - border : "+imgW);
                                            //if (Constants.DEBUG) Log.d(TAG, "dif1 : "+dif1);
                                            //if (Constants.DEBUG) Log.d(TAG, "dif2 : "+dif2);
                                        }
                                        else {
                                            imgW = (ImageDownloaderCache.maxImageSize - (2 * borderSize)) / 3;
                                            if (i==5) {
                                                int dif2 = ImageDownloaderCache.maxImageSize - ((imgW*3)+(borderSize*2));
                                                if (dif2>0) {
                                                    //add extra into last image
                                                    imgW+=dif2;
                                                }
                                            }
                                            //note this formula returns the ceil value instead of the floor - from https://stackoverflow.com/questions/7139382/java-rounding-up-to-an-int-using-math-ceil
                                            //int a = (imgDownloader.maxImageSize - (2 * borderSize));
                                            //int b = 3;
                                            //imgW = (a - 1) / b + 1;
                                            //imgW = (img3d) - (borderSize*2/3);  //size is 1/3;
                                            //imgW = img3d - borderSize;  //size is 1/3;
                                            //imgW = img3d;  //size is 1/3;
                                            imgH = imgW;
                                        }
                                    }
                                    bitmapList[i] = ThumbnailUtils.extractThumbnail(bmImage, imgW, imgH, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                                    //if (Constants.DEBUG) {
                                    //    Log.d(TAG, "Image : "+(i+1)+" width: " +bitmapList[i].getWidth());
                                    //    Log.d(TAG, "Image : "+(i+1)+" height: " + bitmapList[i].getHeight());
                                    //}
                                }
                            }
                            //workingBitmap = Bitmap.createBitmap(bitmapList[0].getWidth() * 2, bitmapList[0].getHeight() * 2, Bitmap.Config.ARGB_8888);
                            workingBitmap = Bitmap.createBitmap(ImageDownloaderCache.maxImageSize, ImageDownloaderCache.maxImageSize, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(workingBitmap);
                            canvas.drawColor(Color.WHITE);
                            Paint paint = new Paint();
                            int xPos = 0;
                            int yPos = 0;
                            count = bitmapList.length;
                            for (int i = 0; i < bitmapList.length; i++) {
                                //if (Constants.DEBUG) {
                                //    Log.d(TAG, "Image : "+(i)+" xPos: " +xPos);
                                //    Log.d(TAG, "Image : "+(i)+" yPos: " + yPos);
                                //    //Log.d(TAG, "even? : "+(i % 2 ));
                                //    Log.d(TAG, "even? : "+(i % 2 ));
                                //}
                                //canvas.drawBitmap(bitmapList[i], bitmapList[i].getWidth() * (i % 2), bitmapList[i].getHeight() * (i / 2), paint);
                                if (bitmapList[i]!=null) {
                                    canvas.drawBitmap(bitmapList[i], xPos, yPos, paint);
                                }
                                //if (count <= 3 && i == 2) {
                                //    yPos = bitmapList[i].getHeight()+borderSize;
                                //}
                                //if (count == 4 && i == 1) {
                                //    yPos = bitmapList[i].getHeight()+borderSize;
                                //}
                                if (count == 4) {
                                    //if (i % 2 == 0) {
                                    //    //move to next column
                                    //    xPos += bitmapList[i].getWidth()+borderSize;
                                    //}
                                    //else {
                                    //    xPos = 0;
                                    //}
                                    /*
                                    if (i == 0 || i == 2) {
                                        //move next row
                                        yPos += bitmapList[i].getHeight()+borderSize;
                                    }
                                    if (i == 1) {
                                        //move to next column
                                        yPos = 0;
                                        xPos += bitmapList[i].getWidth()+borderSize;
                                    }
                                    */
                                    if (i % 2 == 0) {
                                        //move next row
                                        yPos += bitmapList[i].getHeight()+borderSize;
                                    }
                                    else {
                                        //move to next column
                                        yPos = 0;
                                        xPos += bitmapList[i].getWidth()+borderSize;
                                    }
                                }
                                else if (count == 5) {
                                    if (i == 1) {
                                        //move to next column
                                        yPos = 0;
                                        xPos += bitmapList[i].getWidth()+borderSize;
                                    }
                                    else {
                                        //move next row
                                        yPos += bitmapList[i].getHeight()+borderSize;
                                    }
                                }
                                else if (count == 6) {
                                    if (i == 1) {
                                        //move to next column in the same row
                                        xPos += bitmapList[i].getWidth()+borderSize;
                                    }
                                    else if (i == 2) {
                                        //move to next column
                                        yPos = 0;
                                        xPos += bitmapList[i].getWidth()+borderSize;
                                        int xPos2 = bitmapList[0].getWidth()+borderSize;
                                        if (xPos2 > xPos) {
                                            xPos = xPos2;
                                        }
                                    }
                                    else  {
                                        //move next row
                                        yPos += bitmapList[i].getHeight()+borderSize;
                                    }
                                }
                                else {
                                    //this is for 2 or 3 images
                                    //move to next column
                                    xPos += bitmapList[i].getWidth()+borderSize;
                                }
                            }

                            if (progressDialog1!=null && progressDialog1.isShowing()) {
                                progressDialog1.dismiss();
                            }

                        }
                    }

                    if (workingBitmap!=null) {
                        TransitionManager.beginDelayedTransition(flCropImage);

                        if (imgView!=null) {
                            if (vSelect!=null) {
                                vSelect.setVisibility(View.INVISIBLE);
                            }
                            imgView.setImageBitmap(workingBitmap);
                            imgView.setVisibility(View.VISIBLE);
                        }


                        //save the working image
                        try {
                            FileOutputStream out = openFileOutput(Constants.IMAGE_WORKING_FILE, Context.MODE_PRIVATE);
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            workingBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                            out.write(bytes.toByteArray());
                            out.close();
                            File workingImageFile = getActivity().getFileStreamPath(Constants.IMAGE_WORKING_FILE);
                            if (workingImageFile != null) {
                                workingImagePath = workingImageFile.getAbsolutePath();
                            }
                        } catch (Exception e) {

                        }

                        //call to crop image - maybe do as button later
                        if (masterImagePath!= null && !masterImagePath.isEmpty()) {
                            Intent pcIntent = new Intent(getActivity(), CropActivity.class);
                            pcIntent.putExtra(Constants.INTENT_KEY_CROP_IMGPATH, masterImagePath);
                            startActivityForResult(pcIntent, Constants.INTENT_RESULT_CROP);
                        }
                        else if (masterImageList!= null && masterImageList.length > 0) {
                            //open multi image crop
                            Intent pcIntent = new Intent(getActivity(), CropMultiActivity.class);
                            pcIntent.putExtra(Constants.INTENT_KEY_CROP_IMGPATHLIST, masterImageList);
                            startActivityForResult(pcIntent, Constants.INTENT_RESULT_CROP);

                        }


                    }


                }
            }
            else if (requestCode == Constants.INTENT_RESULT_CROP) {
                try {
                    File workingImageFile = getActivity().getFileStreamPath(Constants.IMAGE_WORKING_FILE);
                    if (workingImageFile != null) {
                        workingImagePath = workingImageFile.getAbsolutePath();
                        if (workingImagePath != null && !workingImagePath.isEmpty()) {
                            workingBitmap = BitmapFactory.decodeStream(getActivity().openFileInput(Constants.IMAGE_WORKING_FILE));
                            if (workingBitmap != null && !workingBitmap.isRecycled()) {
                                TransitionManager.beginDelayedTransition(flCropImage);

                                if (imgView!=null) {
                                    if (vSelect!=null) {
                                        vSelect.setVisibility(View.INVISIBLE);
                                    }
                                    imgView.setImageBitmap(workingBitmap);
                                    imgView.setVisibility(View.VISIBLE);
                                }
                            }

                        }
                    }

                }
                catch (Exception e) {
                    //Log.d(TAG,e.getMessage()+e.getStackTrace());
                    //return null;
                }

            }




        }
    }


}
