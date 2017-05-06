package com.bills.bills.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;

import org.beyka.tiffbitmapfactory.TiffBitmapFactory;
import org.beyka.tiffbitmapfactory.TiffSaver;
import org.junit.Test;
import org.opencv.core.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * Created by avielavr on 11/4/2016.
 */
public class TestBench {
    private static final int TEST_EMULATOR = 1;
    private static final int TEST_PHONE = 3;
    Context _context;
    private Double _testsCount = 0.0;
    private Double _testsAccuracyPercentSum = 0.0;

    /*********** Tests Configuration ************/
    /**
     * _testToRun - user must config
     * _restaurantsNamesTestFilter - user can run tests on specific restaurants.
     *                               in case it empty: the test validate all restaurants
     * _billsTestFilter - user can run tests on specific bills.
     *                    in case it empty: the test validate all bills
     * _brandModelDirectoriesTestFilter - RELEVANT JUST FOR EMULATOR TESTS!
     *                                user can run tests oo specific phone directories.
     *                                in case it empty: the test validate phone directories
     */
    private int _testToRun = TEST_PHONE;
    List<String> _restaurantsNamesTestFilter;
    List<String> _billsTestFilter;
    List<String> _brandModelDirectoriesTestFilter;
    /***************** End **********************/

    /**
     * Main Tests Function
     * @throws Exception
     */
    @Test
    public void begin() throws Exception {
        _context = getInstrumentation().getContext();
        SetOutputStream();
        String sourceDirectory;
        //copy images to internal memory
        //if(PreparingEnvironmentUtil.IsRunningOnEmulator(Build.MANUFACTURER, Build.MODEL))
        //{
        //   PreparingEnvironmentUtil.PrepareTesseract(_context);
        //   PreparingEnvironmentUtil.PrepareImagesForTests(_context);
        //}
        switch(_testToRun)
        {
            case TEST_EMULATOR:
                _restaurantsNamesTestFilter = Arrays.asList( "dovrin"/*, "nili", "zanzara"*/);
                _billsTestFilter = Arrays.asList("29112016_2246_croppedCenter.jpg");
                _brandModelDirectoriesTestFilter = Arrays.asList("samsung_GT-I9300"/*, "samsung_GT-I9070"*/);

                //in case it's empty, test run on all phone directories. it get all names from assets
                List<String> brandModelDirectoriesToTest = !_brandModelDirectoriesTestFilter.isEmpty() ?
                        _brandModelDirectoriesTestFilter :
                        Arrays.asList(_context.getAssets().list("billsToTest"));
                ForeachValidateResults(brandModelDirectoriesToTest);
                break;
            case TEST_PHONE:
                _restaurantsNamesTestFilter = Arrays.asList( "mina" /*, "dovrin1", "pastaMarket1", "pastaMarket2", "pastaMarket3"*/ /*"dovrin2",*/  /*, "shanan1" , "shanan2"*/);
                _billsTestFilter = Arrays.asList(/* "12112016_1355_croppedCenter.jpg" */);
                sourceDirectory = Constants.TESSERACT_SAMPLE_DIRECTORY + Build.BRAND + "_" + Build.MODEL +"/";
                ValidateOcrResultsOfBrandModelBills(_restaurantsNamesTestFilter, _billsTestFilter, sourceDirectory);
                break;
            default:
                throw new Exception("Unknown case !!!");
        }
    }

    /**
     * foreach directories and validate bills
     * @param brandModelDirectoriesToTest phones names directories to run tests on
     * @throws Exception
     */
    private void ForeachValidateResults(List<String> brandModelDirectoriesToTest) throws Exception {
        for (String brandModelDirectoryName : brandModelDirectoriesToTest) {
            String rootBrandModelDirectory = Constants.TESSERACT_SAMPLE_DIRECTORY + brandModelDirectoryName+ "/";
            ValidateOcrResultsOfBrandModelBills(_restaurantsNamesTestFilter, _billsTestFilter, rootBrandModelDirectory);
        }
    }

