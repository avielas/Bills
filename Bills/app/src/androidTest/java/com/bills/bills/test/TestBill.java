package com.bills.bills.test;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;
import com.bills.billslib.Utilities.Utilities;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by avielavr on 5/19/2017.
 */

public class TestBill extends Thread{
    private String Tag = this.getClass().getSimpleName();
    private String _rootBrandModelDirectory;
    private String _restaurant;
    private String _billFullName;
    private String _fileName;
    private String _key;
    private StringBuilder _results;
    private Boolean _isRunJustTM;
    private Queue<Pair> _accuracyPercentQueue;
    private Queue<Pair> _passedResultsQueue;
    private Queue<Pair> _failedResultsQueue;
    private UUID _sessionId;
    private int _dragRectViewWidth = 1440;
    private int _dragRectViewHeight = 2308;

    public TestBill(final UUID sessionId, String rootBrandModelDirectory, String restaurant, String bill,
                    Boolean isRunJustTM, Queue<Pair> accuracyPercentQueue,
                    Queue<Pair> passedResultsQueue, Queue<Pair> failedResultsQueue)
    {
        _sessionId = sessionId;
        _rootBrandModelDirectory = rootBrandModelDirectory;
        _restaurant = restaurant;
        _billFullName = bill;
        _results = new StringBuilder();
        _isRunJustTM = isRunJustTM;
        _accuracyPercentQueue = accuracyPercentQueue;
        _passedResultsQueue = passedResultsQueue;
        _failedResultsQueue = failedResultsQueue;
        _fileName = _billFullName.substring(_billFullName.lastIndexOf("/")+1);
        _key = _restaurant + "_" + _fileName.subSequence(0, _fileName.lastIndexOf('.'));
    }

