package com.bills.bills.test;

import android.content.Context;
import android.os.Build;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Utilities.FilesHandler;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

/**
 * Created by avielavr on 11/4/2016.
 */
public class TestBench {
    private static final int TEST_EMULATOR = 1;
    private static final int TEST_PHONE = 3;
    Context _context;
    private Double _testsAccuracyPercentSum = 0.0;
    private long _timeMs;

    /*********** Thread Pool Configuration ************/
    private static int NUMBER_OF_CORES = 4;//Runtime.getRuntime().availableProcessors();
    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1000;
    // Sets the Time Unit to Milliseconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.MILLISECONDS;
    /***************** End **********************/

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
                _restaurantsNamesTestFilter = Arrays.asList(/**/ "mina1", "pastaMarket1", "pastaMarket2", "iza1", "dovrin1", "dovrin2", "nola1", "nola2", "nola3");
                _billsTestFilter = Arrays.asList(/*ocrBytes3.txt"*/);
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
        Queue<Double> accuracyPercentQueue = new ConcurrentLinkedQueue<>();
        Queue<StringBuilder> passedResultsQueue = new ConcurrentLinkedQueue<>();
        Queue<StringBuilder> failedResultsQueue = new ConcurrentLinkedQueue<>();

        ThreadPoolExecutor mThreadPoolExecutor = new ThreadPoolExecutor(
                NUMBER_OF_CORES,  // Initial pool size
                NUMBER_OF_CORES,  // Max pool size
                KEEP_ALIVE_TIME,  // Time idle thread waits before terminating
                KEEP_ALIVE_TIME_UNIT,  // Sets the Time Unit for KEEP_ALIVE_TIME
                new LinkedBlockingDeque<Runnable>());  // Work Queue

        for (Map.Entry<String, List<String>> restaurantBillsPair : specifyBillsByRestaurants.entrySet()) {
            String restaurant = restaurantBillsPair.getKey();
            List<String> currBills = restaurantBillsPair.getValue();
            for(int i=0; i < currBills.size(); i++)
            {
                mThreadPoolExecutor.execute(
                        new TestBill(rootBrandModelDirectory, restaurant, currBills.get(i),
                                accuracyPercentQueue, passedResultsQueue, failedResultsQueue));
            }
        }

        mThreadPoolExecutor.shutdown();
        mThreadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        /**************** Write failed tests output to file ***************/
        File failedTestsFile = new File(Constants.FAILED_TEST_OUTPUT_FILE);
        FileOutputStream stream = new FileOutputStream(failedTestsFile);
        for(StringBuilder sb : failedResultsQueue){
            stream.write(sb.toString().getBytes());
        }
        stream.close();
        /******************************* END ******************************/

        /**************** Write passed tests output and conclusions to file ***************/
        File passedTestsFile = new File(Constants.TEST_OUTPUT_FILE);
        stream = new FileOutputStream(passedTestsFile);
        for(Double item : accuracyPercentQueue){
            _testsAccuracyPercentSum+=item;
        }
        Double accuracyPercentTestsBench = (_testsAccuracyPercentSum / (accuracyPercentQueue.size()*100)) * 100;
        String formattedAccuracyPercentTestsBench = String.format("%.02f", accuracyPercentTestsBench);
        stream.write(("\nConclusions:").getBytes());
        stream.write(("\nAccuracy of tests bench is "+ formattedAccuracyPercentTestsBench +"%").getBytes());
        stream.write(("\nTotally run " + (passedResultsQueue.size() + failedResultsQueue.size()) + " bills").getBytes());
        stream.write(("\n" + passedResultsQueue.size() + " passed").getBytes());
        stream.write(("\n" + failedResultsQueue.size() + " failed").getBytes());
        _timeMs = System.currentTimeMillis() - _timeMs;
        stream.write(("\nIt took " + _timeMs/1000 + " s\n").getBytes());

        for(StringBuilder sb : passedResultsQueue){
            stream.write(sb.toString().getBytes());
        }
        stream.close();
        /******************************* END ******************************/
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
            return filteredBills;
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
                if(file.getName().endsWith(".txt") &&
                        file.getName().contains("ocrBytes") &&
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
}