package com.fogoa.photoapplication.extensions.crop;

import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.MotionEvent;


import java.util.Arrays;

/**
 code from lyft scissors project:
 https://eng.lyft.com/scissors-an-image-cropping-library-for-android-a56369154a19
 https://github.com/lyft/scissors
 */

public class TouchManager {
    private static final String TAG = TouchManager.class.getSimpleName();
    private final int maxNumberOfTouchPoints;
    private final CropViewConfig cropViewConfig;

    private final TouchPoint[] points;
    private final TouchPoint[] previousPoints;

    private float minimumScale;
    private float maximumScale;
    private Rect imageBounds;
    private float aspectRatio;
    private int viewportWidth;
    private int viewportHeight;
    private int bitmapWidth;
    private int bitmapHeight;

    private int verticalLimit;
    private int horizontalLimit;

    private float scale = -1.0f;
    private TouchPoint position = new TouchPoint();

    private RotationGestureDetector mRotateDetector;
    private float mAngle = 0;
    private float mMidPntX, mMidPntY;
    private final Matrix mTempMatrix = new Matrix();
    private final RectF mCropRect = new RectF();
    private static final int RECT_CORNER_POINTS_COORDS = 8;
    private static final int RECT_CENTER_POINT_COORDS = 2;
    protected float[] mCurrentImageCorners = new float[RECT_CORNER_POINTS_COORDS];
    protected float[] mCurrentImageCenter = new float[RECT_CENTER_POINT_COORDS];
    private float[] mInitialImageCorners;
    private float[] mInitialImageCenter;
    private float mMaxScaleMultiplier = 10.0f;

    private class RotateListener extends RotationGestureDetector.SimpleOnRotationGestureListener {

        @Override
        public boolean onRotation(RotationGestureDetector rotationDetector) {
            //postRotate(rotationDetector.getAngle(), mMidPntX, mMidPntY);
            mAngle += rotationDetector.getAngle();
            return true;
        }

    }


