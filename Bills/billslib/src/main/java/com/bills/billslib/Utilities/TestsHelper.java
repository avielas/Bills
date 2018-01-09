package com.bills.billslib.Utilities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.bills.billslib.Contracts.Interfaces.IOcrEngine;
import com.bills.billslib.Core.TesseractOCREngine;

import java.util.List;

/**
 * Created by aviel on 01/07/17.
 */

public class TestsHelper {
    public static Bitmap PrintWordsRects(IOcrEngine tesseractOCREngine, Bitmap warpedBill, Bitmap processedBill, String className) {
        List<Rect> words;
        Bitmap printedBitmap = Bitmap.createBitmap(warpedBill);
        try {
            tesseractOCREngine.SetImage(processedBill);
            List<Rect> lineRects = tesseractOCREngine.GetTextlines();

            /************ the following is waiting for GC to finish his job. ********/
            /************ without it the red lines will not be printed. *************/
//            Thread.sleep(50);
            /**************************/
            Paint paint = new Paint();
            Canvas canvas = new Canvas(printedBitmap);
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);

            for (Rect line : lineRects) {
                tesseractOCREngine.SetRectangle(line);
                try {
                    words = tesseractOCREngine.GetWords();
                    for (Rect rect : words) {
                        canvas.drawRect(rect, paint);
                    }
                } catch (Exception ex) {
                    Log.d(className, "Failed to get words of line. Error: " + ex.getMessage());
                    canvas.drawRect(line, paint);
                }
            }
        } catch (Exception ex) {
            Log.d(className, "Failed to map numbered values to location. Error: " + ex.getMessage());
            return null;
        }
        return printedBitmap;
    }
}
