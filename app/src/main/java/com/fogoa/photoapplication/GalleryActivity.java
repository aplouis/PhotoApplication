package com.fogoa.photoapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.fogoa.photoapplication.adapters.GalleryAdapter;
import com.fogoa.photoapplication.data.listeners.OnLoadMore;
import com.fogoa.photoapplication.data.models.GalleryItem;
import com.fogoa.photoapplication.extensions.BaseActivity;
import com.fogoa.photoapplication.extensions.GridSpacingItemDecoration;
import com.fogoa.photoapplication.misc.Constants;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.fogoa.photoapplication.misc.Constants.INTENT_KEY_GALLERY_SELECTMULTI;
import static com.fogoa.photoapplication.misc.Constants.INTENT_RESULT_CAMERA;

public class GalleryActivity extends BaseActivity {
    private static final String TAG = GalleryActivity.class.getSimpleName();

    // UI references.
    private SwipeRefreshLayout swlContainer;
    RecyclerView rvGallery;
    private RecyclerView.Adapter rvGalleryAdapter;
    private RecyclerView.LayoutManager rvGalleryLayoutManager;
    private ArrayList<GalleryItem> adapterList = new ArrayList<GalleryItem>();
    private final static int pageMax = 30;
    int pageCurrent = 0;

    String mCurrentPhotoPath;
    boolean bSelMultiple = false;
    private ArrayList<GalleryItem> adapterListProfile = new ArrayList<GalleryItem>();
    String lastImageId = "";
    private boolean bListEnd = false;
    int pageCurrentProfile = 0;
    MenuItem miCamera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setTitle(R.string.title_activity_gallery);
            getSupportActionBar().setSubtitle(R.string.title_sub_activity_gallery);
        }

        bSelMultiple = false;
        Bundle bundle = getActivity().getIntent().getExtras();
        if(bundle != null){
            bSelMultiple = bundle.getBoolean(INTENT_KEY_GALLERY_SELECTMULTI, bSelMultiple);
        }

        rvGallery = (RecyclerView)findViewById(R.id.rvGallery);
        rvGalleryLayoutManager = new GridLayoutManager(getApplicationContext(),3);

        rvGallery.setLayoutManager(rvGalleryLayoutManager);
        int spanCount = 3; // 3 columns
        int spacing = 4; // 4px
        int dbspacing = Math.round(spacing * getResources().getDisplayMetrics().density);
        boolean includeEdge = false;
        rvGallery.addItemDecoration(new GridSpacingItemDecoration(spanCount, dbspacing, includeEdge));

        adapterList = getCameraImages(getActivity(), pageMax, (pageMax*pageCurrent));

        rvGalleryAdapter = new GalleryAdapter(getActivity(), adapterList, rvGallery_OnLoadMoreListener, bSelMultiple);
        rvGallery.setAdapter(rvGalleryAdapter);

        swlContainer = (SwipeRefreshLayout) findViewById(R.id.swlContainer);
        swlContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.

                //clear list and adptor on refresh
                //adapterList.clear();
                adapterList = new ArrayList<GalleryItem>();

                pageCurrent = 0;
                adapterList = getCameraImages(getActivity(), pageMax, (pageMax*pageCurrent));
                //rvGalleryAdapter.notifyDataSetChanged();
                rvGalleryAdapter = new GalleryAdapter(getActivity(), adapterList, rvGallery_OnLoadMoreListener, bSelMultiple);
                rvGallery.setAdapter(rvGalleryAdapter);

                swlContainer.setRefreshing(false);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.clear();
        getMenuInflater().inflate(R.menu.menu_gallery, menu);

        miCamera = menu.findItem(R.id.action_add_photo);
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) == null) {
            //only show camera action if device has a camera
            miCamera.setVisible(false);
        }

        if (!bSelMultiple) {
            //only show done action on multiple select
            MenuItem itemDone = menu.findItem(R.id.action_done);
            itemDone.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                setResult(RESULT_CANCELED, null);
                finish();
                return true;
            case R.id.action_add_photo:
                //if (Constants.DEBUG) Log.d(TAG, "camrea menu item clicked");
                //launch camera acitvity
                if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ) {
                    dispatchTakePictureIntent();
                }
                else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.PERMISSIONS_REQUEST_CAMERA_EXTERNAL_STORAGE);
                }
                return true;
            case R.id.action_done:
                String[] all_path = new String[0];
                //ProgressDialog progressDialog1 = null;
                //if (!getActivity().isFinishing()) {
                //    progressDialog1 = ProgressDialog.show(getActivity(), "", "Merging Photos...", true, false);
                //}
                if (rvGalleryAdapter!=null) {
                    ArrayList<GalleryItem> rvItemListSelected = ((GalleryAdapter)rvGalleryAdapter).getSelectedItems();
                    all_path = new String[rvItemListSelected.size()];
                    for (GalleryItem sgi: rvItemListSelected ) {
                        all_path[rvItemListSelected.indexOf(sgi)] =  sgi.img_uri;
                    }
                    //for (int i = 0; i < rvItemListSelected.size(); i++) {
                    //    all_path[i] = rvItemListSelected.get(i).img_uri;
                    //}
                }
                //if (progressDialog1!=null && progressDialog1.isShowing()) {
                //    progressDialog1.dismiss();
                //}
                Intent data = new Intent();
                data.putExtra("image_path_list", all_path);
                getActivity().setResult(getActivity().RESULT_OK, data);
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_RESULT_CAMERA && resultCode == RESULT_OK) {
            //Bundle extras = data.getExtras();
            //Bitmap imageBitmap = (Bitmap) extras.get("data");
            //mImageView.setImageBitmap(imageBitmap);
            if (mCurrentPhotoPath != null) {
                //add file to gallery
                //
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(mCurrentPhotoPath);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);
                //

                Intent rdata = new Intent();
                rdata.putExtra("image_path", mCurrentPhotoPath);
                getActivity().setResult(RESULT_OK, rdata);
                getActivity().finish();
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //Log.d(TAG, "onRequestPermissionsResult requestCode: "+ requestCode + "  constant: "+Constants.PERMISSIONS_REQUEST_CAMERA);
        switch (requestCode) {
            case Constants.PERMISSIONS_REQUEST_CAMERA_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                //request 2 permisions make sure both are good
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    //Log.d(TAG, "onRequestPermissionsResult permision granted");
                    dispatchTakePictureIntent();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + Constants.PUBLIC_PHOTO_FOLDER+ File.separator);

        //if (Constants.DEBUG) Log.d(TAG, "createImageFile storageDir: "+ storageDir);

        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        //if (Constants.DEBUG) Log.d(TAG, "createImageFile image path: "+ mCurrentPhotoPath);
        return image;
    }

    private void dispatchTakePictureIntent() {
        //code from https://developer.android.com/training/camera/photobasics.html
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                if (Constants.DEBUG) Log.e(TAG, "dispatchTakePictureIntent ex: "+ ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    //Uri photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", photoFile);
                    //APL 12/11/17 changed to fully qualify build config
                    Uri photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fogoafileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    //if (Constants.DEBUG) Log.d(TAG, "Build >= N using file provider worked!");
                }
                else {
                    Uri photoURI = Uri.fromFile(photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    //if (Constants.DEBUG) Log.d(TAG, "Build < N using old method!");
                }
                startActivityForResult(takePictureIntent,  INTENT_RESULT_CAMERA);
            }
            else {
                if (Constants.DEBUG) Log.d(TAG, "dispatchTakePictureIntent - photoFile is null");
            }
            //startActivityForResult(cameraIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
        }
    }

    private OnLoadMore rvGallery_OnLoadMoreListener = new OnLoadMore() {
        @Override
        public void onLoadMore() {
            swlContainer.setRefreshing(true);
            //set the view page to the next page
            pageCurrent++;
            //get the next page of data
            adapterList.addAll(getCameraImages(getActivity(), pageMax, (pageMax*pageCurrent)));
            //rvGalleryAdapter.notifyDataSetChanged();
            //rvGalleryAdapter.notifyItemRangeInserted((pageMax*pageCurrent), pageMax);

            swlContainer.setRefreshing(false);
        }
    };

    public static ArrayList<GalleryItem> getCameraImages(Context context, int limit, int offset) {

        /*
        final String[] projection = { MediaStore.Images.Media.DATA };
        final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        final String[] selectionArgs = { CAMERA_IMAGE_BUCKET_ID };
        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);
        */

        final String[] projection = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME  };

        //final String selection = MediaStore.Images.Media.BUCKET_ID + " = ?";
        //final String[] selectionArgs = { CAMERA_IMAGE_BUCKET_ID };
        final String selection = null;
        final String[] selectionArgs = null;
        //final String orderBy = MediaStore.Images.Media.DEFAULT_SORT_ORDER;
        //final String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC LIMIT " +(limit)+ " OFFSET "+offset;
        //if (Constants.DEBUG) Log.d(TAG, "image query order by: " + orderBy);
        final Uri mediaQueryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
        //
        //}
        //if (Constants.DEBUG) Log.d(TAG, "image query mediaQueryUri: " + mediaQueryUri);
        //final Uri mediaQueryUri = MediaStore.Images.Media.INTERNAL_CONTENT_URI;
        final Cursor cursor = context.getContentResolver().query(mediaQueryUri,
                projection,
                selection,
                selectionArgs,
                orderBy);


        //ArrayList<GalleryItem> result = new ArrayList<GalleryItem>(cursor.getCount());
        ArrayList<GalleryItem> result = new ArrayList<GalleryItem>();
        int itemCnt = 0;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                final int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                final int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                final int bidColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
                final int bnameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                do {
                    String sdata = cursor.getString(dataColumn);
                    final String sid = cursor.getString(idColumn);
                    final String sname = cursor.getString(nameColumn);
                    final String sbid = cursor.getString(bidColumn);
                    final String sbname = cursor.getString(bnameColumn);
                    //if (Constants.DEBUG) Log.d(TAG, "sdata 1: "+sdata);
                    //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    //    sdata = "content://com.android.providers.media.documents" + sdata;
                    //}
                    //if (Constants.DEBUG) Log.d(TAG, "sdata 2: "+sdata);
                    final GalleryItem data = new GalleryItem(sdata, sid, sname, sbid, sbname);
                    //if (Constants.DEBUG) Log.d(TAG, data.toString());
                    result.add(data);
                    itemCnt++;
                    //} while (cursor.moveToNext() && itemCnt < pageMax);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        else {
            if (Constants.DEBUG) Log.d(TAG, "getCameraImages query cursor null");
        }

        /*
        final Cursor cursorIn = context.getContentResolver().query(MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DEFAULT_SORT_ORDER);
        if (cursorIn.moveToFirst()) {
            final int dataColumn = cursorIn.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do {
                final String sdata = cursorIn.getString(dataColumn);
                final GalleryItem data = new GalleryItem(sdata);
                result.add(data);
            } while (cursorIn.moveToNext());
        }
        cursorIn.close();
        */


        return result;
    }


}
