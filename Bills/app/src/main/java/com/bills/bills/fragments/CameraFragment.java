package com.bills.bills.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bills.bills.BillsMainActivity;
import com.bills.bills.R;
import com.bills.bills.firebase.PassCodeResolver;
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
import com.bills.billslib.Utilities.FilesHandler;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ThrowOnExtraProperties;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CameraFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class CameraFragment extends Fragment implements View.OnClickListener, IOnCameraFinished {
    private String Tag = this.getClass().getSimpleName();

    //Camera Renderer
    private CameraRenderer mRenderer;

    //Camera Elements
    private TextureView mCameraPreviewView = null;
    private Button mCameraCaptureButton = null;

    //selection order: auto->on->off
    private Button mCameraFlashMode = null;
    private Integer mCurrentFlashMode = R.drawable.camera_screen_flash_auto;

    private OnFragmentInteractionListener mListener;

    private IOcrEngine mOcrEngine;

    private Integer mPassCode;
    private String mRelativeDbAndStoragePath;

    public CameraFragment() {
        // Required empty public constructor
    }

    public void Init(Integer passCode, String relativeDbAndStoragePath){
        mPassCode = passCode;
        mRelativeDbAndStoragePath = relativeDbAndStoragePath;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mRenderer = new CameraRenderer(getContext());
        mRenderer.SetOnCameraFinishedListener(this);

        mCameraPreviewView = (TextureView) getView().findViewById(R.id.camera_textureView);
        mCameraPreviewView.setSurfaceTextureListener(mRenderer);
        mCameraPreviewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mRenderer.setAutoFocus();
                        break;
                }
                return true;
            }
        });

        mCameraPreviewView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                mRenderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
            }
        });

        mCameraCaptureButton = (Button) getView().findViewById(R.id.camera_capture_button);
        mCameraCaptureButton.setOnClickListener(this);

        mCameraFlashMode = (Button)getView().findViewById(R.id.camera_flash_mode);
        mCameraFlashMode.setBackgroundResource(mCurrentFlashMode);
        mCameraFlashMode.setTag(mCurrentFlashMode);
        mCameraFlashMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCurrentFlashMode == R.drawable.camera_screen_flash_auto){
                    mCurrentFlashMode = R.drawable.camera_screen_flash_on;
                    mRenderer.SetFlashMode(Camera.Parameters.FLASH_MODE_ON);

                }else if(mCurrentFlashMode == R.drawable.camera_screen_flash_on){
                    mCurrentFlashMode = R.drawable.camera_screen_flash_off;
                    mRenderer.SetFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }else {
                    mCurrentFlashMode = R.drawable.camera_screen_flash_auto;
                    mRenderer.SetFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                }
                mCameraFlashMode.setBackgroundResource(mCurrentFlashMode);
            }
        });

        if(mOcrEngine == null) {
            mOcrEngine = new TesseractOCREngine();
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    mOcrEngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
                }
            });
            t.start();
        }

    }

    @Override
    public void onClick(View v) {
        mRenderer.setAutoFocus();
        mRenderer.takePicture();
    }

    @Override
    public void OnCameraFinished(byte[] bytes) {

        if (!OpenCVLoader.initDebug()) {
            String message = "Failed to initialize OpenCV.";
            Log.d(Tag, message);
            BillsLog.Log(LogLevel.Error, message);
            mListener.Finish();
        }

        String fileFullName = Constants.IMAGES_PATH + "/ocrBytes" + ".txt";
        Bitmap bitmapBill = null;
        Mat matBill = null;
        BillAreaDetector areaDetector = new BillAreaDetector();
        Point topLeft = new Point();
        Point topRight = new Point();
        Point buttomRight = new Point();
        Point buttomLeft = new Point();
        Mat warpedMat = null;
        Mat warpedMatCopy = null;
        Bitmap processedBillBitmap = null;
        TemplateMatcher templateMatcher;
        int numOfItems;        
        
        while (!mOcrEngine.Initialized()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            matBill = FilesHandler.GetRotatedBillMat(bytes);
            bitmapBill = Bitmap.createBitmap(matBill.width(), matBill.height(), Bitmap.Config.ARGB_8888);
            FilesHandler.SaveToTXTFile(bytes, fileFullName);

            if (!areaDetector.GetBillCorners(matBill, topLeft, topRight, buttomRight, buttomLeft)) {
                //TODO: add drag rect view here
                Log.d(Tag, "Failed\n");
                BillsLog.Log(LogLevel.Error, "Failed to get bill corners.");
                throw new Exception();
            }

            BillsLog.Log(LogLevel.Info, "Got bill corners: " +
                    "Top Left: " + topLeft +
                    "; Top Right: " + topRight +
                    "; Buttom Right: " + buttomRight +
                    "; Buttom Left: " + buttomLeft);


            try {
                warpedMat = ImageProcessingLib.WarpPerspective(matBill, topLeft, topRight, buttomRight, buttomLeft);
                warpedMatCopy = warpedMat.clone();
            } catch (Exception e) {
                e.printStackTrace();
                BillsLog.Log(LogLevel.Error, "Failed to warp perspective. Exception: " + e.getMessage());
                //TODO: decide what to do. Retake the picture? crash the app?
                throw new Exception();
            }

            BillsLog.Log(LogLevel.Info, "Warped perspective successfully.");

            processedBillBitmap = Bitmap.createBitmap(warpedMat.width(), warpedMat.height(), Bitmap.Config.ARGB_8888);
            ImageProcessingLib.PreprocessingForTM(warpedMat);
            Utils.matToBitmap(warpedMat, processedBillBitmap);

            templateMatcher = new TemplateMatcher(mOcrEngine, processedBillBitmap);
            try {
                templateMatcher.Match();
                BillsLog.Log(LogLevel.Info, "Template matcher succeed.");
            } catch (Exception e) {
                BillsLog.Log(LogLevel.Error, "Template matcher threw an exception: " + e.getMessage());
                e.printStackTrace();
            }

            ImageProcessingLib.PreprocessingForParsing(warpedMatCopy);
            numOfItems = templateMatcher.priceAndQuantity.size();

            /***** we use processedBillBitmap second time to prevent another Bitmap allocation due to *****/
            /***** Out Of Memory when running 4 threads parallel                                      *****/
            Utils.matToBitmap(warpedMatCopy, processedBillBitmap);
            templateMatcher.InitializeBeforeSecondUse(processedBillBitmap);
            templateMatcher.Parsing(numOfItems);

            List<BillRow> rows = new ArrayList<>();
            int index = 0;
            for (Double[] row : templateMatcher.priceAndQuantity) {
                Bitmap item = templateMatcher.itemLocationsByteArray.get(index);
                Double price = row[0];
                Integer quantity = row[1].intValue();
                rows.add(new BillRow(price, quantity, index, item));
                index++;
            }
            Utils.matToBitmap(matBill, bitmapBill);
            mListener.StartSummarizerFragment(rows, bitmapBill, mPassCode, mRelativeDbAndStoragePath);
            BillsLog.Log(LogLevel.Info, "Parsing finished");
        }catch (Exception ex){
            mListener.StartWelcomeFragment(bitmapBill);
        }
        finally {
            if(null != matBill){
                matBill.release();
            }
            if(null != processedBillBitmap){
                processedBillBitmap.recycle();
            }
            if(null != warpedMat){
                warpedMat.release();
            }
            if(null != warpedMatCopy){
                warpedMatCopy.release();
            }
        }
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
    public interface OnFragmentInteractionListener {
        void StartSummarizerFragment(List<BillRow> rows, Bitmap image, Integer passCode, String relativeDbAndStoragePath);
        void StartWelcomeFragment();
        void Finish();
        void StartWelcomeFragment(Bitmap image);
    }
}
