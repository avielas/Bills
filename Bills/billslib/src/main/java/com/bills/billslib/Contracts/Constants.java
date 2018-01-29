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
    public static final String FIREBASE_LOCAL_STORAGE = STORAGE_DIRECTORY + "/TesseractSample/fb_storage";
//    public static final String CAMERA_CAPTURED_PNG_PHOTO_PATH = IMAGES_PATH + "/camera.png";
    public static final String WARPED_PNG_PHOTO_PATH = IMAGES_PATH + "/warped.png";
    public static final String BEFORE_BILLS_CORNER_PNG_PHOTO_PATH = IMAGES_PATH + "/before_bill_corners.png";
    public static final String AFTER_BILLS_CORNER_PNG_PHOTO_PATH = IMAGES_PATH + "/after_bill_corners.png";
    public static final String TESSDATA = "tessdata";
    public static final String TESSERACT_SAMPLE_DIRECTORY = STORAGE_DIRECTORY + "/TesseractSample/";
    public static final String TEST_OUTPUT_FILE = TESSERACT_SAMPLE_DIRECTORY + "/TestsOutput.txt";
    public static final String FAILED_TEST_OUTPUT_FILE = TESSERACT_SAMPLE_DIRECTORY + "/FailedTestsOutput.txt";
    public static final int ENLARGE_RECT_VALUE = 3;
}

