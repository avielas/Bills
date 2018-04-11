//package com.bills.billslib.Core;
//
//import android.app.Application;
//import android.graphics.Bitmap;
//import android.graphics.Rect;
//import android.preference.PreferenceManager;
//import android.util.Log;
//
//import com.abbyy.mobile.ocr4.AssetDataSource;
//import com.abbyy.mobile.ocr4.DataSource;
//import com.abbyy.mobile.ocr4.Engine;
//import com.abbyy.mobile.ocr4.FileLicense;
//import com.abbyy.mobile.ocr4.License;
//import com.abbyy.mobile.ocr4.RecognitionConfiguration;
//import com.abbyy.mobile.ocr4.RecognitionManager;
//import com.abbyy.mobile.ocr4.layout.MocrPrebuiltLayoutInfo;
//import com.bills.billslib.Contracts.Enums.Language;
//import com.bills.billslib.Contracts.Enums.PageSegmentation;
//import com.bills.billslib.Contracts.Interfaces.IOcrEngine;
//import com.bills.billslib.R;
//
//import org.opencv.core.Scalar;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * Created by michaelvalershtein on 28/03/2018.
// */
//
//public class AbbyyOcrEngine implements IOcrEngine, RecognitionManager.RecognitionCallback {
//
//    private Application mApplication;
//    private int mResId;
//    private static final String _licenseFile = "license";
//    private static final String _applicationID = "Android_ID";
//
//    private static final String _patternsFileExtension = ".mp3";
//    private static final String _dictionariesFileExtension = ".mp3";
//    private static final String _keywordsFileExtension = ".mp3";
//
//    public AbbyyOcrEngine(Application application, int resId){
//        mApplication = application;
//        mResId = resId;
//    }
//
//    @Override
//    public void Init(String datapath, Language language) throws IllegalArgumentException, RuntimeException {
//        Engine.loadNativeLibrary();
//        try {
//            // Write default settings to the settings store. These values will be written only during the first
//            // startup or
//            // if the values are rubbed.
//            PreferenceManager.setDefaultValues(mApplication, mResId, true);
//
//            final DataSource assetDataSrouce = new AssetDataSource(mApplication.getAssets());
//
//            final List<DataSource> dataSources = new ArrayList<DataSource>();
//            dataSources.add(assetDataSrouce);
//
//            Engine.loadNativeLibrary();
//            try {
//                Engine.createInstance(dataSources, new FileLicense(assetDataSrouce, _licenseFile, _applicationID),
//                        new Engine.DataFilesExtensions(_patternsFileExtension, _dictionariesFileExtension,
//                                _keywordsFileExtension));
//
//
//            } catch (final IOException e) {
//            } catch (final License.BadLicenseException e) {
//            }
//        }catch(Exception ex){
//
//        }
//    }
//
//    @Override
//    public void SetImage(Bitmap bmp) throws IllegalStateException, IllegalArgumentException, RuntimeException {
//
//    }
//
//    @Override
//    public List<Rect> GetTextlines() throws IllegalStateException, RuntimeException {
//        return null;
//    }
//
//    @Override
//    public List<Rect> GetWords() throws IllegalStateException {
//        return null;
//    }
//
//    @Override
//    public String GetUTF8Text() throws IllegalStateException, RuntimeException {
//        return null;
//    }
//
//    @Override
//    public void SetPageSegMode(PageSegmentation pageSegMode) throws IllegalStateException, RuntimeException {
//
//    }
//
//    @Override
//    public boolean Initialized() {
//        return false;
//    }
//
//    @Override
//    public int MeanConfidence() throws IllegalStateException, RuntimeException {
//        return 0;
//    }
//
//    @Override
//    public void End() throws RuntimeException {
//
//    }
//
//    @Override
//    public void SetNumbersOnlyFormat() throws IllegalStateException, RuntimeException {
//
//    }
//
//    @Override
//    public void SetTextOnlyFormat() throws IllegalStateException, RuntimeException {
//
//    }
//
//    @Override
//    public void SetAllCharactersWhitelist() throws IllegalStateException, RuntimeException {
//
//    }
//
//    @Override
//    public void SetRectangle(Rect rect) throws IllegalStateException, RuntimeException {
//
//    }
//
//    @Override
//    public Bitmap GetThresholdedImage() throws IllegalStateException, RuntimeException {
//        return null;
//    }
//
//    @Override
//    public Bitmap ChangeBackgroundColor(Bitmap src, Scalar color) {
//        return null;
//    }
//
//
//}
