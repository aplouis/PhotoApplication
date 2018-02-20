package com.fogoa.photoapplication.misc;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.fogoa.photoapplication.BuildConfig;
import com.fogoa.photoapplication.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;


public class ImageDownloaderCache {
	private static final String TAG = ImageDownloaderCache.class.getSimpleName();

	public static int maxImageSize = 1080;

	private LruCache<String, Bitmap> imgCache;
	
	public static int maxTextureSize = 0;

	public static String cacheFileDir = BuildConfig.APPLICATION_ID + ".cache.";

    public ImageDownloaderCache() {
    	
    	final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
    	
    	final int cacheSize = maxMemory / 8;
    	
    	imgCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

    }
	
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (bitmap != null && key != null) {
			if (getBitmapFromMemCache(key) == null) {
				imgCache.put(key, bitmap);
			}
		}
    }
    
    public Bitmap getBitmapFromMemCache(String key) {
        return imgCache.get(key);
    }


	public void loadBitmapPath(String path, ImageView imageView) {
		//default the size to the screen width
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) imageView.getContext().getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);

		int imgSize = 100;
		imgSize = ((Float)metrics.xdpi).intValue();
		if (imgSize > maxImageSize) {
			//make the max size 640 since the server scales down to that size
			imgSize = maxImageSize;
		}
		loadBitmapPath(path, imageView, imgSize);
	}

	public void loadBitmapPath(String path, ImageView imageView, int imgSize) {
		//BitmapWorkerTask task = new BitmapWorkerTask(imageView);
		//task.execute(path);

		final Bitmap bitmap = getBitmapFromMemCache(path);
		if (bitmap != null) {
			imageView.setImageBitmap(bitmap);
		}
		else {
			if (cancelPotentialWork(path, imageView)) {
				//Bitmap mPlaceHolderBitmap = null;
				Bitmap mPlaceHolderBitmap = BitmapFactory.decodeResource(imageView.getContext().getResources(), R.drawable.ic_photo_black_48dp);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable = new AsyncDrawable(imageView.getContext().getResources(), mPlaceHolderBitmap, task);
				imageView.setImageDrawable(asyncDrawable);
				//added the screen widht as the image size
				task.execute(path, imgSize);
			}

		}

	}

	public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {
		return decodeSampledBitmapFromPath(path, reqWidth, reqHeight, true);
	}
	public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight, boolean bCropImage) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		//BitmapFactory.decodeResource(res, resId, options);
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		//return BitmapFactory.decodeResource(res, resId, options);
		//return BitmapFactory.decodeFile(path, options);

		Bitmap gBitmap = BitmapFactory.decodeFile(path, options);
        if (gBitmap!=null) {

            //rotate the bitmap
            //if (gBitmap.getHeight() < gBitmap.getWidth()) {
            //    Matrix matrix = new Matrix();
            //    matrix.preRotate(90);
            //    gBitmap = Bitmap.createBitmap(gBitmap, 0, 0, gBitmap.getWidth(), gBitmap.getHeight(), matrix, true);
            //}

            try {
                ExifInterface exif = new ExifInterface(path);
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int rotationInDegrees = exifToDegrees(rotation);
                //Log.d(TAG, "EXIF Orientation: " + Integer.toString(rotation));
                if (rotation != 0f) {
                //if (rotationInDegrees != 0f) {
                    Matrix matrix = new Matrix();
                    matrix.preRotate(rotationInDegrees);
                    gBitmap = Bitmap.createBitmap(gBitmap, 0, 0, gBitmap.getWidth(), gBitmap.getHeight(), matrix, true);
                    //Log.d(TAG, "rotated degrees: " + rotationInDegrees);
                }
            } catch (Exception e) {
                //Log.d("oops","decoding issue");
                //FirebaseCrash.logcat(Log.ERROR, TAG,  "Error with background image: "+e.getMessage());
                //FirebaseCrash.report(e);
            }

            if (bCropImage) {
                //crop the bitmap
                return cropBitmap(gBitmap);
            }
            else {
                //APL - 10/2/17 - check if image is bigger than 2048
                if (maxTextureSize == 0) {
                    maxTextureSize = getMaxTextureSize();
                }
                //if (Constants.DEBUG) {
                //    Log.d(TAG, "gBitmap Width: " + (gBitmap.getWidth()));
                //    Log.d(TAG, "gBitmap Height: " + (gBitmap.getHeight()));
                //    Log.d(TAG, " maxTextureSize: " + (maxTextureSize));
                //}
                if (gBitmap.getHeight() > maxTextureSize || gBitmap.getWidth() > maxTextureSize) {
                    int nh = maxTextureSize;
                    int nw = maxTextureSize;
                    if (gBitmap.getHeight() == gBitmap.getWidth()) {
                        nw = maxTextureSize;
                        nh = maxTextureSize;
                    } else if (gBitmap.getHeight() < gBitmap.getWidth()) {
                        nw = maxTextureSize;
                        nh = (int) (gBitmap.getHeight() * ((double) maxTextureSize / gBitmap.getWidth()));
                    } else {
                        nh = maxTextureSize;
                        nw = (int) (gBitmap.getWidth() * ((double) maxTextureSize / gBitmap.getHeight()));
                        //if (Constants.DEBUG) {
                        //    Log.d(TAG, "nw: " + (nw));
                        //    Log.d(TAG, "nh: " + (nh));
                        //}
                    }
                    //gBitmap = Bitmap.createScaledBitmap(gBitmap, nw, nh, true);
                    gBitmap = ThumbnailUtils.extractThumbnail(gBitmap, nw, nh, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                }

            }
		}
        return gBitmap;
	}

	public static int exifToDegrees(int exifOrientation) {
		if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
		else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
		else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
		return 0;
	}

	public static Bitmap cropBitmap(Bitmap bmImage) {
		if (bmImage != null) {
			int dimension =  Math.min(bmImage.getWidth(), bmImage.getHeight());
			//APL 10/2/17 - don't allow size greater than 2048 becuase it can cause error - Bitmap too large to be uploaded into a texture (1944x2592, max=2048x2048)
            if (maxTextureSize==0) {
                maxTextureSize = getMaxTextureSize();
            }
            //if (Constants.DEBUG) {
            //    Log.d(TAG, "bmImage Width: " + (bmImage.getWidth()));
            //    Log.d(TAG, "bmImage Height: " + (bmImage.getHeight()));
            //    Log.d(TAG, " maxTextureSize: " + (maxTextureSize));
            //}
			if (dimension > maxTextureSize) {
				dimension = maxTextureSize;
			}

			return ThumbnailUtils.extractThumbnail(bmImage, dimension, dimension, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
		}
		else {
			return null;
		}

	}

	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) >= reqHeight
					&& (halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	class BitmapWorkerTask extends AsyncTask<Object, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private String data = "";

		public BitmapWorkerTask(ImageView imageView) {
			// Use a WeakReference to ensure the ImageView can be garbage collected
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		// Decode image in background.
		@Override
		protected Bitmap doInBackground(Object... params) {
			data = (String) params[0];

			//Integer imgSize = 100;
			//int imgSize = 100;
			int imgSize = maxImageSize;
			if (params.length > 1 && params[1] instanceof Integer) {
				//imgSize = ((Float)params[1]).intValue();
				imgSize = (Integer) params[1];
			}

			//return decodeSampledBitmapFromResource(getResources(), data, 100, 100));
			//return decodeSampledBitmapFromPath(data, 100, 100);

			//final Bitmap bitmap = decodeSampledBitmapFromPath(data, 100, 100);
			//updated to use the screen width passed in
			//if (Constants.DEBUG) Log.d(TAG, "image size: "+ imgSize);
			final Bitmap bitmap = decodeSampledBitmapFromPath(data, imgSize, imgSize);

			addBitmapToMemoryCache(data, bitmap);
			return bitmap;
		}

		// Once complete, see if ImageView is still around and set bitmap.
		@Override
		protected void onPostExecute(Bitmap bitmap) {
            /*
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
            */

			if (isCancelled()) {
				bitmap = null;
			}

			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
				if (this == bitmapWorkerTask && imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}

		}
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	public static boolean cancelPotentialWork(String data, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final String bitmapData = bitmapWorkerTask.data;
			// If bitmapData is not yet set or it differs from the new data
			if (bitmapData == null || bitmapData.isEmpty() || bitmapData != data) {
				// Cancel previous task
				bitmapWorkerTask.cancel(true);
			} else {
				// The same work is already in progress
				return false;
			}
		}
		// No task associated with the ImageView, or an existing task was cancelled
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	public void saveImageToFile(final AppCompatActivity activity, final String filePath, final String strUrl) {

		final Handler imgHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {

				//Log.d(this.getClass().getSimpleName(), "img downloaded: "+imgURL+"  imageView"+ ivImageView.toString());

				if(message != null && message.obj != null) {
					//String workingImagePath = (String) message.obj;
					Bundle objMsg = (Bundle) message.obj;
					String workingImagePath = objMsg.getString("workingImagePath");
					if (workingImagePath != null && !workingImagePath.isEmpty()) {
						Intent data = new Intent();
						data.putExtra("image_path", workingImagePath);
						long image_date = objMsg.getLong("workingImageDate", 0);
						//if (image_date > 0) {
							data.putExtra("image_date", image_date);
						//}
						activity.setResult(activity.RESULT_OK, data);
						activity.finish();
					}

				}
			}
		};

		//thread to download the image
		Thread thread = new Thread() {
			@Override
			public void run() {
				String workingImagePath = null;
				long dt = 0;
				try {
					URL url = new URL(strUrl);
					HttpURLConnection c = (HttpURLConnection) url.openConnection();
					c.connect();
					//InputStream input = url.openStream();
					InputStream input = c.getInputStream();
					dt=c.getLastModified();
					try {
						byte[] buffer = new byte[1500];
						FileOutputStream output = activity.openFileOutput(filePath, Context.MODE_PRIVATE);
						try {
							int bytesRead = 0;
							while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0)
							{
								output.write(buffer, 0, bytesRead);
							}
						} finally {
							output.close();
						}
					} finally {
						input.close();
						c.disconnect();
					}
					//get the file
					File workingImageFile = activity.getFileStreamPath(filePath);
					if (workingImageFile != null) {
						workingImagePath = workingImageFile.getAbsolutePath();
					}

				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
				Bundle objMsg = new Bundle();
				objMsg.putString("workingImagePath", workingImagePath);
				objMsg.putLong("workingImageDate", dt);
				//Message message = imgHandler.obtainMessage(1, workingImagePath);
				Message message = imgHandler.obtainMessage(1, objMsg);
				imgHandler.sendMessage(message);
			}
		};
		thread.start();

    }


    public static int getMaxTextureSize() {
        // Safe minimum default size
        final int IMAGE_MAX_BITMAP_DIMENSION = 2048;

        // Get EGL Display
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Initialise
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        // Query total number of configurations
        int[] totalConfigurations = new int[1];
        egl.eglGetConfigs(display, null, 0, totalConfigurations);

        // Query actual list configurations
        EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

        int[] textureSize = new int[1];
        int maximumTextureSize = 0;

        // Iterate through all the configurations to located the maximum texture size
        for (int i = 0; i < totalConfigurations[0]; i++) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0])
                maximumTextureSize = textureSize[0];
        }

        // Release
        egl.eglTerminate(display);

        // Return largest texture size found, or default
        return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
    }

}
