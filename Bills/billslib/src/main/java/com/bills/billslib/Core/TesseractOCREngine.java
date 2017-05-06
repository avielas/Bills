package com.bills.billslib.Core;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.bills.billslib.Contracts.IOcrEngine;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Contracts.Enums.PageSegmentation;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by michaelvalershtein on 02/04/2017.
 */

public class TesseractOCREngine implements IOcrEngine {
    private TessBaseAPI _tessBaseAPI = new TessBaseAPI();
    private final String EngLanguage = "eng";
    private final String HebLanguage = "heb";
    private final String NumbersOnlyWhiteList = "1234567890.";

    private Rect _curRectangle = null;

    private boolean _initialized = false;
    private boolean _imageSet = false;

    @Override
    public void Init(String datapath, Language language) throws IllegalArgumentException, RuntimeException {
        String tessLanguage = "";
        switch (language){
            case English:
                tessLanguage = EngLanguage;
                break;
            case Hebrew:
                tessLanguage = HebLanguage;
                break;
            default:
                throw new IllegalArgumentException("Unrecogised language: " + language.toString());
        }
        try {
            _tessBaseAPI.init(datapath, tessLanguage);
            _initialized = true;
            _imageSet = false;
        }catch (Exception ex){
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Override
    public void SetImage(Bitmap bmp) throws IllegalStateException, IllegalArgumentException, RuntimeException {
        if(bmp == null){
            throw new IllegalArgumentException("Bitmap cannot be null.");
        }else if(bmp.isRecycled()){
            throw new IllegalArgumentException("Bitmap cannot be recycled.");
        }else if(!_initialized){
            throw new IllegalStateException("OCR Engine must be initialized before use.");
        }

        try{
            _tessBaseAPI.setImage(bmp);
            _imageSet = true;
        }catch(Exception ex){
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Override
    public List<Rect> GetTextlines() throws IllegalStateException, RuntimeException {
        CheckInitialized();

        Pixa pixaTextLines = null;
        try{
            List<Rect> textLinesRectangles = new ArrayList<>();
            pixaTextLines = _tessBaseAPI.getTextlines();

            for(int i = 0; i < pixaTextLines.size(); i++){
                textLinesRectangles.add(pixaTextLines.getBoxRect(i));
            }

            if(textLinesRectangles.size() <= 0){
                throw new RuntimeException("Failed to find lines.");
            }

            return textLinesRectangles;
        }catch (Exception ex){
            throw new RuntimeException("Something went wrong while getting lines bounding rectangles. Error: " + ex.getMessage());
        }finally {
            if(pixaTextLines != null){
                pixaTextLines.recycle();
            }
        }
    }

    @Override
    public List<Rect> GetWords() throws IllegalStateException, RuntimeException {
        CheckInitialized();
        //TODO validate that offsets needed
        int offsetX = 0;
        int offsetY = 0;

        if(_curRectangle != null){
            offsetX = _curRectangle.left;
            offsetY = _curRectangle.top;
        }
        Pixa wordsPixa = null;
        try {
            wordsPixa = _tessBaseAPI.getConnectedComponents();
            Pix pixWithMaxWidth = FindPixWithMaxWidth(wordsPixa);
            MergeComponentsToWords(pixWithMaxWidth, wordsPixa);
            pixWithMaxWidth.recycle();

            List<Rect> wordsRectangles = new ArrayList<>();

            if(wordsPixa == null){
                throw new RuntimeException("No words found.");
            }

            for (int i = 0; i < wordsPixa.size(); i++){
                Rect wordRect = wordsPixa.getBoxRect(i);
                if(wordRect != null) {
                    wordRect.left += offsetX;
                    wordRect.right += offsetX;
                    wordRect.top += offsetY;
                    wordRect.bottom += offsetY;
                }
                wordsRectangles.add(wordRect);
            }

            if(wordsRectangles.size() <= 0){
                throw new RuntimeException("No words found.");
            }

            return  wordsRectangles;
        }catch(Exception ex){
            throw new RuntimeException("Something went wrong while getting words bounding rectangles. Error: " + ex.getMessage());
        }finally {
            if(wordsPixa != null){
                wordsPixa.recycle();
            }
        }
    }

    @Override
    public String GetUTF8Text() throws IllegalStateException, RuntimeException {
        CheckInitialized();

        try{
            return _tessBaseAPI.getUTF8Text();
        }catch(Exception ex){
            throw new RuntimeException("Something went wrong while getting text. Error: " + ex.getMessage());
        }

    }

    @Override
    public void SetPageSegMode(PageSegmentation pageSegMode) throws IllegalStateException, RuntimeException {
        CheckInitialized();

        try{
            _tessBaseAPI.setPageSegMode(GetPageSegMode(pageSegMode));
        }catch (Exception ex){
            throw new RuntimeException("Something went wrong while setting page segmentation mode to " + pageSegMode +
            ". Error: " + ex.getMessage());
        }
    }


    @Override
    public boolean Initialized() {

        return _initialized;
    }

    @Override
    public int MeanConfidence() throws IllegalStateException, RuntimeException {
        CheckInitialized();

        try{
            return _tessBaseAPI.meanConfidence();
        }catch (Exception ex){
            throw new RuntimeException("Something went wrong while getting mean confidence. Error: " + ex.getMessage());
        }
    }

    @Override
    public void End() throws RuntimeException {
        _tessBaseAPI.end();

    }

    @Override
    public void SetNumbersOnlyFormat() throws IllegalStateException, RuntimeException {
        if(!_initialized){
            throw new IllegalStateException("OCR Engine must be initialized before use");
        }

        try {
            _tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, NumbersOnlyWhiteList);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to set numbers only format. Error: " + ex.getMessage());
        }
    }

    @Override
    public void SetTextOnlyFormat() throws IllegalStateException, RuntimeException {

    }

    @Override
    public void SetAllCharactersWhitelist() throws IllegalStateException, RuntimeException {
        if(!_initialized){
            throw new IllegalStateException("OCR Engine must be initialized before use");
        }

        try{
            _tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "");
        }catch (Exception ex){
            throw new RuntimeException("Something went wrong while setting empty whitelist. Error: " + ex.getMessage());
        }
    }

    @Override
    public void SetRectangle(Rect rect) throws IllegalStateException, RuntimeException {
        CheckInitialized();

        try{
            _tessBaseAPI.setRectangle(rect);
            _curRectangle = rect;
        }catch(Exception ex){
            throw new RuntimeException("Something went wrong while setting bounding rectangle. Error: " + ex.getMessage());
        }
    }

    @Override
    public Bitmap GetThresholdedImage() throws IllegalStateException, RuntimeException {
        CheckInitialized();

        Pix thresholdedPixImage = null;
        try{
            thresholdedPixImage = _tessBaseAPI.getThresholdedImage();
            if(thresholdedPixImage == null){
                throw new RuntimeException("Failed to get thresholeded image, got null from TessBaseAPI.");
            }

            return WriteFile.writeBitmap(thresholdedPixImage);
        }catch (Exception ex){
            throw new RuntimeException("Something went wrong while getting thresholded image. Error: " + ex.getMessage());
        }finally {
            if(thresholdedPixImage != null){
                thresholdedPixImage.recycle();
            }
        }
    }

    private void CheckInitialized() throws IllegalStateException{
        if(!_initialized || !_imageSet){
            String message = "OCR Engine must be initialized and image must be set before use. Image set: " + _imageSet +
                    ", Initialized: " + _initialized;
            throw new IllegalStateException(message);
        }
    }

    private int GetPageSegMode(PageSegmentation pageSegMode) throws Exception {
        switch (pageSegMode) {
            case SingleLine:
                return TessBaseAPI.PageSegMode.PSM_SINGLE_LINE;
            case SingleWord:
                return TessBaseAPI.PageSegMode.PSM_SINGLE_WORD;
            default:
                throw new Exception("Failed parse page segmentation mode to " + pageSegMode.toString());

        }
    }

    private Pixa MergeComponentsToWords(Pix pixWithMaxWidth, Pixa pixa) {
        int i = 0;
        Pix currPix = pixa.getPix(0);
        Pix nextPix = pixa.getPix(1);
        Rect currRect = pixa.getBoxRect(0);
        Rect nextRect = pixa.getBoxRect(1);
        while(null != currPix && null != nextPix) {
            if(nextRect.left - currRect.right < (pixWithMaxWidth.getWidth() + 1.5*pixWithMaxWidth.getWidth()))
            {
                pixa.mergeAndReplacePix(i, i + 1);
            }
            else {
                i++;
            }
            currPix = pixa.getPix(i);
            nextPix = pixa.getPix(i+1);
            currRect = pixa.getBoxRect(i);
            nextRect = pixa.getBoxRect(i+1);
        }
        return pixa;
    }

    private Pix FindPixWithMaxWidth(Pixa pixa) {
        Pix pixWithMaxWidth;
        int i = 0;
        Pix currPix = pixa.getPix(0);
        pixWithMaxWidth = currPix;
        i++;
        currPix = pixa.getPix(i);
        while(null != currPix) {
            pixWithMaxWidth = currPix.getWidth() > pixWithMaxWidth.getWidth() ?
                    currPix :
                    pixWithMaxWidth;
            i++;
            currPix = pixa.getPix(i);
        }
        return pixWithMaxWidth;
    }
}
