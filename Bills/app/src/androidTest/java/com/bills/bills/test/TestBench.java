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
    private long _timeMs;

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
        _timeMs = System.currentTimeMillis();
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
                _restaurantsNamesTestFilter = Arrays.asList( "mina", "dovrin1", "pastaMarket3", "pastaMarket2" /*, "pastaMarket1", "dovrin2", "shanan1" , "shanan2"*/);
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

        List<TestBill> threads = new ArrayList();
        int i=0;

        for (Map.Entry<String, List<String>> restaurantBillsPair : specifyBillsByRestaurants.entrySet()) {
            String restaurant = restaurantBillsPair.getKey();
            List<String> bill = restaurantBillsPair.getValue();
            threads.add(i, new TestBill(rootBrandModelDirectory, restaurant, bill.get(0)));

            _testsCount++;

            threads.get(i).start();
            ++i;
        }

        for(int j=0; j<i; ++j) {
           try {
               threads.get(j).join();
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
        }

        Double accuracyPercentTestsBench = (_testsAccuracyPercentSum / (_testsCount*100)) * 100;
        System.out.println("Conclusions:");
        System.out.println("Accuracy of tests bench is "+ accuracyPercentTestsBench.intValue()+"%");
        _timeMs = System.currentTimeMillis() - _timeMs;
        System.out.println("It took " + _timeMs/1000 + " s");
        System.out.println(System.getProperty("line.separator"));
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
}