    public TouchManager(final int maxNumberOfTouchPoints, final CropViewConfig cropViewConfig) {
        this.maxNumberOfTouchPoints = maxNumberOfTouchPoints;
        this.cropViewConfig = cropViewConfig;

        points = new TouchPoint[maxNumberOfTouchPoints];
        previousPoints = new TouchPoint[maxNumberOfTouchPoints];
        minimumScale = cropViewConfig.getMinScale();
        maximumScale = cropViewConfig.getMaxScale();

        mRotateDetector = new RotationGestureDetector(new RotateListener());
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    public void onEvent(MotionEvent event) {
        int index = event.getActionIndex();
        if (index >= maxNumberOfTouchPoints) {
            return; // We don't care about this pointer, ignore it.
        }

        if (event.getPointerCount() > 1) {
            mMidPntX = (event.getX(0) + event.getX(1)) / 2;
            mMidPntY = (event.getY(0) + event.getY(1)) / 2;
        }

        if (isUpAction(event.getActionMasked())) {
            previousPoints[index] = null;
            points[index] = null;
        } else {
            updateCurrentAndPreviousPoints(event);
        }

        handleDragGesture();

        mRotateDetector.onTouchEvent(event);

        handlePinchGesture();

        if (isUpAction(event.getActionMasked())) {
            ensureInsideViewport();
        }
    }

    public void applyPositioningAndScale(Matrix matrix) {
        matrix.postTranslate(-bitmapWidth / 2.0f, -bitmapHeight / 2.0f);
        matrix.postScale(scale, scale);
        if (Float.compare(0f,mAngle) != 0) {
            //if (Constants.DEBUG) Log.d(TAG, "post rotate angle: "+mAngle);
            matrix.postRotate(mAngle);
        }
        //matrix.postRotate(90, mMidPntX, mMidPntY);
        matrix.postTranslate(position.getX(), position.getY());

        // APL 7/31/17 - updateCurrentImagePoints
        matrix.mapPoints(mCurrentImageCorners, mInitialImageCorners);
        matrix.mapPoints(mCurrentImageCenter, mInitialImageCenter);
    }

    public void resetFor(int bitmapWidth, int bitmapHeight, int availableWidth, int availableHeight) {
        aspectRatio = cropViewConfig.getViewportRatio();
        imageBounds = new Rect(0, 0, availableWidth / 2, availableHeight / 2);
        mAngle = 0.0f;
        scale = 0.0f;
        //APL - 10/5/17 - unsure if we want to maintain the scale if it has changed
        //if (scale == -1.0f) {
        //    scale = cropViewConfig.getMinScale();
        //}

        //mCropRect.set(0, 0, availableWidth / 2, availableHeight / 2);
        //mCropRect.set(0, 0, availableWidth , availableHeight );
        //RectF initialImageRect = new RectF(0, 0, bitmapWidth, bitmapHeight);
        //mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        //mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect);

        //if (Constants.DEBUG) {
        //    Log.d(TAG, "availableWidth: "+availableWidth);
        //    Log.d(TAG, "availableHeight: "+availableHeight);
        //    Log.d(TAG, "bitmapWidth: "+bitmapWidth);
        //    Log.d(TAG, "bitmapHeight: "+bitmapHeight);
        //    Log.d(TAG, "resetFor imageBounds: "+imageBounds.toString());
            //Log.d(TAG, "resetFor mCropRect: "+mCropRect.toString());
        //}


        setViewport(bitmapWidth, bitmapHeight, availableWidth, availableHeight);

        this.bitmapWidth = bitmapWidth;
        this.bitmapHeight = bitmapHeight;
        if (bitmapWidth > 0 && bitmapHeight > 0) {
            setMinimumScale();
            setLimits();
            resetPosition();
            ensureInsideViewport();
            //insure scale is not 0
            scale = Math.max(scale, minimumScale);
        }
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float ratio) {
        aspectRatio = ratio;
        cropViewConfig.setViewportRatio(ratio);
    }

    public void setShape(@CropView.Shape int shape) {
        cropViewConfig.setShape(shape);
    }

    private void handleDragGesture() {
        if (getDownCount() != 1) {
            return;
        }
        position.add(moveDelta(0));
    }

    private void handlePinchGesture() {
        if (getDownCount() != 2) {
            return;
        }
        updateScale();
        setLimits();
    }

    private void ensureInsideViewport() {
        if (imageBounds == null) {
            return;
        }

        //if (Constants.DEBUG) Log.d(TAG, "ensureInsideViewport mAngle: "+mAngle);

        /*
        float newY = position.getY();
        int bottom = imageBounds.bottom;
        float newX = position.getX();
        int right = imageBounds.right;
        if (Constants.DEBUG) {
            Log.d(TAG, "ensureInsideViewport position.getX(): "+position.getX());
            Log.d(TAG, "ensureInsideViewport position.getY(): "+position.getY());
            Log.d(TAG, "ensureInsideViewport imageBounds.bottom: "+imageBounds.bottom);
            Log.d(TAG, "ensureInsideViewport imageBounds.right: "+imageBounds.right);
        }

        float wAngle = Math.abs(mAngle);
        if (wAngle > 360) {
            wAngle -= 360;
        }
        if (Constants.DEBUG) Log.d(TAG, "ensureInsideViewport wAngle: "+wAngle);
        if (wAngle>=90 && wAngle<180 || wAngle>=270 && wAngle<360) {
            if (bottom - newY >= horizontalLimit) {
                newY = bottom - horizontalLimit;
            } else if (newY - bottom >= horizontalLimit) {
                newY = bottom + horizontalLimit;
            }

            if (newX <= right - verticalLimit) {
                newX = right - verticalLimit;
            } else if (newX > right + verticalLimit) {
                newX = right + verticalLimit;
            }
        }
        else {

            if (bottom - newY >= verticalLimit) {
                newY = bottom - verticalLimit;
            } else if (newY - bottom >= verticalLimit) {
                newY = bottom + verticalLimit;
            }

            if (newX <= right - horizontalLimit) {
                newX = right - horizontalLimit;
            } else if (newX > right + horizontalLimit) {
                newX = right + horizontalLimit;
            }

        }

        if (Constants.DEBUG) {
            Log.d(TAG, "ensureInsideViewport horizontalLimit: "+horizontalLimit);
            Log.d(TAG, "ensureInsideViewport verticalLimit: "+verticalLimit);
            Log.d(TAG, "ensureInsideViewport newX: "+newX);
            Log.d(TAG, "ensureInsideViewport newY: "+newY);
            Log.d(TAG, "ensureInsideViewport mCurrentImageCorners: "+mCurrentImageCorners.toString());
        }

        position.set(newX, newY);
         */

        //APL - 7/31/17 - check rotated image scale
        //if (Float.compare(0f,mAngle) != 0 && !isImageWrapCropBounds(mCurrentImageCorners) ) {
        if (!isImageWrapCropBounds(mCurrentImageCorners) ) {
        //if (Float.compare(0f,mAngle) != 0 ) {
            setImageToWrapCropBounds();
        }
        else {

            /*
            float newY = position.getY();
            int bottom = imageBounds.bottom;


            if (bottom - newY >= verticalLimit) {
                newY = bottom - verticalLimit;
            } else if (newY - bottom >= verticalLimit) {
                newY = bottom + verticalLimit;
            }

            float newX = position.getX();
            int right = imageBounds.right;
            if (newX <= right - horizontalLimit) {
                newX = right - horizontalLimit;
            } else if (newX > right + horizontalLimit) {
                newX = right + horizontalLimit;
            }


            position.set(newX, newY);

            */
        }

        /*
        //APL - 7/31/17 - check rotated image scale
        if (Float.compare(0f,mAngle) != 0 ) {
            if (!isImageWrapCropBounds(mCurrentImageCorners)) {
        //        setImageToWrapCropBounds();

                float currentX = mCurrentImageCenter[0];
                float currentY = mCurrentImageCenter[1];
                //float currentX = newX;
                //float currentY = newY;
                float currentScale = scale;

                float deltaX = mCropRect.centerX() - currentX;
                float deltaY = mCropRect.centerY() - currentY;
                float deltaScale = 0;

                mTempMatrix.reset();
                mTempMatrix.setTranslate(deltaX, deltaY);

                float[] tempCurrentImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
                mTempMatrix.mapPoints(tempCurrentImageCorners);

                boolean willImageWrapCropBoundsAfterTranslate = isImageWrapCropBounds(tempCurrentImageCorners);

                if (willImageWrapCropBoundsAfterTranslate) {
                    final float[] imageIndents = calculateImageIndents();
                    deltaX = -(imageIndents[0] + imageIndents[2]);
                    deltaY = -(imageIndents[1] + imageIndents[3]);

                    mTempMatrix.reset();
                    mTempMatrix.setTranslate(deltaX, deltaY);

                    tempCurrentImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
                    mTempMatrix.mapPoints(tempCurrentImageCorners);

                    willImageWrapCropBoundsAfterTranslate = isImageWrapCropBounds(tempCurrentImageCorners);
                }

                if (!willImageWrapCropBoundsAfterTranslate) {
                    RectF tempCropRect = new RectF(mCropRect);
                    mTempMatrix.reset();
                    mTempMatrix.setRotate(mAngle);
                    mTempMatrix.mapRect(tempCropRect);

                    final float[] currentImageSides = RectUtils.getRectSidesFromCorners(mCurrentImageCorners);

                    deltaScale = Math.max(tempCropRect.width() / currentImageSides[0],
                            tempCropRect.height() / currentImageSides[1]);
                    deltaScale = deltaScale * currentScale - currentScale;
                }

                float newScale = scale + deltaScale;
                newScale = newScale < minimumScale ? minimumScale : newScale;
                newScale = newScale > maximumScale ? maximumScale : newScale;

                scale = newScale;

            }

        }
        */
    }

    private void updateCurrentAndPreviousPoints(MotionEvent event) {
        for (int i = 0; i < maxNumberOfTouchPoints; i++) {
            if (i < event.getPointerCount()) {
                final float eventX = event.getX(i);
                final float eventY = event.getY(i);

                if (points[i] == null) {
                    points[i] = new TouchPoint(eventX, eventY);
                    previousPoints[i] = null;
                } else {
                    if (previousPoints[i] == null) {
                        previousPoints[i] = new TouchPoint();
                    }
                    previousPoints[i].copy(points[i]);
                    points[i].set(eventX, eventY);
                }
            } else {
                previousPoints[i] = null;
                points[i] = null;
            }
        }
    }

    private void setViewport(int bitmapWidth, int bitmapHeight, int availableWidth, int availableHeight) {
        final float imageAspect = (float) bitmapWidth / bitmapHeight;
        final float viewAspect = (float) availableWidth / availableHeight;

        float ratio = cropViewConfig.getViewportRatio();
        if (Float.compare(0f, ratio) == 0) {
            // viewport ratio of 0 means match native ratio of bitmap
            ratio = imageAspect;
        }

        int left = 0;
        int top = 0;
        if (cropViewConfig.shape()==CropView.Shape.FULL) {
            //the view port should be the same size as imageview
            viewportWidth = availableWidth;
            viewportHeight = availableHeight;
        }
        else if (ratio > viewAspect) {
            // viewport is wider than view
            viewportWidth = availableWidth - cropViewConfig.getViewportOverlayPadding() * 2;
            viewportHeight = (int) (viewportWidth * (1 / ratio));
            left = (availableWidth - viewportWidth) / 2;
            top = (availableHeight - viewportHeight) / 2;
        } else {
            // viewport is taller than view
            viewportHeight = availableHeight - cropViewConfig.getViewportOverlayPadding() * 2;
            viewportWidth = (int) (viewportHeight * ratio);
            left = (availableWidth - viewportWidth) / 2;
            top = (availableHeight - viewportHeight) / 2;
        }

        mCropRect.set(left, top, viewportWidth+left, viewportHeight+top);
        //RectF initialImageRect = new RectF(0, 0, availableWidth / 2, availableHeight / 2);
        RectF initialImageRect = new RectF(0, 0, bitmapWidth, bitmapHeight);
        mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect);
        mCurrentImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        mCurrentImageCenter = RectUtils.getCenterFromRect(initialImageRect);

        /*
        if (Constants.DEBUG) {
            Log.d(TAG, "setViewport viewportWidth: "+viewportWidth);
            Log.d(TAG, "setViewport viewportHeight: "+viewportHeight);
            Log.d(TAG, "setViewport left: "+left);
            Log.d(TAG, "setViewport top: "+top);
            Log.d(TAG, "setViewport mCropRect: "+mCropRect.toString());
        }
        */

        /*
        final int left = (availableWidth - viewportWidth) / 2;
        final int top = (availableWidth - viewportHeight) / 2;

        mCropRect.set(left, top, viewportWidth / 2, viewportHeight / 2);
        RectF initialImageRect = new RectF(0, 0, bitmapWidth, bitmapHeight);
        mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect);

        final int left = (availableWidth - viewportWidth) / 2;
        final int top = (availableWidth - viewportHeight) / 2;
        final int right = availableWidth - left;
        final int bottom = availableHeight - top;
        final int leftI = (bitmapWidth - viewportWidth) / 2;
        final int topI = (bitmapHeight - viewportHeight) / 2;

        mCropRect.set(left, top, viewportWidth, viewportHeight);
        RectF initialImageRect = new RectF(leftI, topI, bitmapWidth, bitmapHeight);
        mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect);
        */

    }

