package com.bills.billslib.CustomViews;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by mvalersh on 9/19/2016.
 */
public class ItemView extends LinearLayout {
    private final String Tag = this.getClass().getSimpleName();
    public final Double Price;
    private Bitmap _item;
    private boolean _recycled = false;

    public ItemView(Context context, double price, Bitmap item) {
        super(context);
        Price = price;
        _item = item;
        _recycled = false;
    }

    public boolean SetItemBackgroundColor(int color){
        if (_recycled){
            Log.d(this.Tag, "Failed to set background color, already recycled.");
            return false;
        }

        this.setBackgroundColor(color);
        this.removeAllViewsInLayout();
        ChangeColor(color);

        ImageView imageView = new ImageView(this.getContext());
        imageView.setPadding(20, 20, 20, 20);
        imageView.setImageBitmap(_item);

        TextView textView = new TextView(this.getContext());
        textView.setPadding(20, 20, 20, 20);
        textView.setText(Price.toString());

        this.addView(textView);
        this.addView(imageView);
        return true;
    }

    public void Recycle() {
        _recycled = true;
        _item.recycle();
    }

    protected void finalize(){
        Recycle();
        try {
            super.finalize();
        } catch (Throwable throwable) {
        }
    }

    private void ChangeColor(int color){
        if (!OpenCVLoader.initDebug()) {
            Log.d(Tag, "Failed to initialize OpenCV.");
        }

        if(color == Color.WHITE) {
            Mat monoChromeMat = new Mat();
            Mat src = new Mat(_item.getWidth(), _item.getHeight(), CvType.CV_8UC4);
            Utils.bitmapToMat(_item, src);
            Imgproc.threshold(src, monoChromeMat, 180, 255, Imgproc.THRESH_BINARY);
            Utils.matToBitmap(monoChromeMat, _item);
            monoChromeMat.release();
            src.release();
            return;
        }
        else {
            for (int i = 0; i < _item.getWidth(); i++) {
                for (int j = 0; j < _item.getHeight(); j++) {
                    if (_item.getPixel(i, j) != Color.BLACK) {
                        _item.setPixel(i, j, color);
                    }
                }
            }
        }
    }
}
