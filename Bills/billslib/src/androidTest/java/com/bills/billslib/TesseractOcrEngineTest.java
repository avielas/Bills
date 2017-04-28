package com.bills.billslib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Core.TesseractOCREngine;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by michaelvalershtein on 16/04/2017.
 */

@RunWith(AndroidJUnit4.class)
public class TesseractOcrEngineTest {

    private static final String Product = "product";
    private static final String Quantity = "quantity";
    private static final String Price = "price";

    private static final String[] ProductsList = {"mike", "גלידה"};
    private static final Integer[] QuantityList = {1, 1};
    private static final Double[] PricesList = {54.2, 4.5};

    private ArrayList<ArrayList<Rect>> actualLocations = new ArrayList<>();

    private LinkedHashMap billLines = new LinkedHashMap();
    @Test
    public void TestGetWords(){
        TesseractOCREngine tesseractOCREngine = new TesseractOCREngine();
        tesseractOCREngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
        tesseractOCREngine.SetImage(CreateTextImage(650, 480));

        List<Rect> wordRects = tesseractOCREngine.GetWords();
        for (int i = 0; i < actualLocations.size(); i++) {
            for (int j = 0; j < actualLocations.get(i).size(); j++) {
                assertEquals(wordRects.get(i*actualLocations.get(i).size() + j), actualLocations.get(i).get(j));
                Log.d("MMM", "" + wordRects.get(i*actualLocations.get(i).size() + j) + actualLocations.get(i).get(j));
            }
        }
        tesseractOCREngine.End();

    }

    @Test
    public void TestGetWordsFromLine(){
        TesseractOCREngine tesseractOCREngine = new TesseractOCREngine();
        tesseractOCREngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
        tesseractOCREngine.SetImage(CreateTextImage(650, 480));

        List<Rect> lineRects = tesseractOCREngine.GetTextlines();
        for (int i = 0; i < lineRects.size(); i++) {
            tesseractOCREngine.SetRectangle(lineRects.get(i));
            List<Rect> wordRects = tesseractOCREngine.GetWords();

            for (int j = 0; j < wordRects.size(); j++) {
                assertEquals(wordRects.get(j), actualLocations.get(i).get(j));
                Log.d("MMM", "" + wordRects.get(j) + actualLocations.get(i).get(j));
            }
        }
        tesseractOCREngine.End();
    }

    @Test
    public void TestGetUTF8Text(){
        try {
            TesseractOCREngine tesseractOCREngine = new TesseractOCREngine();
            tesseractOCREngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
            tesseractOCREngine.SetImage(CreateTextImage(650, 480));

            List<Rect> wordRects = tesseractOCREngine.GetWords();
            Log.d("MMM", "Start UTF8");

            for (int i = 0; i < wordRects.size() / 3; i++) {
                tesseractOCREngine.SetRectangle(wordRects.get(i));
                String res = tesseractOCREngine.GetUTF8Text();
                Log.d("MMM", res + " : " + ProductsList[i]);
                assertEquals(res, ProductsList[i]);


                tesseractOCREngine.SetRectangle(wordRects.get(i + 1));
                res = tesseractOCREngine.GetUTF8Text();
                Log.d("MMM", res + " : " + QuantityList[i]);
                assertEquals(res, QuantityList[i]);

                tesseractOCREngine.SetRectangle(wordRects.get(i + 2));
                res = tesseractOCREngine.GetUTF8Text();
                Log.d("MMM", res + " : " + PricesList[i]);
                assertEquals(res, PricesList[i]);
            }
        }catch (Exception ex){
            Log.d("MMM", ex.getMessage());
        }
    }

    private Bitmap CreateTextImage(int width, int height) {
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Paint paint = new Paint();
        final Canvas canvas = new Canvas(bmp);

        canvas.drawColor(Color.WHITE);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(50.0f);

        InitializeTestsParameters(0);

        int yOffset = 45;

        actualLocations.add(0, new ArrayList<Rect>());
        actualLocations.get(0).add(new Rect(0, 9, 48, 45));
        actualLocations.get(0).add(new Rect(126, 9, 140, 45));
        actualLocations.get(0).add(new Rect(327, 8, 430, 45));

        actualLocations.add(1, new ArrayList<Rect>());
        actualLocations.get(1).add(new Rect(0, 250, 48, 285));
        actualLocations.get(1).add(new Rect(126, 249, 140, 285));
        actualLocations.get(1).add(new Rect(317, 244, 432, 286));
        for (int i = 0; i < billLines.size(); i++)
        {
            String price = ((HashMap)billLines.get(i)).get(Price).toString();
            String quantity = ((HashMap)billLines.get(i)).get(Quantity).toString();
            String product = ((HashMap)billLines.get(i)).get(Product).toString();


            canvas.drawText(price, 50, yOffset, paint);
            canvas.drawText(quantity, 150, yOffset, paint);
            canvas.drawText(product, 2*(width / 3), yOffset, paint);
            yOffset += height / billLines.size();
        }

        return bmp;
    }

    private void InitializeTestsParameters(int startIndex) {
        for(int i = 0; i < ProductsList.length; i++) {
            billLines.put(startIndex, new HashMap<>());
            HashMap lineHash = (HashMap) billLines.get(startIndex++);
            lineHash.put(Product, ProductsList[i]);
            lineHash.put(Quantity, QuantityList[i]);
            lineHash.put(Price, PricesList[i]);
        }
    }
}