    private void setLimits() {
        horizontalLimit = computeLimit((int) (bitmapWidth * scale), viewportWidth);
        verticalLimit = computeLimit((int) (bitmapHeight * scale), viewportHeight);
    }

    private void resetPosition() {
        position.set(imageBounds.right, imageBounds.bottom);
    }

    private void setMinimumScale() {
        final float fw = (float) viewportWidth / bitmapWidth;
        final float fh = (float) viewportHeight / bitmapHeight;
        minimumScale = Math.max(fw, fh);
        //if (Constants.DEBUG) {
        //    Log.d("TAG", "setMinimumScale old minimumScale: "+minimumScale);
        //    Log.d("TAG", "setMinimumScale old maximumScale: "+maximumScale);
        //}

        //APL - 8/3/17 trying this method for calculation the min and max scale
        /* this did not work better
        float widthScale = Math.min(mCropRect.width() / bitmapWidth, mCropRect.width() / bitmapHeight);
        float heightScale = Math.min(mCropRect.height() / bitmapHeight, mCropRect.height() / bitmapWidth);

        minimumScale = Math.min(widthScale, heightScale);
        maximumScale = minimumScale * mMaxScaleMultiplier;

        if (Constants.DEBUG) {
            Log.d("TAG", "setMinimumScale new minimumScale: "+minimumScale);
            Log.d("TAG", "setMinimumScale new maximumScale: "+maximumScale);
        }
        */

    }

