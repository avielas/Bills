package com.bills.testsdebug;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import com.bills.billslib.Contracts.*;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;
import com.bills.billslib.CustomViews.DragRectView;
import com.bills.billslib.Utilities.FilesHandler;
import com.gregacucnik.EditableSeekBar;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import uk.co.senab.photoview.PhotoViewAttacher;

import static android.view.View.GONE;

public class TestsDebugActivity extends AppCompatActivity implements View.OnClickListener{
    private enum StructureElement {
        NONE,
        HORIZONTAL_LINE,
        VERTICAL_LINE,
        RECTANGULAR,
        ELLIPTICAL,
        CROSS_SHAPED
    }
    TesseractOCREngine tesseractOCREngine;
    String _billName;
    String _restaurantName;
    String _brandAndModelPath;
    String _expectedTxtFileName;
    EditableSeekBar _adaptiveThresholdBlockSizeSeekBar;
    EditableSeekBar _adaptiveThresholdConstantSubtractedSeekBar;
    EditableSeekBar _dilateKernelSizeSeekBar;
    EditableSeekBar _erodeKernelSizeSeekBar;
    PhotoViewAttacher _photoViewAttacher;
    Bitmap _billWithPrintedRedLines;
    Bitmap _bill;
    Bitmap _processedBill;
    Bitmap _processedBillForCreateNewBill;
    Mat _rgba;
    Mat _gray;
    ImageView _processedImageView;
    ImageView _processedForCreateNewBillImageView;
    ImageView _originalImageView;
    Button _ocrOnPreprocessedButton;
    Button _templateMatcherButton;
    Button _printWordsLocationButton;
    Button _saveProccessedButton;
    Button _generateBillButton;
    Button _adaptiveThresholdButton;
    Button _dilateButton;
    Button _erodeButton;
    EditText _adaptiveThresholdIterationsEditText;
    EditText _dilateIterationsEditText;
    EditText _erodeIterationsEditText;
    StringBuilder _results;
    StringBuilder _algorithmsTracing;
    Spinner _kernelTypeSpinner;
    List<String> _kernelTypes;
    private final int BILLS_REQUEST_CODE = 1;
    TemplateMatcher templateMatcher;
    private Button mUserCropFinished;
    private DragRectView mDragRectView;
    private Point mTopLeft = new Point();
    private Point mTopRight = new Point();
    private Point mButtomLeft = new Point();
    private Point mButtomRight = new Point();
    private LinearLayout _testsDebugView;
    private RelativeLayout _dragRectView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tests_debug_view);
        _testsDebugView = (LinearLayout)findViewById(R.id.tests_debug_view);
        _dragRectView = (RelativeLayout)findViewById(R.id.drag_rect_view);
        _dragRectView.setVisibility(GONE);
//        PreparingEnvironmentUtil.PrepareTesseract(this);
        //copy images to internal memory just in case of emulator