    @Override
    public void run(){
        synchronized (this) {
            TemplateMatcher templateMatcher;
            TesseractOCREngine tesseractOCREngine;
            tesseractOCREngine = new TesseractOCREngine();
            String expectedTxtFileName = _restaurant.toString() + ".txt";
            BillAreaDetector areaDetector = new BillAreaDetector(_sessionId);
            Point topLeft = new Point();
            Point topRight = new Point();
            Point buttomLeft = new Point();
            Point buttomRight = new Point();
            List<String> expectedBillTextLines = null;

            if (!OpenCVLoader.initDebug()) {
                Log.d(Tag, "Failed to initialize OpenCV.");
                return;
            }
            Mat billMat = new Mat();
            Mat billMatCopy = new Mat();

            try {
                _results.append(System.getProperty("line.separator") + "Test of " + _billFullName + System.getProperty("line.separator"));
                tesseractOCREngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
                expectedBillTextLines = Utilities.ReadTextFile(_sessionId, _rootBrandModelDirectory + _restaurant + "/" + expectedTxtFileName);
                if(expectedBillTextLines == null){
                    throw new Exception();
                }
                byte[] bytes = Utilities.ImageTxtFile2ByteArray(_sessionId, _billFullName);
                if(bytes == null){
                    throw new Exception();
                }
                billMat = Utilities.Bytes2MatAndRotateClockwise90(_sessionId, bytes);
                if(billMat == null){
                    throw new Exception();
                }
                if (!areaDetector.GetBillCorners(billMat, topRight, buttomRight, buttomLeft, topLeft)) {
                    throw new Exception();
                }

                /*** CONVERSION ***/
                double factorX = billMat.width() / (1.0*_dragRectViewWidth);
                double factorY = billMat.height() / (1.0*_dragRectViewHeight);

                topRight = (org.opencv.core.Point)Utilities.GetScaledPoint(topRight, factorX, factorY);
                buttomRight = (org.opencv.core.Point)Utilities.GetScaledPoint(buttomRight, factorX, factorY);
                buttomLeft = (org.opencv.core.Point)Utilities.GetScaledPoint(buttomLeft, factorX, factorY);
                topLeft = (org.opencv.core.Point)Utilities.GetScaledPoint(topLeft, factorX, factorY);

                double factorXreverse = (1.0*_dragRectViewWidth) / billMat.width();
                double factorYreverse = (1.0*_dragRectViewHeight) / billMat.height();

                topRight = (org.opencv.core.Point) Utilities.GetScaledPoint(topRight, factorXreverse, factorYreverse);
                buttomRight = (org.opencv.core.Point) Utilities.GetScaledPoint(buttomRight, factorXreverse, factorYreverse);
                buttomLeft = (org.opencv.core.Point) Utilities.GetScaledPoint(buttomLeft, factorXreverse, factorYreverse);
                topLeft = (org.opencv.core.Point) Utilities.GetScaledPoint(topLeft, factorXreverse, factorYreverse);
                /*** END CONVERSION ***/

                try {
                    billMat = ImageProcessingLib.WarpPerspective(billMat, topLeft, topRight, buttomRight, buttomLeft);
                    billMatCopy = billMat.clone();
                } catch (Exception e) {
                    BillsLog.Log(_sessionId, LogLevel.Error, "Failed to warp perspective. Exception: " + e.getMessage(), LogsDestination.BothUsers, Tag);
                    //TODO: decide what to do. Retake the picture? crash the app?
                    throw new Exception();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Bitmap processedBillBitmap = Bitmap.createBitmap(billMat.width(), billMat.height(), Bitmap.Config.ARGB_8888);
            ImageProcessingLib.PreprocessingForTM(billMat);
            Utils.matToBitmap(billMat, processedBillBitmap);

            /********* the following prints resd lines on bill and save it *********/
//            Bitmap warpedBillBitmap = Bitmap.createBitmap(billMat.width(), billMat.height(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(billMatCopy, warpedBillBitmap);
//            Bitmap printWordsRectsbill = TestsHelper.PrintWordsRects(tesseractOCREngine, warpedBillBitmap, processedBillBitmap,
//                                                                                                            this.getClass().getSimpleName());
//            String pathToSavePrintRects = Constants.PRINTED_RECTS_IMAGES_PATH + "/" +_key + ".jpg";
//            String pathToSavePrePrintRects = Constants.PRINTED_RECTS_IMAGES_PATH + "/" +_key + "1.jpg";
//            Utilities.SaveToJPGFile(printWordsRectsbill, pathToSavePrintRects);
//            Utilities.SaveToJPGFile(processedBillBitmap, pathToSavePrePrintRects);
//            printWordsRectsbill.recycle();
//            warpedBillBitmap.recycle();
            /***********************************************************************/

            templateMatcher = new TemplateMatcher(tesseractOCREngine, processedBillBitmap);
            try{
                templateMatcher.Match(_sessionId);
            }
            catch (Exception e){
                _results.append(" " + Log.getStackTraceString(e)+ System.getProperty("line.separator"));
                _failedResultsQueue.add(new Pair(_key, _results));
                _accuracyPercentQueue.add(new Pair(_key, 0.0));
                return;
            }

            int numOfItems = templateMatcher.priceAndQuantity.size();

            if (_isRunJustTM) {
                HandlingTMResults(numOfItems, expectedBillTextLines.size());
                processedBillBitmap.recycle();
                billMat.release();
                billMatCopy.release();
                tesseractOCREngine.End();
                return;
            }

            ImageProcessingLib.PreprocessingForParsing(billMatCopy);
            /***** we use processedBillBitmap second time to prevent another Bitmap allocation due to *****/
            /***** Out Of Memory when running 4 threads parallel                                      *****/
            Utils.matToBitmap(billMatCopy, processedBillBitmap);
            templateMatcher.InitializeBeforeSecondUse(processedBillBitmap);
            templateMatcher.Parsing(_sessionId, numOfItems);
            LinkedHashMap ocrResultCroppedBill = GetOcrResults(templateMatcher);
            CompareExpectedToOcrResult(ocrResultCroppedBill, expectedBillTextLines);
            processedBillBitmap.recycle();
            billMat.release();
            billMatCopy.release();
            tesseractOCREngine.End();
            _passedResultsQueue.add(new Pair(_key, _results));
        }
    }

    /**
     *
     * @param numOfItemsTM
     * @param numOfItemsExpectedTxt
     */
    private void HandlingTMResults(int numOfItemsTM, int numOfItemsExpectedTxt) {
        if(numOfItemsExpectedTxt == numOfItemsTM){
            _passedResultsQueue.add(new Pair(_key, _results));
            _accuracyPercentQueue.add(new Pair(_key, 100.0));
        }
        else{
            _failedResultsQueue.add(new Pair(_key, _results));
            _accuracyPercentQueue.add(new Pair(_key, 0.0));
        }
    }

    /**
     *
     * @param templateMatcher TM structure
     * @return LinkedHashMap structure
     */
    private LinkedHashMap GetOcrResults(TemplateMatcher templateMatcher) {
        int i = 0;
        LinkedHashMap imageLinesLinkedHashMap = new LinkedHashMap();
        for(Double[] priceQuantity : templateMatcher.priceAndQuantity){
            imageLinesLinkedHashMap.put(i, new HashMap<>());
            HashMap lineHash = (HashMap)imageLinesLinkedHashMap.get(i);
            lineHash.put("product",templateMatcher.itemLocationsRect.get(i));
            lineHash.put("price",priceQuantity[0]);
            lineHash.put("quantity",priceQuantity[1]);
            i++;
        }
        return imageLinesLinkedHashMap;
    }

    /**
     * comparing line to line ocr results of bill vs expected txt file
     * @param ocrResultCroppedBill ocr results of cropped bill
     * @param expectedBillTextLines expected bill lines from txt file
     */
    private void CompareExpectedToOcrResult(LinkedHashMap ocrResultCroppedBill, List<String> expectedBillTextLines) {
        _results.append("Validating Ocr Result" + System.getProperty("line.separator"));
        Double accuracyPercent = Compare(ocrResultCroppedBill, expectedBillTextLines);

        if(ocrResultCroppedBill.size() != expectedBillTextLines.size())
        {
            _results.append("ocrResultCroppedBill contains "+ ocrResultCroppedBill.size() + " lines, but" +
                    " expectedBillTextLines contains "+ expectedBillTextLines.size()+" lines" + System.getProperty("line.separator"));
        }

        String formattedAccuracyPercent = String.format("%.02f", accuracyPercent);
        _results.append("Accuracy is " + formattedAccuracyPercent + "%" + System.getProperty("line.separator"));
        _accuracyPercentQueue.add(new Pair(_key, accuracyPercent));
    }

    /**
     *
     * @param ocrResult ocr result of bill included price and quantity
     * @param expectedBillTextLines expected bill lines from txt file
     * @return true in case of equal results. false if unequal
     */
    private Double Compare(LinkedHashMap ocrResult, List<String> expectedBillTextLines) {
        int lineNumber = 0;
        Double countInvalids = 0.0;
        Double accuracyPercent;

        for (String expectedLine : expectedBillTextLines)
        {
            String[] rowsOfLine = expectedLine.split(" ");
            Double expectedPrice = Double.parseDouble(rowsOfLine[0]);
            Integer expectedQuantity = Integer.parseInt(rowsOfLine[1]);
            HashMap ocrResultLine = (HashMap)ocrResult.get(lineNumber);
            if(null == ocrResultLine)
            {
                _results.append("line "+ lineNumber +" doesn't exist on ocr results" + System.getProperty("line.separator"));
                countInvalids++;
                lineNumber++;
                continue;
            }
            Double quantity = (Double)ocrResultLine.get("quantity");
            Integer ocrResultQuantity = quantity.intValue();
            Double ocrResultPrice = (Double)ocrResultLine.get("price");
            if(!expectedPrice.equals(ocrResultPrice))
            {
                _results.append("line "+lineNumber+" - Price: expected "+expectedPrice+", "+"ocr "+ocrResultPrice
                        + System.getProperty("line.separator"));
                ++countInvalids;
            }
            if(!expectedQuantity.equals(ocrResultQuantity))
            {
                _results.append("line "+lineNumber+" - Quantity: expected "+expectedQuantity+", "+"ocr "+ocrResultQuantity
                        + System.getProperty("line.separator"));
                ++countInvalids;
            }
            lineNumber++;
        }
        //calculate the accuracy percent
        accuracyPercent = ((lineNumber*2 - countInvalids)/(lineNumber*2)) * 100;
        return accuracyPercent;
    }
}