    private void updateScale() {
        TouchPoint current = vector(points[0], points[1]);
        TouchPoint previous = previousVector(0, 1);
        float currentDistance = current.getLength();
        float previousDistance = previous.getLength();

        float newScale = scale;
        if (previousDistance != 0) {
            newScale *= currentDistance / previousDistance;
        }
        newScale = newScale < minimumScale ? minimumScale : newScale;
        newScale = newScale > maximumScale ? maximumScale : newScale;

        scale = newScale;
    }

    private boolean isPressed(int index) {
        return points[index] != null;
    }

    private int getDownCount() {
        int count = 0;
        for (TouchPoint point : points) {
            if (point != null) {
                count++;
            }
        }
        return count;
    }

    private TouchPoint moveDelta(int index) {
        if (isPressed(index)) {
            TouchPoint previous =
                    previousPoints[index] != null ? previousPoints[index] : points[index];
            return TouchPoint.subtract(points[index], previous);
        } else {
            return new TouchPoint();
        }
    }

    private TouchPoint previousVector(int indexA, int indexB) {
        return previousPoints[indexA] == null || previousPoints[indexB] == null
                ? vector(points[indexA], points[indexB])
                : vector(previousPoints[indexA], previousPoints[indexB]);
    }

    private static int computeLimit(int bitmapSize, int viewportSize) {
        return (bitmapSize - viewportSize) / 2;
    }

