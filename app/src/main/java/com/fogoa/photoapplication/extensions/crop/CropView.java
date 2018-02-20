package com.fogoa.photoapplication.extensions.crop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
code from lyft scissors project:
 https://eng.lyft.com/scissors-an-image-cropping-library-for-android-a56369154a19
 https://github.com/lyft/scissors
 */

public class CropView extends AppCompatImageView {
    private static final String TAG = CropView.class.getSimpleName();
    private static final int MAX_TOUCH_POINTS = 2;
    private TouchManager touchManager;
    private CropViewConfig config;

    private Paint viewportPaint = new Paint();
    private Paint bitmapPaint = new Paint();

    private Bitmap bitmap;
    private Matrix transform = new Matrix();
    //private Extensions extensions;

    /** Corresponds to the values in {@link com.fogoa.photoapplication.R.attr#cropviewShape} */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ Shape.RECTANGLE, Shape.OVAL, Shape.FULL })
    public @interface Shape {
        int RECTANGLE = 0;
        int OVAL = 1;
        int FULL = 2;
    }

    private @Shape int shape = Shape.RECTANGLE;
    private Path ovalPath;
    private RectF ovalRect;

    public CropView(Context context) {
        super(context);
        initCropView(context, null);
    }

    public CropView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initCropView(context, attrs);
    }

    void initCropView(Context context, AttributeSet attrs) {
        config = CropViewConfig.from(context, attrs);

        touchManager = new TouchManager(MAX_TOUCH_POINTS, config);

        bitmapPaint.setFilterBitmap(true);
        setViewportOverlayColor(config.getViewportOverlayColor());
        shape = config.shape();

        // We need anti-aliased Paint to smooth the curved edges
        viewportPaint.setFlags(viewportPaint.getFlags() | Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (bitmap == null) {
            return;
        }

        drawBitmap(canvas);
        if (shape == Shape.RECTANGLE) {
            drawSquareOverlay(canvas);
        } else if (shape == Shape.OVAL) {
            drawOvalOverlay(canvas);
        }
    }

    private void drawBitmap(Canvas canvas) {
        transform.reset();
        touchManager.applyPositioningAndScale(transform);

        canvas.drawBitmap(bitmap, transform, bitmapPaint);
    }

    private void drawSquareOverlay(Canvas canvas) {
        final int viewportWidth = touchManager.getViewportWidth();
        final int viewportHeight = touchManager.getViewportHeight();
        final int left = (getWidth() - viewportWidth) / 2;
        final int top = (getHeight() - viewportHeight) / 2;

        canvas.drawRect(0, top, left, getHeight() - top, viewportPaint); // left
        canvas.drawRect(0, 0, getWidth(), top, viewportPaint); // top
        canvas.drawRect(getWidth() - left, top, getWidth(), getHeight() - top, viewportPaint); // right
        canvas.drawRect(0, getHeight() - top, getWidth(), getHeight(), viewportPaint); // bottom
    }

    private void drawOvalOverlay(Canvas canvas) {
        if (ovalRect == null) {
            ovalRect = new RectF();
        }
        if (ovalPath == null) {
            ovalPath = new Path();
        }

        final int viewportWidth = touchManager.getViewportWidth();
        final int viewportHeight = touchManager.getViewportHeight();
        final int left = (getWidth() - viewportWidth) / 2;
        final int top = (getHeight() - viewportHeight) / 2;
        final int right = getWidth() - left;
        final int bottom = getHeight() - top;
        ovalRect.left = left;
        ovalRect.top = top;
        ovalRect.right = right;
        ovalRect.bottom = bottom;

        // top left arc
        ovalPath.reset();
        ovalPath.moveTo(left, getHeight() / 2); // middle of the left side of the circle
        ovalPath.arcTo(ovalRect, 180, 90, false); // draw arc to top
        ovalPath.lineTo(left, top); // move to top-left corner
        ovalPath.lineTo(left, getHeight() / 2); // move back to origin
        ovalPath.close();
        canvas.drawPath(ovalPath, viewportPaint);

        // top right arc
        ovalPath.reset();
        ovalPath.moveTo(getWidth() / 2, top); // middle of the top side of the circle
        ovalPath.arcTo(ovalRect, 270, 90, false); // draw arc to the right
        ovalPath.lineTo(right, top); // move to top-right corner
        ovalPath.lineTo(getWidth() / 2, top); // move back to origin
        ovalPath.close();
        canvas.drawPath(ovalPath, viewportPaint);

        // bottom right arc
        ovalPath.reset();
        ovalPath.moveTo(right, getHeight() / 2); // middle of the right side of the circle
        ovalPath.arcTo(ovalRect, 0, 90, false); // draw arc to the bottom
        ovalPath.lineTo(right, bottom); // move to bottom-right corner
        ovalPath.lineTo(right, getHeight() / 2); // move back to origin
        ovalPath.close();
        canvas.drawPath(ovalPath, viewportPaint);

        // bottom left arc
        ovalPath.reset();
        ovalPath.moveTo(getWidth() / 2, bottom); // middle of the bottom side of the circle
        ovalPath.arcTo(ovalRect, 90, 90, false); // draw arc to the left
        ovalPath.lineTo(left, bottom); // move to bottom-left corner
        ovalPath.lineTo(getWidth() / 2, bottom); // move back to origin
        ovalPath.close();
        canvas.drawPath(ovalPath, viewportPaint);

        // Draw the square overlay as well
        drawSquareOverlay(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //APL - 10/5/17 - what if we don't do this on size chanege???
        resetTouchManager();
    }

    /**
     * Sets the color of the viewport overlay
     *
     * @param viewportOverlayColor The color to use for the viewport overlay
     */
    public void setViewportOverlayColor(@ColorInt int viewportOverlayColor) {
        viewportPaint.setColor(viewportOverlayColor);
        config.setViewportOverlayColor(viewportOverlayColor);
    }

    /**
     * Sets the padding for the viewport overlay
     *
     * @param viewportOverlayPadding The new padding of the viewport overlay
     */
    public void setViewportOverlayPadding(int viewportOverlayPadding) {
        config.setViewportOverlayPadding(viewportOverlayPadding);
        resetTouchManager();
        invalidate();
    }

    /**
     * Returns the native aspect ratio of the image.
     *
     * @return The native aspect ratio of the image.
     */
    public float getImageRatio() {
        Bitmap bitmap = getImageBitmap();
        return bitmap != null ? (float) bitmap.getWidth() / (float) bitmap.getHeight() : 0f;
    }

    /**
     * Returns the aspect ratio of the viewport and crop rect.
     *
     * @return The current viewport aspect ratio.
     */
    public float getViewportRatio() {
        return touchManager.getAspectRatio();
    }

    /**
     * Sets the aspect ratio of the viewport and crop rect.  Defaults to
     * the native aspect ratio if <code>ratio == 0</code>.
     *
     * @param ratio The new aspect ratio of the viewport.
     */
    public void setViewportRatio(float ratio) {
        if (Float.compare(ratio, 0) == 0) {
            ratio = getImageRatio();
        }
        touchManager.setAspectRatio(ratio);
        resetTouchManager();
        invalidate();
    }

    public void setCropShape(@CropView.Shape int shape) {
        this.shape = shape;
        config.setShape(shape);
        touchManager.setShape(shape);
        resetTouchManager();
        invalidate();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        final Bitmap bitmap = resId > 0
                ? BitmapFactory.decodeResource(getResources(), resId)
                : null;
        setImageBitmap(bitmap);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        final Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            bitmap = bitmapDrawable.getBitmap();
        } else if (drawable != null) {
            //bitmap = Utils.asBitmap(drawable, getWidth(), getHeight());
            final Rect tmpRect = new Rect();
            drawable.copyBounds(tmpRect);
            if (tmpRect.isEmpty()) {
                tmpRect.set(0, 0, Math.max(getWidth(), drawable.getIntrinsicWidth()), Math.max(getHeight(), drawable.getIntrinsicHeight()));
                drawable.setBounds(tmpRect);
            }
            bitmap = Bitmap.createBitmap(tmpRect.width(), tmpRect.height(), Bitmap.Config.ARGB_8888);
            drawable.draw(new Canvas(bitmap));
        } else {
            bitmap = null;
        }

        setImageBitmap(bitmap);
    }

    /*
    @Override
    public void setImageURI(@Nullable Uri uri) {
        extensions().load(uri);
    }
    */

    @Override
    public void setImageBitmap(@Nullable Bitmap bitmap) {
        this.bitmap = bitmap;
        resetTouchManager();
        invalidate();
    }

    /**
     * @return Current working Bitmap or <code>null</code> if none has been set yet.
     */
    @Nullable
    public Bitmap getImageBitmap() {
        return bitmap;
    }

    private void resetTouchManager() {
        final boolean invalidBitmap = bitmap == null;
        final int bitmapWidth = invalidBitmap ? 0 : bitmap.getWidth();
        final int bitmapHeight = invalidBitmap ? 0 : bitmap.getHeight();
        touchManager.resetFor(bitmapWidth, bitmapHeight, getWidth(), getHeight());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean result = super.dispatchTouchEvent(event);

        if(!isEnabled()) {
            return result;
        }

        touchManager.onEvent(event);
        invalidate();
        return true;
    }

    /**
     * Performs synchronous image cropping based on configuration.
     *
     * @return A {@link Bitmap} cropped based on viewport and user panning and zooming or <code>null</code> if no {@link Bitmap} has been
     * provided.
     */
    @Nullable
    public Bitmap crop() {
        if (bitmap == null) {
            return null;
        }

        final Bitmap src = bitmap;
        final Bitmap.Config srcConfig = src.getConfig();
        final Bitmap.Config config = srcConfig == null ? Bitmap.Config.ARGB_8888 : srcConfig;
        final int viewportHeight = touchManager.getViewportHeight();
        final int viewportWidth = touchManager.getViewportWidth();
        //final int viewportHeight = bitmap.getWidth();
        //final int viewportWidth = bitmap.getHeight();
        //if (Constants.DEBUG) {
        //    Log.d(TAG, "crop viewportHeight: "+viewportHeight);
        //    Log.d(TAG, "crop viewportWidth: "+viewportWidth);
        //}

        final Bitmap dst = Bitmap.createBitmap(viewportWidth, viewportHeight, config);

        Canvas canvas = new Canvas(dst);
        final int left = (getRight() - viewportWidth) / 2;
        final int top = (getBottom() - viewportHeight) / 2;
        canvas.translate(-left, -top);

        drawBitmap(canvas);

        return dst;
    }

    /**
     * Obtain current viewport width.
     *
     * @return Current viewport width.
     * <p>Note: It might be 0 if layout pass has not been completed.</p>
     */
    public int getViewportWidth() {
        return touchManager.getViewportWidth();
    }

    /**
     * Obtain current viewport height.
     *
     * @return Current viewport height.
     * <p>Note: It might be 0 if layout pass has not been completed.</p>
     */
    public int getViewportHeight() {
        return touchManager.getViewportHeight();
    }

    /**
     * Offers common utility extensions.
     *
     * @return Extensions object used to perform chained calls.
     */
    /*
    public Extensions extensions() {
        if (extensions == null) {
            extensions = new Extensions(this);
        }
        return extensions;
    }
    */

    /**
     * Get the transform matrix
     * @return
     */
    public Matrix getTransformMatrix() {
        return transform;
    }

}
