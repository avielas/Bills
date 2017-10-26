package com.bills.testslib;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bills.billslib.Camera.CameraRenderer;
import com.bills.billslib.Camera.IOnCameraFinished;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Interfaces.IOcrEngine;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;
import com.bills.billslib.R;
import com.bills.billslib.Utilities.FilesHandler;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CameraFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class CameraFragment extends com.bills.billslib.Fragments.CameraFragment {
    @Override
    public void OnCameraFinished(byte[] image) {

        if (!OpenCVLoader.initDebug()) {
            String message = "Failed to initialize OpenCV.";
            Log.d(Tag, message);
            BillsLog.Log(Tag, LogLevel.Error, message);
            mListener.Finish();
        }
        Mat billMat = null;
        Mat billMatCopy = null;
        Bitmap processedBillBitmap = null;
        TemplateMatcher templateMatcher;
        int numOfItems;
        BillAreaDetector areaDetector = new BillAreaDetector();
        Point topLeft = new Point();
        Point topRight = new Point();
        Point buttomLeft = new Point();
        Point buttomRight = new Point();

        while (!mOcrEngine.Initialized()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            billMat = FilesHandler.Bytes2MatAndRotateClockwise90(image);
//            FilesHandler.SaveMatToPNGFile(billMat, Constants.STORAGE_DIRECTORY + "/TesseractSample/imgs" +"/camera.png");
            if(billMat == null){
                throw new Exception();
            }
            if (!areaDetector.GetBillCorners(billMat, topLeft, topRight, buttomRight, buttomLeft)) {
                throw new Exception();
            }

            try {
                billMat = ImageProcessingLib.WarpPerspective(billMat, topLeft, topRight, buttomRight, buttomLeft);
//                FilesHandler.SaveMatToPNGFile(billMat, Constants.STORAGE_DIRECTORY + "/TesseractSample/imgs" +"/warped.png");
//                billMatCopy = billMat.clone();
            } catch (Exception e) {
                BillsLog.Log(Tag, LogLevel.Error, "Failed to warp perspective. Exception: " + e.getMessage());
                //TODO: decide what to do. Retake the picture? crash the app?
                throw new Exception();
            }
            mListener.StartWelcomeFragment(image);
//            BillsLog.Log(Tag, LogLevel.Info, "Warped perspective successfully.");
//
//            processedBillBitmap = Bitmap.createBitmap(billMat.width(), billMat.height(), Bitmap.Config.ARGB_8888);
//            ImageProcessingLib.PreprocessingForTM(billMat);
//            Utils.matToBitmap(billMat, processedBillBitmap);
//
//            templateMatcher = new TemplateMatcher(mOcrEngine, processedBillBitmap);
//            try {
//                templateMatcher.Match();
//                BillsLog.Log(Tag, LogLevel.Info, "Template matcher succeed.");
//            } catch (Exception e) {
//                BillsLog.Log(Tag, LogLevel.Error, "Template matcher threw an exception: " + e.getMessage());
//                e.printStackTrace();
//            }
//
//            ImageProcessingLib.PreprocessingForParsing(billMatCopy);
//            numOfItems = templateMatcher.priceAndQuantity.size();
//
//            /***** we use processedBillBitmap second time to prevent another Bitmap allocation due to *****/
//            /***** Out Of Memory when running 4 threads parallel                                      *****/
//            Utils.matToBitmap(billMatCopy, processedBillBitmap);
//            templateMatcher.InitializeBeforeSecondUse(processedBillBitmap);
//            templateMatcher.Parsing(numOfItems);
//
//            List<BillRow> rows = new ArrayList<>();
//            int index = 0;
//            for (Double[] row : templateMatcher.priceAndQuantity) {
//                Bitmap item = templateMatcher.itemLocationsByteArray.get(index);
//                Double price = row[0];
//                Integer quantity = row[1].intValue();
//                rows.add(new BillRow(price, quantity, index, item));
//                index++;
//            }
//
//            mListener.StartSummarizerFragment(rows, image, mPassCode, mRelativeDbAndStoragePath);
//            BillsLog.Log(Tag, LogLevel.Info, "Parsing finished");
        }catch (Exception ex){
            mListener.StartWelcomeFragment(image);
        }
        finally {
            if(null != billMat){
                billMat.release();
            }
            if(null != processedBillBitmap){
                processedBillBitmap.recycle();
            }
            if(null != billMatCopy){
                billMatCopy.release();
            }
        }
    }
}