    private static TouchPoint vector(TouchPoint a, TouchPoint b) {
        return TouchPoint.subtract(b, a);
    }

    private static boolean isUpAction(int actionMasked) {
        return actionMasked == MotionEvent.ACTION_POINTER_UP || actionMasked == MotionEvent.ACTION_UP;
    }

    /***
     * from https://github.com/Yalantis/uCrop/blob/master/ucrop/src/main/java/com/yalantis/ucrop/view/CropImageView.java
     * using to scale photo when rotated
     */

    /**
     * This methods checks whether a rectangle that is represented as 4 corner points (8 floats)
     * fills the crop bounds rectangle.
     *
     * @param imageCorners - corners of a rectangle
     * @return - true if it wraps crop bounds, false - otherwise
     */
    protected boolean isImageWrapCropBounds(float[] imageCorners) {
        mTempMatrix.reset();
        mTempMatrix.setRotate(-mAngle);

        float[] unrotatedImageCorners = Arrays.copyOf(imageCorners, imageCorners.length);
        mTempMatrix.mapPoints(unrotatedImageCorners);

        //if (Constants.DEBUG) {
        //    Log.d(TAG, "isImageWrapCropBounds mCropRect.top: "+mCropRect.top);
        //    Log.d(TAG, "isImageWrapCropBounds mCropRect.left: "+mCropRect.left);
        //    Log.d(TAG, "isImageWrapCropBounds mCropRect.bottom: "+mCropRect.bottom);
        //    Log.d(TAG, "isImageWrapCropBounds mCropRect.right: "+mCropRect.right);
        //}


        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRect);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);

        return RectUtils.trapToRect(unrotatedImageCorners).contains(RectUtils.trapToRect(unrotatedCropBoundsCorners));
    }
    /**
     * If image doesn't fill the crop bounds it must be translated and scaled properly to fill those.
     * <p/>
     * Therefore this method calculates delta X, Y and scale values and passes them to the
     * {which animates image.
     * Scale value must be calculated only if image won't fill the crop bounds after it's translated to the
     * crop bounds rectangle center. Using temporary variables this method checks this case.
     */
    public void setImageToWrapCropBounds() {

        //float newY = position.getY();
        //float newX = position.getX();

        float currentX = mCurrentImageCenter[0];
        float currentY = mCurrentImageCenter[1];
        float currentScale = scale;

        float deltaX = mCropRect.centerX() - currentX;
        float deltaY = mCropRect.centerY() - currentY;
        float deltaScale = 0;

        /*
        if (Constants.DEBUG) {
            Log.d(TAG, "setImageToWrapCropBounds mCropRect.centerX(): "+mCropRect.centerX());
            Log.d(TAG, "setImageToWrapCropBounds mCropRect.centerY(): "+mCropRect.centerY());
            Log.d(TAG, "setImageToWrapCropBounds currentX: "+currentX);
            Log.d(TAG, "setImageToWrapCropBounds currentY: "+currentY);
            Log.d(TAG, "setImageToWrapCropBounds deltaX: "+deltaX);
            Log.d(TAG, "setImageToWrapCropBounds deltaY: "+deltaY);
            Log.d(TAG, "setImageToWrapCropBounds mCropRect.top: "+mCropRect.top);
            Log.d(TAG, "setImageToWrapCropBounds mCropRect.left: "+mCropRect.left);
            Log.d(TAG, "setImageToWrapCropBounds mCropRect.bottom: "+mCropRect.bottom);
            Log.d(TAG, "setImageToWrapCropBounds mCropRect.right: "+mCropRect.right);
            Log.d(TAG, "setImageToWrapCropBounds mCurrentImageCorners: "+mCurrentImageCorners.toString());
        }
        */

        mTempMatrix.reset();
        mTempMatrix.setTranslate(deltaX, deltaY);

        float[] tempCurrentImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
        mTempMatrix.mapPoints(tempCurrentImageCorners);

        boolean willImageWrapCropBoundsAfterTranslate = isImageWrapCropBounds(tempCurrentImageCorners);

        if (willImageWrapCropBoundsAfterTranslate) {
            final float[] imageIndents = calculateImageIndents();
            deltaX = -(imageIndents[0] + imageIndents[2]);
            deltaY = -(imageIndents[1] + imageIndents[3]);

            /*
            if (Constants.DEBUG) {
                Log.d(TAG, "setImageToWrapCropBounds imageIndents[0]: "+imageIndents[0]);
                Log.d(TAG, "setImageToWrapCropBounds imageIndents[1]: "+imageIndents[1]);
                Log.d(TAG, "setImageToWrapCropBounds imageIndents[2]: "+imageIndents[2]);
                Log.d(TAG, "setImageToWrapCropBounds imageIndents[3]: "+imageIndents[3]);
                Log.d(TAG, "setImageToWrapCropBounds no scale needed deltaX: "+deltaX);
                Log.d(TAG, "setImageToWrapCropBounds no scale needed deltaY: "+deltaY);
            }
            */

            //position.set(deltaX, deltaY);
            float newX = currentX+deltaX;
            float newY = currentY+deltaY;
            //if (Constants.DEBUG) {
            //    Log.d(TAG, "setImageToWrapCropBounds no scale needed newX: "+newX);
            //    Log.d(TAG, "setImageToWrapCropBounds no scale needed newY: "+newY);
            //}
            position.set(newX, newY);


            //mTempMatrix.reset();
            //mTempMatrix.setTranslate(deltaX, deltaY);

            //tempCurrentImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
            //mTempMatrix.mapPoints(tempCurrentImageCorners);

            //willImageWrapCropBoundsAfterTranslate = isImageWrapCropBounds(tempCurrentImageCorners);
        }
        else {
            RectF tempCropRect = new RectF(mCropRect);
            mTempMatrix.reset();
            mTempMatrix.setRotate(mAngle);
            mTempMatrix.mapRect(tempCropRect);

            final float[] currentImageSides = RectUtils.getRectSidesFromCorners(mCurrentImageCorners);

            //if (Constants.DEBUG) {
            //    Log.d(TAG, "setImageToWrapCropBounds scale "+scale);
            //    Log.d(TAG, "setImageToWrapCropBounds currentScale: "+currentScale);
            //    Log.d(TAG, "setImageToWrapCropBounds deltaScale 1: "+deltaScale);
            //}
            deltaScale = Math.max(tempCropRect.width() / currentImageSides[0], tempCropRect.height() / currentImageSides[1]);
            //if (Constants.DEBUG) {
            //    Log.d(TAG, "setImageToWrapCropBounds deltaScale 2: "+deltaScale);
            //}
            deltaScale = deltaScale * currentScale - currentScale;
            //if (Constants.DEBUG) {
            //    Log.d(TAG, "setImageToWrapCropBounds deltaScale 3: "+deltaScale);
            //}


        }

        //position.set(deltaX, deltaY);
        //position.set(deltaX-(mCurrentImageCenter[0]-currentX), deltaY - (mCurrentImageCenter[1] - currentY));
        //float newX = currentX+deltaX;
        //float newY = currentY+deltaY;
        //if (Constants.DEBUG) {
        //    Log.d(TAG, "setImageToWrapCropBounds no scale needed newX: "+newX);
        //    Log.d(TAG, "setImageToWrapCropBounds no scale needed newY: "+newY);
        //}
        //position.set(newX, newY);

        //cropImageView.postTranslate(newX - (cropImageView.mCurrentImageCenter[0] - mOldX), newY - (cropImageView.mCurrentImageCenter[1] - mOldY));

        if (!willImageWrapCropBoundsAfterTranslate) {
            //float newScale = (currentScale + deltaScale) / scale;
            float newScale = (currentScale + deltaScale);
            //if (Constants.DEBUG) {
            //    Log.d(TAG, "setImageToWrapCropBounds newScale 1: "+newScale);
            //}
            newScale = newScale < minimumScale ? minimumScale : newScale;
            newScale = newScale > maximumScale ? maximumScale : newScale;
            //if (Constants.DEBUG) {
            //    Log.d(TAG, "setImageToWrapCropBounds newScale 2: "+newScale);
            //}

            scale = newScale;
            position.set(mCropRect.centerX(), mCropRect.centerY());
        }

    }

    /**
     * First, un-rotate image and crop rectangles (make image rectangle axis-aligned).
     * Second, calculate deltas between those rectangles sides.
     * Third, depending on delta (its sign) put them or zero inside an array.
     * Fourth, using Matrix, rotate back those points (indents).
     *
     * @return - the float array of image indents (4 floats) - in this order [left, top, right, bottom]
     */
    private float[] calculateImageIndents() {
        mTempMatrix.reset();
        mTempMatrix.setRotate(-mAngle);

        float[] unrotatedImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRect);

        mTempMatrix.mapPoints(unrotatedImageCorners);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);

        RectF unrotatedImageRect = RectUtils.trapToRect(unrotatedImageCorners);
        RectF unrotatedCropRect = RectUtils.trapToRect(unrotatedCropBoundsCorners);

        float deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left;
        float deltaTop = unrotatedImageRect.top - unrotatedCropRect.top;
        float deltaRight = unrotatedImageRect.right - unrotatedCropRect.right;
        float deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom;

        float indents[] = new float[4];
        indents[0] = (deltaLeft > 0) ? deltaLeft : 0;
        indents[1] = (deltaTop > 0) ? deltaTop : 0;
        indents[2] = (deltaRight < 0) ? deltaRight : 0;
        indents[3] = (deltaBottom < 0) ? deltaBottom : 0;

        mTempMatrix.reset();
        mTempMatrix.setRotate(mAngle);
        mTempMatrix.mapPoints(indents);

        return indents;
    }

}
