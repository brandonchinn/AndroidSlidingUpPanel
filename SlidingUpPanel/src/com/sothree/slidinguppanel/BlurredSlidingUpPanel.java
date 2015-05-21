package com.sothree.slidinguppanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class BlurredSlidingUpPanel extends SlidingUpPanelLayout implements View.OnLayoutChangeListener {
    private static final String TAG = BlurredSlidingUpPanel.class.getSimpleName();
    private Context context;
    private Bitmap blurredBmp;
    private BitmapDrawable bmDrawable;

    public BlurredSlidingUpPanel(Context context) {
        super(context);
        this.context = context;
    }

    public BlurredSlidingUpPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public BlurredSlidingUpPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            mMainView.addOnLayoutChangeListener(this);
        } catch (Exception e) {
            Log.e(TAG, "Error removing layout listener");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mMainView != null) {
            try {
                mMainView.removeOnLayoutChangeListener(this);
            } catch (Exception e) {
                Log.e(TAG, "Error removing layout listener");
            }

        }
        super.onDetachedFromWindow();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (mSlideableView == child && bmDrawable != null) {
            bmDrawable.draw(canvas);
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private void updateBackgroundImage() {
        mMainView.setDrawingCacheEnabled(true);
        Bitmap screenbm = Bitmap.createBitmap(mMainView.getDrawingCache());
        mMainView.setDrawingCacheEnabled(false);

        blurredBmp = blurRenderScript(context, screenbm, 20);
        screenbm.recycle();
    }

    private Bitmap blurRenderScript(Context context, Bitmap bitmap, int radius) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());

        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        Allocation inAlloc = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE);
        Allocation outAlloc = Allocation.createFromBitmap(rs, output);
        script.setRadius(radius);
        script.setInput(inAlloc);
        script.forEach(outAlloc);
        outAlloc.copyTo(output);

        rs.destroy();

        return output;
    }

    @Override
    protected void onPanelDragged(int newTop) {
        if (bmDrawable != null) {
            bmDrawable.getBitmap().recycle();
        }
        if (blurredBmp != null && blurredBmp.getHeight() - newTop > 0) {
            Bitmap croppedBmp = Bitmap.createBitmap(blurredBmp, 0, newTop, blurredBmp.getWidth(), blurredBmp.getHeight() - newTop);
            bmDrawable = new BitmapDrawable(getResources(), croppedBmp);
            bmDrawable.setBounds(mMainView.getLeft(), newTop, mMainView.getRight(), blurredBmp.getHeight());
        } else {
            bmDrawable = null;
        }
        super.onPanelDragged(newTop);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        updateBackgroundImage();
    }

}
