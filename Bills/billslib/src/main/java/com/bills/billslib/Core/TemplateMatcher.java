package com.bills.billslib.Core;

import android.graphics.Bitmap;

import com.bills.billslib.Contracts.IOcrEngine;

/**
 * Created by michaelvalershtein on 17/04/2017.
 */

public class TemplateMatcher {
    private IOcrEngine _ocrEngine;
    private Bitmap _image;

    public TemplateMatcher(final IOcrEngine ocrEngine, final Bitmap image) throws Exception {
        if(!ocrEngine.Initialized()){
            throw new Exception("OCR Engine was not initialized.");
        }

        if(image == null){
            throw new Exception("Image cannot be null");
        }
        _image = image;
        _ocrEngine = ocrEngine;
    }


}
