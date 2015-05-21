package com.sothree.slidinguppanel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;

import java.util.ArrayList;

public class BlurredSlidingUpPanel extends SlidingUpPanelLayout implements ViewTreeObserver.OnGlobalLayoutListener {
    private Context context;
    private Bitmap blurredBmp;
    private BitmapDrawable bmDrawable;
    private ArrayList<AsyncTask> blurtasks = new ArrayList<AsyncTask>();

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

    private class BlurredBGTask extends AsyncTask<Void, Void, Bitmap> {
        private Bitmap screenbm;
        @Override
        protected void onPreExecute() {
            mMainView.setDrawingCacheEnabled(true);
            screenbm = Bitmap.createBitmap(mMainView.getDrawingCache());
            mMainView.setDrawingCacheEnabled(false);
        }
        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap blurred = blurRenderScript(context, screenbm, 20);
            screenbm.recycle();
            return blurred;
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            blurredBmp = bitmap;
            blurtasks.remove(this);
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
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mMainView != null) {
            mMainView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
    }

    @SuppressLint("NewApi") @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mMainView != null) {
            try {
                mMainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } catch (Exception e) {
                mMainView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                Log.e("BlurredSlidingUpPanel", "Error removing global layout listener");
            }

        }
        for(AsyncTask task : blurtasks) {
            task.cancel(true);
        }
        blurtasks.clear();
    }

    @Override
    public void onDraw(Canvas c) {
        if (bmDrawable != null) {
            bmDrawable.draw(c);
        }
        super.onDraw(c);
    }

    private void updateBackgroundImage() {
        for (AsyncTask task : blurtasks) {
            task.cancel(true);
        }
        blurtasks.clear();
        BlurredBGTask task = new BlurredBGTask();
        blurtasks.add(task);
        task.execute();
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

    /*
    * OnGlobalLayoutListener implementation
    */

    @Override
    public void onGlobalLayout() {
        Log.i("BRANDON", "onGlobalLayout");
        updateBackgroundImage();
    }
}
