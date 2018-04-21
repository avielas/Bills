package com.bills.bills.fragments;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.abbyy.mobile.ocr4.AssetDataSource;
import com.abbyy.mobile.ocr4.DataSource;
import com.abbyy.mobile.ocr4.Engine;
import com.abbyy.mobile.ocr4.FileLicense;
import com.abbyy.mobile.ocr4.License;
import com.abbyy.mobile.ocr4.RecognitionConfiguration;
import com.abbyy.mobile.ocr4.RecognitionLanguage;
import com.abbyy.mobile.ocr4.RecognitionManager;
import com.abbyy.mobile.ocr4.layout.MocrPrebuiltLayoutInfo;
import com.bills.bills.R;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Contracts.Interfaces.IOcrEngine;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;
import com.bills.billslib.CustomViews.DragRectView;
import com.bills.billslib.Utilities.Utilities;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class BillAnalyzerFragment extends Fragment implements RecognitionManager.RecognitionCallback {
    private final String Tag = this.getClass().getName();
    private final String TAG = this.getClass().getName();

    private OnBillAnalyzernteractionListener mListener;

    private DragRectView mDragRectView;

    private Dialog mProgressDialog;
    private Context mContext;
    private UUID _sessionId;

    private Handler mHandler;

    protected IOcrEngine mOcrEngine;

    protected String mRelativeDbAndStoragePath;
    protected Integer mPassCode;

    public BillAnalyzerFragment() {}

    private byte[] mImage;

    private Thread mOcrEngineInitThread;

    private int mDragRectViewWidth = Integer.MIN_VALUE;
    private int mDragRectViewHeight = Integer.MIN_VALUE;

    private int mImageWidth = Integer.MIN_VALUE;
    private int mImageHeight = Integer.MIN_VALUE;

    private Point TopLeft;
    private Point TopRight;
    private Point BottomRight;
    private Point BottomLeft;

    private ViewTreeObserver mViewTreeObserver;

    public void Init(byte[] image, UUID sessionId, Integer passCode, String relativeDbAndStoragePath, Context context){
        mImage = image;
        mContext = context;
        _sessionId = sessionId;
        mPassCode = passCode;
        mRelativeDbAndStoragePath = relativeDbAndStoragePath;
        mHandler = new Handler();
        if(mOcrEngine == null) {
            mOcrEngine = new TesseractOCREngine();
            mOcrEngineInitThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mOcrEngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
                }
            });
            mOcrEngineInitThread.start();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProgressDialog = new Dialog(mContext);

        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                mHandler.post(mShowProgressDialog);

                Mat billMat = null;

                try {
                    billMat = Utilities.Bytes2MatAndRotateClockwise90(_sessionId, mImage);
                } catch (Exception ex) {
                    String logMessage = "failed to convert bytes to mat or rotating the image";
                    String toastMessage = "משהו השתבש... נא לנסות שוב";
                    ReportError(logMessage, toastMessage);
                    mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                }

                mImageWidth = billMat.width();
                mImageHeight = billMat.height();

                Bitmap imageBmp = Bitmap.createBitmap(billMat.width(), billMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(billMat, imageBmp);

                final Bitmap imageForDRV = imageBmp;


                Button doneButton = getActivity().findViewById(R.id.dragRectFragmentDone);
                doneButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        analyze();
                    }
                });

                try {
                    if (billMat == null) {
                        String logMessage = "failed to convert bytes to mat or rotating the image";
                        String toastMessage = "משהו השתבש... נא לנסות שוב";
                        ReportError(logMessage, toastMessage);
                        mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                        return;
                    }

                    BillAreaDetector areaDetector = new BillAreaDetector(_sessionId);
                    org.opencv.core.Point topLeft = new org.opencv.core.Point();
                    org.opencv.core.Point topRight = new org.opencv.core.Point();
                    org.opencv.core.Point buttomLeft = new org.opencv.core.Point();
                    org.opencv.core.Point buttomRight = new org.opencv.core.Point();

                    if (!areaDetector.GetBillCorners(billMat, topRight, buttomRight, buttomLeft, topLeft)) {
                        String logMessage = "failed to get bills corners";
                        String toastMessage = "אזור החשבון לא זוהה, נא לסמן את אזור החשבונית";
                        ReportError(logMessage, toastMessage);
                        TopLeft = new android.graphics.Point(imageBmp.getWidth() / 3, imageBmp.getHeight() / 3);
                        TopRight = new android.graphics.Point(imageBmp.getWidth() * 2 / 3, imageBmp.getHeight() / 3);
                        BottomRight = new android.graphics.Point(imageBmp.getWidth() * 2 / 3, imageBmp.getHeight() * 2 / 3);
                        BottomLeft = new android.graphics.Point(imageBmp.getWidth() / 3, imageBmp.getHeight() * 2 / 3);

                    } else {
                        TopLeft = new android.graphics.Point(((Double) (topLeft.x)).intValue(),
                                ((Double) (topLeft.y)).intValue());

                        TopRight = new android.graphics.Point(((Double) (topRight.x)).intValue(),
                                ((Double) (topRight.y)).intValue());

                        BottomRight = new android.graphics.Point(((Double) (buttomRight.x)).intValue(),
                                ((Double) (buttomRight.y)).intValue());

                        BottomLeft = new android.graphics.Point(((Double) (buttomLeft.x)).intValue(),
                                ((Double) (buttomLeft.y)).intValue());
                    }
                } catch (Exception ex) {
                    mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                    //init to default values
                    if(null != imageBmp) {
                        TopLeft = new android.graphics.Point(imageBmp.getWidth() / 3, imageBmp.getHeight() / 3);
                        TopRight = new android.graphics.Point(imageBmp.getWidth() * 2 / 3, imageBmp.getHeight() / 3);
                        BottomRight = new android.graphics.Point(imageBmp.getWidth() * 2 / 3, imageBmp.getHeight() * 2 / 3);
                        BottomLeft = new android.graphics.Point(imageBmp.getWidth() / 3, imageBmp.getHeight() * 2 / 3);
                    }
                    return;
                } finally {
                    Point zeroPoint = new Point(0, 0);
                    if(TopLeft.equals(zeroPoint)    && TopRight.equals(zeroPoint) &&
                       BottomLeft.equals(zeroPoint) && BottomRight.equals(zeroPoint)) {
                       String logMessage = "failed to get bills corners";
                       String toastMessage = "אזור החשבון לא זוהה, נא לסמן את אזור החשבונית";
                       ReportError(logMessage, toastMessage);
                       TopLeft = new android.graphics.Point(imageBmp.getWidth() / 3, imageBmp.getHeight() / 3);
                       TopRight = new android.graphics.Point(imageBmp.getWidth() * 2 / 3, imageBmp.getHeight() / 3);
                       BottomRight = new android.graphics.Point(imageBmp.getWidth() * 2 / 3, imageBmp.getHeight() * 2 / 3);
                       BottomLeft = new android.graphics.Point(imageBmp.getWidth() / 3, imageBmp.getHeight() * 2 / 3);
                    }
                    mDragRectView = getActivity().findViewById(R.id.dragRectView);
                    mViewTreeObserver = mDragRectView.getViewTreeObserver();
                    if (mViewTreeObserver.isAlive()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                mViewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        mDragRectView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                        mDragRectViewWidth = mDragRectView.getWidth();
                                        mDragRectViewHeight = mDragRectView.getHeight();

                                        double factorX = 1.0 * mImageWidth / mDragRectViewWidth;
                                        double factorY = 1.0 * mImageHeight / mDragRectViewHeight;

                                        mDragRectView.TopLeft = (android.graphics.Point) Utilities.GetScaledPoint(TopLeft, factorX, factorY);
                                        mDragRectView.TopRight = (android.graphics.Point) Utilities.GetScaledPoint(TopRight, factorX, factorY);
                                        mDragRectView.ButtomRight = (android.graphics.Point) Utilities.GetScaledPoint(BottomRight, factorX, factorY);
                                        mDragRectView.ButtomLeft = (android.graphics.Point) Utilities.GetScaledPoint(BottomLeft, factorX, factorY);

                                        synchronized (mViewTreeObserver) {
                                            mViewTreeObserver.notifyAll();
                                        }
                                    }
                                });
                            }
                        });
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (android.os.Build.VERSION.SDK_INT >= 16) {
                                mDragRectView.setBackground(new BitmapDrawable(getActivity().getResources(), imageForDRV));
                            } else {
                                mDragRectView.setBackgroundDrawable(new BitmapDrawable(imageForDRV));
                            }

                        }
                    });

                    if (billMat != null) {
                        billMat.release();
                    }
                    mHandler.post(mHideProgressDialog);
                }
            }
        });

        t.start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bill_analyzer, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnBillAnalyzernteractionListener) {
            mListener = (OnBillAnalyzernteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCameraFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mImage = null;
        mContext = null;
        _sessionId = null;
        mPassCode = null;
        mRelativeDbAndStoragePath = null;
        mHandler.post(mHideProgressDialog);
        mHandler = null;
    }

    private void analyze() {
        mProgressDialog = new Dialog(mContext);
        mHandler.post(mShowProgressDialog);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                synchronized (mViewTreeObserver) {
                    try {
                        mViewTreeObserver.wait(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                double factorX = 1.0 * mDragRectViewWidth / mImageWidth;
                double factorY = 1.0 * mDragRectViewHeight / mImageHeight;

                TopLeft = (android.graphics.Point) Utilities.GetScaledPoint(mDragRectView.TopLeft, factorX, factorY);
                TopRight = (android.graphics.Point) Utilities.GetScaledPoint(mDragRectView.TopRight, factorX, factorY);
                BottomRight = (android.graphics.Point) Utilities.GetScaledPoint(mDragRectView.ButtomRight, factorX, factorY);
                BottomLeft = (android.graphics.Point) Utilities.GetScaledPoint(mDragRectView.ButtomLeft, factorX, factorY);
                try {
                    if (!OpenCVLoader.initDebug()) {
                        String logMessage = "Failed to initialize OpenCV.";
                        BillsLog.Log(_sessionId, LogLevel.Error, logMessage, LogsDestination.BothUsers, Tag);
                        ReportError(logMessage, logMessage);
                        mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                        return;
                    }
                    Mat billMatCopy = null;
                    Mat billMat = null;
                    Bitmap processedBillBitmap = null;
                    TemplateMatcher templateMatcher;
                    int numOfItems;


                    try {
                        mOcrEngineInitThread.join(2 * 1000);
                    } catch (Exception ex) {
                        ReportError("MM", "MMM");
                    }

                    if (!mOcrEngine.Initialized()) {
                        String logMessage = "OCR Engine initialization failed.";
                        String toastMessage = "משהו השתבש... נא לנסות שוב";
                        ReportError(logMessage, toastMessage);
                        mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                        return;
                    }

                    try {
                        billMat = Utilities.Bytes2MatAndRotateClockwise90(_sessionId, mImage);

                        try {

                            billMat = ImageProcessingLib.WarpPerspective(billMat,
                                    new org.opencv.core.Point(TopLeft.x + 20, TopLeft.y + 20),
                                    new org.opencv.core.Point(Math.max(TopRight.x - 20, 0), TopRight.y + 20),
                                    new org.opencv.core.Point(Math.max(BottomRight.x - 20, 0), Math.max(BottomRight.y - 20, 0)),
                                    new org.opencv.core.Point(BottomLeft.x + 20, Math.max(BottomLeft.y - 20, 0)));

                            billMatCopy = billMat.clone();
                        } catch (Exception e) {
                            String logMessage = "Warp perspective has been failed. \nStackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage();
                            String toastMessage = "סיבוב החשבון נכשל... נא לנסות שנית";
                            ReportError(logMessage, toastMessage);
                            mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                            return;
                        }

                        BillsLog.Log(_sessionId, LogLevel.Info, "Warped perspective successfully.", LogsDestination.BothUsers, Tag);

                        processedBillBitmap = Bitmap.createBitmap(billMat.width(), billMat.height(), Bitmap.Config.ARGB_8888);
                        ImageProcessingLib.PreprocessingForTM(billMat);
                        Utils.matToBitmap(billMat, processedBillBitmap);

                        templateMatcher = new TemplateMatcher(mOcrEngine, processedBillBitmap, getRecognitionManager());
                        try {
                            templateMatcher.Match(_sessionId);
                            BillsLog.Log(_sessionId, LogLevel.Info, "Template matcher succeeded.", LogsDestination.BothUsers, Tag);
                        } catch (Exception e) {
                            String logMessage = "Template matcher threw an exception. \nStackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage();
                            String toastMessage = "החשבונית לא זוהתה... נא לנסות שנית";
                            ReportError(logMessage, toastMessage);
                            mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                            return;
                        }

                        numOfItems = templateMatcher.priceAndQuantity.size();

                        /***** we use processedBillBitmap second time to prevent another Bitmap allocation due to *****/
                        /***** Out Of Memory when running 4 threads parallel                                      *****/

                        ImageProcessingLib.PreprocessingForParsing(billMatCopy);
                        Utils.matToBitmap(billMatCopy, processedBillBitmap);
                        templateMatcher.InitializeBeforeSecondUse(processedBillBitmap);
                        templateMatcher.Parsing(_sessionId, numOfItems);

                        List<BillRow> rows = new ArrayList<>();
                        int index = 0;
                        for (Double[] row : templateMatcher.priceAndQuantity) {
                            Bitmap item = templateMatcher.itemLocationsByteArray.get(index);
                            Double price = row[0];
                            Integer quantity = row[1].intValue();
                            rows.add(new BillRow(price, quantity, index, item));
                            index++;
                        }
                        BillsLog.Log(_sessionId, LogLevel.Info, "Parsing finished", LogsDestination.BothUsers, Tag);
                        mListener.onBillAnalyzerSucceed(rows, mImage, mPassCode, mRelativeDbAndStoragePath, mDragRectViewWidth);
                    } catch (Exception e) {
                        String logMessage = "Exception has been thrown. StackTrace: " + e.getStackTrace() +
                                "\nException Message: " + e.getMessage();
                        String toastMessage = "משהו השתבש... נא לנסות שוב";
                        ReportError(logMessage, toastMessage);
                        mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                        return;
                    } finally {
                        if (null != billMat) {
                            billMat.release();
                        }
                        if (null != processedBillBitmap) {
                            processedBillBitmap.recycle();
                        }
                        if (null != billMatCopy) {
                            billMatCopy.release();
                        }
                    }
                } catch (Exception e) {
                    String logMessage = "Exception has been thrown. StackTrace: " + e.getStackTrace() +
                            "\nException Message: " + e.getMessage();
                    BillsLog.Log(_sessionId, LogLevel.Error, logMessage, LogsDestination.BothUsers, Tag);
                    mListener.onBillAnalyzerFailed(mImage, mRelativeDbAndStoragePath);
                    return;
                } finally {
                    mHandler.post(mHideProgressDialog);
                }

            }
        });
        t.start();

//        Mat m = null;
//        try{
//            m=Utilities.Bytes2MatAndRotateClockwise90(_sessionId, mImage);
//        }catch (Exception e){
//
//        }
//        Bitmap image = Bitmap.createBitmap(m.width(), m.height(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(m, image);
//        startRecognition(image);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnBillAnalyzernteractionListener {
        void onBillAnalyzerSucceed(List<BillRow> rows, byte[] image, Integer passCode, String relativeDbAndStoragePath, int screenWidth);
        void onBillAnalyzerFailed();
        void onBillAnalyzerFailed(final byte[] image, String mRelativeDbAndStoragePath);
    }

    // Create runnable for hiding progress dialog
    private final Runnable mHideProgressDialog = new Runnable() {
        public void run() {
            if(mProgressDialog != null)
            {
                mProgressDialog.cancel();
                mProgressDialog.hide();
            }
        }
    };

    // Create runnable for posting progress dialog
    private final Runnable mShowProgressDialog = new Runnable() {
        public void run() {
            try {
                mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mProgressDialog.setContentView(R.layout.custom_dialog_progress);
                mProgressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    };

    private void ReportError(String logMessage, final String toastMessage) {
        BillsLog.Log(_sessionId, LogLevel.Error, logMessage, LogsDestination.BothUsers, Tag);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, toastMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private RecognitionManager getRecognitionManager() {
        final RecognitionConfiguration recognitionConfiguration = new RecognitionConfiguration();
        recognitionConfiguration.setImageResolution( 0 );
        int imageProcessingOptions = RecognitionConfiguration.ImageProcessingOptions.PROHIBIT_VERTICAL_CJK_TEXT;
        imageProcessingOptions |= RecognitionConfiguration.ImageProcessingOptions.BUILD_WORDS_INFO;
        recognitionConfiguration.setImageProcessingOptions( imageProcessingOptions );
        recognitionConfiguration.setRecognitionMode( RecognitionConfiguration.RecognitionMode.FULL );
        final DataSource assetDataSrouce = new AssetDataSource( this.getActivity().getAssets() );
        final List<DataSource> dataSources = new ArrayList<DataSource>();
        dataSources.add( assetDataSrouce );

        System.loadLibrary("MobileOcrEngine");
//        Engine.loadNativeLibrary();


        try {
            final String _licenseFile = "license";
            final String _applicationID = "Android_ID";
            final String _patternsFileExtension = ".mp3";
            final String _dictionariesFileExtension = ".mp3";
            final String _keywordsFileExtension = ".mp3";

            Engine.createInstance( dataSources, new FileLicense( assetDataSrouce,
                            _licenseFile, _applicationID ),
                    new Engine.DataFilesExtensions( _patternsFileExtension,
                            _dictionariesFileExtension,
                            _keywordsFileExtension ) );

        } catch( final IOException e ) {
//            Log.d(TAG, "startRecognition: ");
        } catch( final License.BadLicenseException e ) {
//            Log.d(TAG, "startRecognition: ");
        }

        Set<RecognitionLanguage> langSet =  EnumSet.noneOf( RecognitionLanguage.class );
        langSet.add(RecognitionLanguage.Digits);
        recognitionConfiguration.setRecognitionLanguages(langSet);

        return Engine.getInstance().getRecognitionManager( recognitionConfiguration );
    }

    @Override
    public boolean onRecognitionProgress(int i, int i1) {
        return false;
    }

    @Override
    public void onRotationTypeDetected(RecognitionManager.RotationType rotationType) {

    }

    @Override
    public void onPrebuiltWordsInfoReady(MocrPrebuiltLayoutInfo mocrPrebuiltLayoutInfo) {
        Log.d("MMM", "onPrebuiltWordsInfoReady: mocrPrebuiltLayoutInfo");
    }
}
