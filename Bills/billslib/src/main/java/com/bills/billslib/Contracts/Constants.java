package com.bills.billslib.Contracts;

import android.os.Build;
import android.os.Environment;

/**
 * Created by michaelvalershtein on 16/04/2017.
 */

public final class Constants
{
    /***
     The path "/data/user/0/com.billsplit.billsplit/files" comes from calling to getFilesDir().
     You can call this function just from activuty so we put it here hard-coded
     When accessing from ADB shell the directory is "/data/data/com.billsplit.billsplit/files/TesseractSample/"
     ***/
    public static final String STORAGE_DIRECTORY =
            PreparingEnvironmentUtil.IsRunningOnEmulator(Build.MANUFACTURER, Build.MODEL) ?
                    "/data/local/tmp" :
                    Environment.getExternalStorageDirectory().toString();

    public static final String IMAGES_PATH = STORAGE_DIRECTORY + "/TesseractSample/imgs";
    public static final String PRINTED_RECTS_IMAGES_PATH = STORAGE_DIRECTORY + "/TesseractSample/printRectsImgs";
    public static final String CAMERA_CAPTURED_PHOTO_PATH = IMAGES_PATH + "/ocr.tif";
    public static final String CAMERA_CAPTURED_JPG_PHOTO_PATH = IMAGES_PATH + "/ocr.jpg";
    public static final String CAMERA_CAPTURED_TXT_PHOTO_PATH = IMAGES_PATH + "/ocrBytes.txt";
    public static final String CAMERA_CAPTURED_PHOTO_PATH_TO_DELETE = IMAGES_PATH + "/ocrToDelete.tif";
    public static final String WARPED_PHOTO_PATH = IMAGES_PATH + "/warped.tif";
    public static final String WARPED_TXT_PHOTO_PATH = IMAGES_PATH + "/warped.txt";
    public static final String WARPED_JPG_PHOTO_PATH = IMAGES_PATH + "/warped.jpg";
    public static final String PREPROCESSED_CAPTURED_PHOTO_PATH = IMAGES_PATH + "/preprocessed.jpg";
    public static final String TESSDATA = "tessdata";
    public static final String TESSDATA_PATH = STORAGE_DIRECTORY + "/TesseractSample/"+TESSDATA;
    public static final String TESSERACT_SAMPLE_DIRECTORY = STORAGE_DIRECTORY + "/TesseractSample/";
    public static final String TEST_OUTPUT_FILE = TESSERACT_SAMPLE_DIRECTORY + "/TestsOutput.txt";
    public static final String FAILED_TEST_OUTPUT_FILE = TESSERACT_SAMPLE_DIRECTORY + "/FailedTestsOutput.txt";
    public static final String LANGUAGE_TAG = "heb";
    public static final int ENLARGE_RECT_VALUE = 3;
}

