package com.bills.bills.test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;

import org.beyka.tiffbitmapfactory.TiffBitmapFactory;
import org.beyka.tiffbitmapfactory.TiffSaver;
import org.opencv.core.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by avielavr on 5/19/2017.
 */

public class TestBill extends Thread{
    String _rootBrandModelDirectory;
    String _restaurant;
    String _bill;

    public TestBill(String rootBrandModelDirectory, String restaurant, String bill)
    {
        _rootBrandModelDirectory = rootBrandModelDirectory;
        _restaurant = restaurant;
        _bill = bill;
    }

    @Override
    public void run(){
        synchronized (this) {
            TemplateMatcher templateMatcher;
            TesseractOCREngine tesseractOCREngine;
            tesseractOCREngine = new TesseractOCREngine();
            try {
                tesseractOCREngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String currRestaurant = _restaurant;
            String currBill = _bill;
            String expectedTxtFileName = _restaurant.toString() + ".txt";
            List<String> expectedBillTextLines = null;

            try {
                expectedBillTextLines = ReadTxtFile(_rootBrandModelDirectory + currRestaurant + "/" + expectedTxtFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Test of " + currBill);
            Bitmap bill = TifToBitmap(currBill);
            Bitmap warped = CropAndWarpPerspective(bill, currBill);
            Bitmap processedBill = ImageProcessingLib.PreprocessingForTemplateMatcher(warped);
            File file = new File(currBill);
            String pathToSave = file.getParent();
            SaveToJPGFile(processedBill, pathToSave + "/processedTM.jpg");
            Bitmap processedBillForCreateNewBill = ImageProcessingLib.PreprocessingForParsingBeforeTM(warped);
            SaveToJPGFile(processedBillForCreateNewBill, pathToSave + "/processedPars.jpg");
            templateMatcher = new TemplateMatcher(tesseractOCREngine, processedBillForCreateNewBill, processedBill);
            Bitmap itemsArea = templateMatcher.MatchWhichReturnCroppedItemsArea();
            SaveToJPGFile(itemsArea, pathToSave + "/itemsArea.jpg");
            Bitmap processedItemsArea = ImageProcessingLib.PreprocessingForParsing(itemsArea);
            SaveToJPGFile(processedItemsArea, pathToSave + "/processedItemsArea.jpg");
            int numOfItems = templateMatcher.priceAndQuantity.size();
            templateMatcher = new TemplateMatcher(tesseractOCREngine, processedItemsArea);
            templateMatcher.ParsingItemsArea(numOfItems);
            LinkedHashMap ocrResultCroppedBill = GetOcrResults(templateMatcher);
            CompareExpectedToOcrResult(ocrResultCroppedBill, expectedBillTextLines);

            bill.recycle();
            warped.recycle();
            processedBill.recycle();
            processedBillForCreateNewBill.recycle();
            itemsArea.recycle();
            processedItemsArea.recycle();
        }
    }

    /**
     * Save bitmap to jpg file
     * @param bmp bitmap to save
     * @param path path to save to
     * @return
     */
    private boolean SaveToJPGFile(Bitmap bmp, String path){
        FileOutputStream out = null;
        try {
            File file = new File(path);
            if(file.exists()){
                file.delete();
            }
            out = new FileOutputStream(path);

            // bmp is your Bitmap instance, PNG is a lossless format, the compression factor (100) is ignored
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     *
     * @param fileFullName txt file full name on device
     * @return list of string with file lines
     * @throws IOException
     */
    private static List<String> ReadTxtFile(String fileFullName) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileFullName));
        // do reading, usually loop until end of file reading
        List<String> lines = new ArrayList<>();
        String line = bufferedReader.readLine();
        while (line != null) {
            lines.add(line);
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        return lines;
    }

    /**
     *
     * @param tifFilePath path of tif file
     * @return rotated bitmap
     */
    private Bitmap TifToBitmap(String tifFilePath) {
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        bitmapOptions.inMutable = true;
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        TiffBitmapFactory.Options options = new TiffBitmapFactory.Options();
        options.inAvailableMemory = 1024 * 1024 * 10 * 3; //30 mb
        File file = new File(tifFilePath);
        bitmap = TiffBitmapFactory.decodeFile(file, options);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.recycle();

        return rotated;
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
     * Crop the bill from original capture and warp it
     * @param bill original capture of bill
     * @param billFullName bill full name
     * @return warped bill
     */
    private Bitmap CropAndWarpPerspective(Bitmap bill, String billFullName) {
        Point mTopLeft = new Point();
        Point mTopRight = new Point();
        Point mButtomLeft = new Point();
        Point mButtomRight = new Point();
        BillAreaDetector.GetBillCorners(bill , mTopLeft, mTopRight, mButtomRight, mButtomLeft);

        /** Preparing Warp Perspective Dimensions **/
        int newWidth = (int) Math.max(mButtomRight.x - mButtomLeft.x, mTopRight.x - mTopLeft.x);
        int newHeight = (int) Math.max(mButtomRight.y - mTopRight.y, mButtomLeft.y - mTopLeft.y);
        int xBegin = (int) Math.min(mTopLeft.x, mButtomLeft.x);
        int yBegin = (int) Math.min(mTopLeft.y, mTopRight.y);
        Bitmap resizedBitmap = Bitmap.createBitmap(bill, xBegin, yBegin, newWidth, newHeight);
        Bitmap warpedBitmap = Bitmap.createBitmap(newWidth , newHeight, bill.getConfig());
        mTopLeft.x = mTopLeft.x - xBegin;
        mTopLeft.y = mTopLeft.y - yBegin;
        mTopRight.x = mTopRight.x - xBegin;
        mTopRight.y = mTopRight.y - yBegin;
        mButtomRight.x = mButtomRight.x - xBegin;
        mButtomRight.y = mButtomRight.y - yBegin;
        mButtomLeft.x = mButtomLeft.x - xBegin;
        mButtomLeft.y = mButtomLeft.y - yBegin;

        if(!ImageProcessingLib.WarpPerspective(resizedBitmap, warpedBitmap, mTopLeft, mTopRight, mButtomRight, mButtomLeft)) {
            System.out.println("Failed to warp perspective " + billFullName);
            warpedBitmap.recycle();
            resizedBitmap.recycle();
            return null;
        }

        File file = new File(billFullName);
        String warpPathToSave = file.getParent();
        SaveToJPGFile(warpedBitmap, warpPathToSave + "/warped.jpg");
        SaveToTIFFile(warpedBitmap, warpPathToSave + "/warped.tif");
        resizedBitmap.recycle();
        return warpedBitmap;
    }

    /**
     * comparing line to line ocr results of bill vs expected txt file
     * @param ocrResultCroppedBill ocr results of cropped bill
     * @param expectedBillTextLines expected bill lines from txt file
     */
    private void CompareExpectedToOcrResult(LinkedHashMap ocrResultCroppedBill, List<String> expectedBillTextLines) {
        System.out.println("Validating Ocr Result");
        Integer accuracyPercent = Compare(ocrResultCroppedBill, expectedBillTextLines);

        if(ocrResultCroppedBill.size() != expectedBillTextLines.size())
        {
            System.out.println("ocrResultCroppedBill contains "+ ocrResultCroppedBill.size() + " lines, but" +
                    " expectedBillTextLines contains "+ expectedBillTextLines.size()+" lines");
        }

        System.out.println("Accuracy is "+ accuracyPercent+"%");
        System.out.println(System.getProperty("line.separator"));

//        _testsCount++;
//        _testsAccuracyPercentSum += accuracyPercent;
    }

    /**
     * Save bitmap to tif file
     * @param bmp bitmap to save
     * @param path path to save to
     * @return
     */
    private boolean SaveToTIFFile(Bitmap bmp, String path) {
        TiffSaver.SaveOptions options = new TiffSaver.SaveOptions();
        options.author = "aviel";
        options.copyright = "aviel copyright";
        boolean saved = TiffSaver.saveBitmap(path, bmp, options);
        return saved;
    }

    /**
     *
     * @param ocrResult ocr result of bill included price and quantity
     * @param expectedBillTextLines expected bill lines from txt file
     * @return true in case of equal results. false if unequal
     */
    private Integer Compare(LinkedHashMap ocrResult, List<String> expectedBillTextLines) {
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
                System.out.println("line "+ lineNumber +" doesn't exist on ocr results");
                lineNumber++;
                continue;
            }
            Double quantity = (Double)ocrResultLine.get("quantity");
            Integer ocrResultQuantity = quantity.intValue();
            Double ocrResultPrice = (Double)ocrResultLine.get("price");
            if(!expectedPrice.equals(ocrResultPrice))
            {
                System.out.println("line "+lineNumber+" - Price: expected "+expectedPrice+", "+"ocr "+ocrResultPrice);
                ++countInvalids;
            }
            if(!expectedQuantity.equals(ocrResultQuantity))
            {
                System.out.println("line "+lineNumber+" - Quantity: expected "+expectedQuantity+", "+"ocr "+ocrResultQuantity);
                ++countInvalids;
            }
            lineNumber++;
        }
        //calculate the accuracy percent
        accuracyPercent = ((lineNumber*2 - countInvalids)/(lineNumber*2)) * 100;
        return accuracyPercent.intValue();
    }


}

