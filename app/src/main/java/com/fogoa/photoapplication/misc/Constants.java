package com.fogoa.photoapplication.misc;


import com.fogoa.photoapplication.BuildConfig;

public class Constants {
    public static final boolean DEBUG = true;
    public static final boolean DEV_BUILD = true;
    public static final boolean STAGE_BUILD = false;

    /* user level storage */
    public static final String USER_FILE = "USERINFO";

    /* working image file */
    public static final String IMAGE_WORKING_FILE = BuildConfig.APPLICATION_ID + ".workingimage";


    /* permission request */
    public static final int PERMISSIONS_REQUEST_CAMERA = 101;
    public static final int PERMISSIONS_REQUEST_CAMERA_EXTERNAL_STORAGE = 102;
    public static final int PERMISSIONS_REQUEST_READ_STORAGE = 103;


    /* activty intent parm keys */
    public static final String INTENT_KEY_GALLERY_SELECTMULTI = BuildConfig.APPLICATION_ID + ".gallery.selectmulti";
    public static final String INTENT_KEY_CROP_IMGPATHLIST = BuildConfig.APPLICATION_ID + ".crop.imagepathlist";
    public static final String INTENT_KEY_CROP_IMGPATH = BuildConfig.APPLICATION_ID + ".crop.imagepath";


    /* activty result ids */
    public static final int INTENT_RESULT_GALLERY = 101;
    public static final int INTENT_RESULT_CAMERA = 102;
    public static final int INTENT_RESULT_CROP = 103;

    public static final String PUBLIC_PHOTO_FOLDER = "fogoa";

}