    /**
     * this is the main test function. it get the current brand-model directory and validate ocr results
     * for all bills by default. user can use filters to run just specific.
     * @param restaurantsNamesTestFilter user can run tests on specific restaurants. in case it empty: the test validate all restaurants
     * @param billsTestFilter user can run tests on specific bills. in case it empty: the test validate all bills
     * @param rootBrandModelDirectory root directory of current brand-model bills
     * @throws Exception
     */
    private void ValidateOcrResultsOfBrandModelBills(List<String> restaurantsNamesTestFilter, List<String> billsTestFilter, String rootBrandModelDirectory) throws Exception{
        File brandModelRootDirectory = new File(rootBrandModelDirectory);
        List<File> bills = GetBills(brandModelRootDirectory, restaurantsNamesTestFilter);
        bills = FilterBills(bills, billsTestFilter);
        HashMap<String, List<String>> specifyBillsByRestaurants =
                SpecifyBillsByRestaurants(bills, brandModelRootDirectory);
        TemplateMatcher templateMatcher;
        TesseractOCREngine tesseractOCREngine;
        tesseractOCREngine = new TesseractOCREngine();
        try {
            tesseractOCREngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, List<String>> restaurantBillsPair : specifyBillsByRestaurants.entrySet()) {
            String currRestaurant = restaurantBillsPair.getKey();
            List<String> currBill = restaurantBillsPair.getValue();
            String expectedTxtFileName = restaurantBillsPair.getKey().toString() + ".txt";
            List<String> expectedBillTextLines = ReadTxtFile(rootBrandModelDirectory + currRestaurant + "/" + expectedTxtFileName);

            System.out.println("Test of " + currBill.get(0));
            Bitmap bill = TifToBitmap(currBill.get(0));
            Bitmap warped = CropAndWarpPerspective(bill, currBill.get(0));
            Bitmap processedBill = ImageProcessingLib.PreprocessingForTemplateMatcher(warped);
            File file = new File(currBill.get(0));
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

        Double accuracyPercentTestsBench = (_testsAccuracyPercentSum / (_testsCount*100)) * 100;
        System.out.println("Conclusions:");
        System.out.println("Accuracy of tests bench is "+ accuracyPercentTestsBench.intValue()+"%");
        System.out.println(System.getProperty("line.separator"));
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
        _testsCount++;
        _testsAccuracyPercentSum += accuracyPercent;
    }

    /**
     *
     * @param croppedBills cropped bills of brand-model
     * @param brandModelRootDirectory root directory of brand-model
     * @return hash map which contains restaurants names as keys, and path to files as values
     */
    private HashMap<String,List<String>> SpecifyBillsByRestaurants(List<File> croppedBills, File brandModelRootDirectory) {
        HashMap<String, List<String>> specifyCroppedBillsByRestaurants = new HashMap<>();

        for (File file: croppedBills) {
            String restaurant = file.getParent().split(brandModelRootDirectory.getName())[1].substring(1);
            if(!specifyCroppedBillsByRestaurants.containsKey(restaurant))
            {
                specifyCroppedBillsByRestaurants.put(restaurant, new ArrayList<String>());
            }
            specifyCroppedBillsByRestaurants.get(restaurant).add(file.getAbsolutePath());
        }

        return specifyCroppedBillsByRestaurants;
    }

    /**
     *
     * @param croppedBills all cropped bills of root directory
     * @param billsTestFilter see _billsTestFilter
     * @return filtered bills
     */
    private List<File> FilterBills(List<File> croppedBills, List<String> billsTestFilter) {
        List<File> filteredBills = new ArrayList<>();
        if(billsTestFilter.size() != 0)
        {
            for (File file : croppedBills) {
                if (billsTestFilter.contains(file.getName())) {
                    filteredBills.add(file);
                }
            }
        }
        return croppedBills;
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
     * It return a list of files from parent directory after filtering
     * @param parentDirectory root directory for beginning
     * @param restaurantName for specific filtering. It return all restaurants bills in case of empty
     * @return Specific bills by restaurantName list. All restaurants in case of empty
     */
    private List<File> GetBills(File parentDirectory, List<String> restaurantName) {
        ArrayList<File> files = new ArrayList<>();
        File[] filesParentDirectory = parentDirectory.listFiles();
        for (File file : filesParentDirectory) {
            if (file.isDirectory()) {
                files.addAll(GetBills(file, restaurantName));
            } else {
                if(file.getName().endsWith(".tif") &&
                        file.getName().contains("ocr") &&
                        IsFileNameContainAnyRestaurants(file.getPath(), restaurantName))
                    files.add(file);
            }
        }
        return files;
    }

    /**
     * Filter for GetBills function.
     *
     * @param name current restaurant name
     * @param restaurantsNames restaurant name to filter by
     * @return true or false due to restaurant name existence
     */
    private boolean IsFileNameContainAnyRestaurants(String name, List<String> restaurantsNames) {
        if(restaurantsNames.isEmpty())
        {
            //validate all restaurants in case of restaurantsNames is empty
            return true;
        }

        for (String restaurant: restaurantsNames) {
            if(name.contains(restaurant))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Set output stream for 'System.out.println'. The test prints just to file.
     * Read TEST_README for more info
     * @throws FileNotFoundException
     */
    private void SetOutputStream() throws FileNotFoundException {
        File file = new File(Constants.TEST_OUTPUT_FILE);
        PrintStream printStreamToFile = new PrintStream(file);
        System.setOut(printStreamToFile);
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
}