//        if(PreparingEnvironmentUtil.IsRunningOnEmulator(Build.MANUFACTURER, Build.MODEL))
//        {
//            PreparingEnvironmentUtil.PrepareImagesForTests(this);
//        }
        _billName = "29112016_2246_croppedCenter.jpg";
        _restaurantName = "mina";
        _brandAndModelPath = Constants.TESSERACT_SAMPLE_DIRECTORY + Build.BRAND + "_" + Build.MODEL;
        //extract date from bill, for creating expected txt file name
        _expectedTxtFileName = _restaurantName + ".txt";
        _originalImageView = (ImageView)findViewById(R.id.originalImageView);
        _processedImageView = (ImageView)findViewById(R.id.processedImageView);
        _processedForCreateNewBillImageView = (ImageView)findViewById(R.id.processedForCreateNewBillImageView);
        _adaptiveThresholdBlockSizeSeekBar =(EditableSeekBar) findViewById(R.id.adaptiveThresholdBlockSizeEditableSeekBar);
        _adaptiveThresholdConstantSubtractedSeekBar =(EditableSeekBar) findViewById(R.id.adaptiveThresholdConstantSubtractedEditableSeekBar);
        _dilateKernelSizeSeekBar =(EditableSeekBar) findViewById(R.id.dilateEditableSeekBar);
        _erodeKernelSizeSeekBar =(EditableSeekBar) findViewById(R.id.erodeEditableSeekBar);
        _ocrOnPreprocessedButton = (Button) findViewById(R.id.runOcrOnPreprocessedButton);
        _templateMatcherButton = (Button) findViewById(R.id.templateMatcherButton);
        _printWordsLocationButton = (Button) findViewById(R.id.printWordsLocationButton);
        _saveProccessedButton = (Button) findViewById(R.id.saveProccessedButton);
        _generateBillButton =  (Button) findViewById(R.id.generateBillButton);
        _adaptiveThresholdButton = (Button) findViewById(R.id.adaptiveThresholdBlockSizeButton);
        _dilateButton = (Button) findViewById(R.id.dilateButton);
        _erodeButton = (Button) findViewById(R.id.erodeButton);
        _adaptiveThresholdIterationsEditText = (EditText) findViewById(R.id.adaptiveThresholdIterationsNumber);
        _adaptiveThresholdIterationsEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        _dilateIterationsEditText = (EditText) findViewById(R.id.dilateIterationsNumber);
        _dilateIterationsEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        _erodeIterationsEditText = (EditText) findViewById(R.id.erodeIterationsNumber);
        _erodeIterationsEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        _results = new StringBuilder();
        _algorithmsTracing = new StringBuilder();
        _algorithmsTracing.append("Algorithms Tracing:");
        _algorithmsTracing.append(System.getProperty("line.separator"));
        _kernelTypeSpinner = (Spinner) findViewById(R.id.kernelTypeSpinner);
        _kernelTypes = new ArrayList<>();
        _kernelTypes.add(StructureElement.NONE.toString());
        _kernelTypes.add(StructureElement.HORIZONTAL_LINE.toString());
        _kernelTypes.add(StructureElement.VERTICAL_LINE.toString());
        _kernelTypes.add(StructureElement.RECTANGULAR.toString());
        _kernelTypes.add(StructureElement.ELLIPTICAL.toString());
        _kernelTypes.add( StructureElement.CROSS_SHAPED.toString());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, _kernelTypes);
        _kernelTypeSpinner.setAdapter(adapter);

        if (!OpenCVLoader.initDebug()) {
            Log.d("aa", "Failed to initialize OpenCV.");
        }

        _rgba = new Mat();
        _gray = new Mat();
        try {
            _bill = InitBillFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            _billWithPrintedRedLines = InitBillFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        _processedBill = Bitmap.createBitmap(_bill.getWidth(), _bill.getHeight(), Bitmap.Config.ARGB_8888);
        _processedBillForCreateNewBill = Bitmap.createBitmap(_bill.getWidth(), _bill.getHeight(), Bitmap.Config.ARGB_8888);

        //Show original image on ImageView
        _originalImageView.setImageBitmap(_bill);
        _photoViewAttacher = new PhotoViewAttacher(_originalImageView);

        PreprocessingForTemplateMatcher();
        AddListenerOcrOnPreprocessedButton();
        AddListenerSaveProccessedButton();
        AddListenerGenerateBillButton();
        AddListenerTemplateMatcherButton();
        AddListenerPrintWordsLocationButton();
        AddListenerAdaptiveThresholdButton();
        AddListenerDilateButton();
        AddListenerErodeButton();
        tesseractOCREngine = new TesseractOCREngine();
        try {
            tesseractOCREngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void PreprocessingForTemplateMatcher() {
        _processedBill = ImageProcessingLib.PreprocessingForTemplateMatcher(_bill);
        _processedImageView.setImageBitmap(_processedBill);
        _processedBillForCreateNewBill = ImageProcessingLib.PreprocessingForParsingBeforeTM(_bill);
        _processedForCreateNewBillImageView.setImageBitmap(_processedBillForCreateNewBill);
        _photoViewAttacher = new PhotoViewAttacher(_processedForCreateNewBillImageView);
    }

    private void PreprocessingForParsing() {
//        Utils.bitmapToMat(_bill, _rgba);
//        AdaptiveThreshold(_rgba, 100, 33.0);
//        Utils.matToBitmap(_rgba, _processedBill);
        _originalImageView.setImageBitmap(_bill);
        _photoViewAttacher = new PhotoViewAttacher(_originalImageView);
        _processedImageView.setImageBitmap(_processedBill);
        _photoViewAttacher = new PhotoViewAttacher(_processedImageView);
    }

    private Bitmap InitBillFromFile() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
//        String billPath = _brandAndModelPath + _restaurantName + _billName;
        return GetLastWarpedBillPhoto(); //BitmapFactory.decodeFile(billPath, options); //
    }

    public void AddListenerAdaptiveThresholdButton() {
        _adaptiveThresholdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    _algorithmsTracing.setLength(0);
                    _algorithmsTracing.append("Algorithms Tracing:");
                    _algorithmsTracing.append(System.getProperty("line.separator"));
                    int blockSize = _adaptiveThresholdBlockSizeSeekBar.getValue();
                    int constantSubtracted = _adaptiveThresholdConstantSubtractedSeekBar.getValue();
                    /*** convert block size to odd number according to opencv specs ***/
                    int blockSizeToOddNumber = blockSize%2 == 0 ? blockSize-1 : blockSize;
                    /****************/
                    _rgba.release();
                    _rgba = new Mat();
                    Utils.bitmapToMat(_bill, _rgba);
                    AdaptiveThreshold(_rgba, blockSizeToOddNumber, constantSubtracted);
                    Utils.matToBitmap(_rgba, _processedBill);
                    _processedImageView.setImageBitmap(_processedBill);
                    _photoViewAttacher = new PhotoViewAttacher(_processedImageView);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void AdaptiveThreshold(Mat rgba, int blockSize, double C){
//        Utils.bitmapToMat(_bill, _rgba);
        ImageProcessingLib.AdaptiveThreshold(rgba, blockSize, C);
//        Utils.matToBitmap(_rgba, _processedBill);
        _algorithmsTracing.append("AdaptiveThreshold: block size " + blockSize + ", constant subtracted " + C);
        _algorithmsTracing.append(System.getProperty("line.separator"));
    }

    private void ValidateOcrBillResult(String imageStatus, Bitmap billBitmap) throws Exception{
        List<String> expectedBillTextLines = ReadTxtFile(_brandAndModelPath + "/" +_restaurantName + "/" + _expectedTxtFileName);
        _results.append("Test of " + imageStatus + " " + _restaurantName);
        _results.append(System.getProperty("line.separator"));
        LinkedHashMap ocrResultCroppedBill = GetOcrResults();
        CompareExpectedToOcrResult(ocrResultCroppedBill, expectedBillTextLines);
    }

    private LinkedHashMap GetOcrResults() {
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
     * comparing line to line ocr results of bill vs expected txt file
     * @param ocrResultCroppedBill ocr results of cropped bill
     * @param expectedBillTextLines expected bill lines from txt file
     */
    private void CompareExpectedToOcrResult(LinkedHashMap ocrResultCroppedBill, List<String> expectedBillTextLines) {
        _results.append("Validating Ocr Result:");
        _results.append(System.getProperty("line.separator"));
        Double accuracyPercent = Compare(ocrResultCroppedBill, expectedBillTextLines);
        _results.append(System.getProperty("line.separator"));
        if(ocrResultCroppedBill.size() != expectedBillTextLines.size())
        {
            _results.append("ocrResultCroppedBill contains "+ ocrResultCroppedBill.size() + " lines, but" +
                    " expectedBillTextLines contains "+ expectedBillTextLines.size()+" lines");
        }
//        PrintParsedNumbers(ocrResultCroppedBill);
        _results.append("Accuracy is "+ accuracyPercent+"%");
        _results.append(System.getProperty("line.separator"));
        _results.append(System.getProperty("line.separator"));
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
                _results.append("line "+ lineNumber +" doesn't exist on ocr results");
                _results.append(System.getProperty("line.separator"));
                lineNumber++;
                continue;
            }
            Double quantity = (Double)ocrResultLine.get("quantity");
            Integer ocrResultQuantity = quantity.intValue();
            Double ocrResultPrice = (Double)ocrResultLine.get("price");
            if(!expectedPrice.equals(ocrResultPrice))
            {
                _results.append("line "+lineNumber+" - Price: expected "+expectedPrice+", "+"ocr "+ocrResultPrice);
                _results.append(System.getProperty("line.separator"));
                ++countInvalids;
            }
            if(!expectedQuantity.equals(ocrResultQuantity))
            {
                _results.append("line "+lineNumber+" - Quantity: expected "+expectedQuantity+", "+"ocr "+ocrResultQuantity);
                _results.append(System.getProperty("line.separator"));
                ++countInvalids;
            }
            lineNumber++;
        }
        //calculate the accuracy percent
        accuracyPercent = ((lineNumber*2 - countInvalids)/(lineNumber*2)) * 100;
        return accuracyPercent;
    }

    private void PrintParsedNumbers(LinkedHashMap ocrResult) {
        int lineNumber = 0;

        for (int i=0; i < ocrResult.size(); i++)
        {
            HashMap ocrResultLine = (HashMap)ocrResult.get(lineNumber);

            Double quantity = (Double)ocrResultLine.get("quantity");
            Integer ocrResultQuantity = quantity.intValue();
            Double ocrResultPrice = (Double)ocrResultLine.get("price");

            _results.append("line "+lineNumber+" - Price: "+ocrResultPrice+", Quantity: "+ocrResultQuantity);
            _results.append(System.getProperty("line.separator"));
            lineNumber++;
        }
    }

    public void AddListenerDilateButton() {
        _dilateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {

                    int iterations = Integer.parseInt(_dilateIterationsEditText.getText().toString());
                    int kernelSize = _dilateKernelSizeSeekBar.getValue();
                    String selectedStructureElement = _kernelTypeSpinner.getSelectedItem().toString();
                    Utils.bitmapToMat(_processedBill, _rgba);
                    ImageProcessingLib.Dilate(_rgba, iterations, kernelSize, selectedStructureElement);
                    Utils.matToBitmap(_rgba, _processedBill);
                    _algorithmsTracing.append("Dilate: iterations " + iterations + ", kernel size " + kernelSize);
                    _algorithmsTracing.append(System.getProperty("line.separator"));
                    _processedImageView.setImageBitmap(_processedBill);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void AddListenerErodeButton() {
        _erodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    int iterations = Integer.parseInt(_erodeIterationsEditText.getText().toString());
                    int kernelSize = _erodeKernelSizeSeekBar.getValue();
                    String selectedStructureElement = _kernelTypeSpinner.getSelectedItem().toString();
                    Utils.bitmapToMat(_processedBill, _rgba);
                    ImageProcessingLib.Erode(_rgba, iterations, kernelSize, selectedStructureElement);
                    Utils.matToBitmap(_rgba, _processedBill);
                    _algorithmsTracing.append("Erode: iterations " + iterations + ", kernel size " + kernelSize);
                    _algorithmsTracing.append(System.getProperty("line.separator"));
                    _processedImageView.setImageBitmap(_processedBill);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void AddListenerOcrOnPreprocessedButton() {
        _ocrOnPreprocessedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    _results.setLength(0);
                    int numOfItems = templateMatcher.priceAndQuantity.size();
                    templateMatcher = null;
                    templateMatcher = new TemplateMatcher(tesseractOCREngine, _processedBill);
                    templateMatcher.ParsingItemsArea(numOfItems);
                    //ValidateOcrBillResult("Original", _bill);
                    ValidateOcrBillResult("Processed", _processedBill);
                    OpenUserInputDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void AddListenerTemplateMatcherButton() {
        _templateMatcherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    PreprocessingForTemplateMatcher();
                    templateMatcher = new TemplateMatcher(tesseractOCREngine,_processedBillForCreateNewBill, _processedBill);
                    Bitmap matched = templateMatcher.MatchWhichReturnCroppedItemsArea();
                    _bill.recycle();
                    _processedBill.recycle();
                    _bill = matched.copy(matched.getConfig(), true);
                    matched.recycle();
                    _processedBill = _bill.copy(_bill.getConfig(), true);
                    PreprocessingForParsing();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void AddListenerPrintWordsLocationButton() {
        _printWordsLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    _billWithPrintedRedLines.recycle();
                    String pathToSave = Constants.IMAGES_PATH;
                    FilesHandler.SaveToJPGFile(_bill, pathToSave + "/bill.jpg");
                    FilesHandler.SaveToJPGFile(_processedBill, pathToSave + "/processedBill.jpg");
                    _billWithPrintedRedLines = PrintWordsRects(_bill, _processedBill);
                    _originalImageView.setImageBitmap(_billWithPrintedRedLines);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void AddListenerSaveProccessedButton() {
        _saveProccessedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
//                    String imagePathToSave = _brandAndModelPath + _restaurantName
//                                            + "nili_24_2_17";
//
//                    WriteCroppedImageToTIFFile(_bill, imagePathToSave);
                    String processedImagePathToSave = _brandAndModelPath + _restaurantName
                            + "processed_nili_24_2_17";

//                    WriteCroppedImageToTIFFile(_billWithPrintedRedLines, processedImagePathToSave);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void AddListenerGenerateBillButton() {
        _generateBillButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    RunBillsMainFlow(BILLS_REQUEST_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void RunBillsMainFlow(int requestCode) {
        try {
            String imagePathToSave = Constants.CAMERA_CAPTURED_TXT_PHOTO_PATH;
            Intent intent = new Intent(getBaseContext(), CameraActivity.class);
            intent.putExtra(CameraActivity.BILLS_CROPPED_PHOTO_EXTRA_NAME, imagePathToSave);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, requestCode);
            }
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void OpenUserInputDialog() throws FileNotFoundException {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
        View mView = layoutInflaterAndroid.inflate(R.layout.user_input_dialog_box, null);
        TextView textView = (TextView)mView.findViewById(R.id.testOutput);
        textView.setMovementMethod(new ScrollingMovementMethod());
        _results.append(System.getProperty("line.separator"));
        _results.append(_algorithmsTracing);
        textView.setText(_results);
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(this);
        alertDialogBuilderUserInput.setView(mView);
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton("Export to file", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        // ToDo get user input here
                        try {
                            SetOutputStreamToFile();
                            System.out.println(_results);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });
        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.show();
    }

    /**
     * Set output stream for 'System.out.println'. The test prints just to file.
     * Read TEST_README for more info
     * @throws FileNotFoundException
     */
    private void SetOutputStreamToFile() throws FileNotFoundException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
        String currentDateAndTime = simpleDateFormat.format(new Date());
        File file = new File(_brandAndModelPath + _restaurantName + "/preprocessing_results_" + currentDateAndTime +".txt");
        PrintStream printStreamToFile = new PrintStream(file);
        System.setOut(printStreamToFile);
    }

    private Bitmap GetLastWarpedBillPhoto() throws IOException {
//        byte[] bytes = FilesHandler.ReadFromTXTFile(Constants.WARPED_TXT_PHOTO_PATH);
        byte[] bytes = FilesHandler.ReadFromTXTFile(Constants.CAMERA_CAPTURED_TXT_PHOTO_PATH);
//        Bitmap bitmapWarped = FilesHandler.ByteArrayToBitmap(bytesWarped);
        Bitmap bitmap = FilesHandler.ByteArrayToBitmap(bytes);
        bitmap = FilesHandler.Rotating(bitmap);
//        Bitmap bitmap = FilesHandler.GetBitmapFromTifFile();
        return bitmap;
    }

    public Bitmap PrintWordsRects(Bitmap bitmap, Bitmap _processedBill){
        List<Rect> words;
        Bitmap printedBitmap = Bitmap.createBitmap(bitmap);
        try{
            tesseractOCREngine.SetImage(_processedBill);
            List<Rect> lineRects = tesseractOCREngine.GetTextlines();

            /************ the following is waiting for GC to finish his job. ********/
            /************ without it the red lines will not be printed. *************/
//            Thread.sleep(50);
            /**************************/
            Paint paint = new Paint();
            Canvas canvas = new Canvas(printedBitmap);
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);

            for (Rect line: lineRects) {
                tesseractOCREngine.SetRectangle(line);
                words = tesseractOCREngine.GetWords();
                for (Rect rect : words) {
                    canvas.drawRect(rect, paint);
                }
            }
        }
        catch(Exception ex){
            Log.d(this.getClass().getSimpleName(), "Failed to map numbered values to location. Error: " + ex.getMessage());
            return null;
        }
            return printedBitmap;
        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (resultCode) {
            case RESULT_OK:
                _bill.recycle();
                try {
                    _bill = GetLastWarpedBillPhoto();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mUserCropFinished = new Button(this);
                mUserCropFinished.setText("Done");
                mUserCropFinished.setOnClickListener(this);
                mDragRectView = new DragRectView(this);
                BillAreaDetector areaDetector = new BillAreaDetector();

                if (!areaDetector.GetBillCorners(_bill , mTopLeft, mTopRight, mButtomRight, mButtomLeft)) {
                    Log.d(this.getClass().getSimpleName(), "Failed ot get bounding rectangle automatically.");
                    mDragRectView.TopLeft = null;
                    mDragRectView.TopRight = null;
                    mDragRectView.ButtomLeft = null;
                    mDragRectView.ButtomRight = null;
                }
                else {
                    int x = (int) Math.round((720.0/_bill.getWidth())*mTopLeft.x);
                    int y = (int) Math.round((1118.0/_bill.getHeight())*mTopLeft.y);
                    mDragRectView.TopLeft = new android.graphics.Point(x, y);

                    x = (int) Math.round((720.0/_bill.getWidth())*mTopRight.x);
                    y = (int) Math.round((1118.0/_bill.getHeight())*mTopRight.y);
                    mDragRectView.TopRight = new android.graphics.Point(x, y);

                    x = (int) Math.round((720.0/_bill.getWidth())*mButtomRight.x);
                    y = (int) Math.round((1118.0/_bill.getHeight())*mButtomRight.y);
                    mDragRectView.ButtomRight = new android.graphics.Point(x, y);

                    x = (int) Math.round((720.0/_bill.getWidth())*mButtomLeft.x);
                    y = (int) Math.round((1118.0/_bill.getHeight())*mButtomLeft.y);
                    mDragRectView.ButtomLeft = new android.graphics.Point(x, y);
                }

                BitmapDrawable bitmapDrawable = new BitmapDrawable(_bill);
                mDragRectView.setBackground(bitmapDrawable);
                mDragRectView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.BOTTOM;
                mUserCropFinished.setLayoutParams(params);
                _testsDebugView.setVisibility(GONE);
                _dragRectView.setVisibility(View.VISIBLE);
                _dragRectView.addView(mDragRectView);
                _dragRectView.addView(mUserCropFinished);
                break;
            default:
                //mBeginBillSplitFlowButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {

        if(v == mUserCropFinished){
            double stretchFactorX = (1.0 * _bill.getWidth()) / mDragRectView.getBackground().getBounds().width();
            double stretchFactorY = (1.0 * _bill.getHeight()) / mDragRectView.getBackground().getBounds().height();

            double x = mDragRectView.TopLeft.x * stretchFactorX;
            double y = mDragRectView.TopLeft.y * stretchFactorY ;
            mTopLeft = new Point(x,y);

            x = mDragRectView.TopRight.x * stretchFactorX;
            y = mDragRectView.TopRight.y * stretchFactorY;
            mTopRight = new Point(x,y);

            x = mDragRectView.ButtomLeft.x * stretchFactorX;
            y = mDragRectView.ButtomLeft.y * stretchFactorY;
            mButtomLeft= new Point(x,y);

            x = mDragRectView.ButtomRight.x * stretchFactorX;
            y = mDragRectView.ButtomRight.y * stretchFactorY;
            mButtomRight = new Point(x,y);

            /** Preparing Warp Perspective Dimensions **/
            int newWidth = (int) Math.max(mButtomRight.x - mButtomLeft.x, mTopRight.x - mTopLeft.x);
            int newHeight = (int) Math.max(mButtomRight.y - mTopRight.y, mButtomLeft.y - mTopLeft.y);
            int xBegin = (int) Math.min(mTopLeft.x, mButtomLeft.x);
            int yBegin = (int) Math.min(mTopLeft.y, mTopRight.y);
            Bitmap resizedBitmap = Bitmap.createBitmap(_bill, xBegin, yBegin, newWidth, newHeight);
            Bitmap warpedBitmap = Bitmap.createBitmap(newWidth , newHeight, _bill.getConfig());
            mTopLeft.x = mTopLeft.x - xBegin;
            mTopLeft.y = mTopLeft.y - yBegin;
            mTopRight.x = mTopRight.x - xBegin;
            mTopRight.y = mTopRight.y - yBegin;
            mButtomRight.x = mButtomRight.x - xBegin;
            mButtomRight.y = mButtomRight.y - yBegin;
            mButtomLeft.x = mButtomLeft.x - xBegin;
            mButtomLeft.y = mButtomLeft.y - yBegin;

            if(!ImageProcessingLib.WarpPerspective(resizedBitmap, warpedBitmap, mTopLeft, mTopRight, mButtomRight, mButtomLeft)) {
                Log.d(this.getClass().getSimpleName(), "Failed to warp perspective");
                resizedBitmap.recycle();
                warpedBitmap.recycle();
                return;
            }
            resizedBitmap.recycle();
            FilesHandler.SaveToJPGFile(warpedBitmap, Constants.WARPED_JPG_PHOTO_PATH);
            _billWithPrintedRedLines.recycle();
            _processedBill.recycle();
            _processedBillForCreateNewBill.recycle();
            _bill = warpedBitmap.copy(warpedBitmap.getConfig(), true);
            _billWithPrintedRedLines = _bill.copy(_bill.getConfig(), true);
//                _processedBill = Bitmap.createBitmap(_bill.getWidth(), _bill.getHeight(), Bitmap.Config.ARGB_8888);
//                _processedBillForCreateNewBill = Bitmap.createBitmap(_bill.getWidth(), _bill.getHeight(), Bitmap.Config.ARGB_8888)
            _dragRectView.removeView(mDragRectView);
            _dragRectView.removeView(mUserCropFinished);
            _dragRectView.setVisibility(GONE);
            _testsDebugView.setVisibility(View.VISIBLE);
            _originalImageView.setImageBitmap(_bill);
            warpedBitmap.recycle();
            PreprocessingForTemplateMatcher();
        }
    }
}
