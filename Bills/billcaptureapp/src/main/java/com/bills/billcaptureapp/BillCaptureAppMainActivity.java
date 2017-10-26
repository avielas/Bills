package com.bills.billcaptureapp;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;

import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Core.MainActivityBase;
import com.bills.billslib.Utilities.FilesHandler;
import com.bills.testslib.CameraFragment;
import com.bills.testslib.TestsUtilities;

import java.util.List;

public class BillCaptureAppMainActivity extends MainActivityBase implements com.bills.billslib.Fragments.CameraFragment.OnFragmentInteractionListener {
    private String Tag = BillCaptureAppMainActivity.class.getName();
    private final int REQUEST_CODE = 1;
    Button _clickToSimpleCapture;
    Button _clickToRightCapture;
    Button _clickToLeftCapture;
    Button _clickToRemotlyCapture;
    Button _clickToStraightCapture;
    final Handler mHandler = new Handler();
    private String mResult;
    private CameraFragment mCameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_capture_app_main);
        TestsUtilities.InitBillsLogToLogcat();

        //Initialize parameters
        _clickToSimpleCapture = findViewById(R.id.simpleCaptureButton);
        _clickToRightCapture = findViewById(R.id.rightCaptureButton);
        _clickToLeftCapture = findViewById(R.id.leftCaptureButton);
        _clickToRemotlyCapture = findViewById(R.id.remotlyCaptureButton);
        _clickToStraightCapture = findViewById(R.id.straightCaptureButton);
        AddListenerToSimpleCaptureButton();
        AddListenerToRightCaptureButton();
        AddListenerToLeftCaptureButton();
        AddListenerToRemotlyCaptureButton();
        AddListenerToStraightCaptureButton();
        mCameraFragment = new CameraFragment();
    }

    public void AddListenerToSimpleCaptureButton() {
        _clickToSimpleCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    startTestThread();
                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    public void AddListenerToRightCaptureButton() {
        _clickToRightCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {

                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    public void AddListenerToLeftCaptureButton() {
        _clickToLeftCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {

                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    public void AddListenerToRemotlyCaptureButton() {
        _clickToRemotlyCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {

                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    public void AddListenerToStraightCaptureButton() {
        _clickToStraightCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {

                } catch (Exception e) {
                    BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                }
            }
        });
    }

    private void StartCameraFragment(int requestCode) {
        try {
            mCameraFragment.Init(0, "");
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, mCameraFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();

//            Intent intent = new Intent(getBaseContext(), CameraActivity.class);
//            if (intent.resolveActivity(getPackageManager()) != null) {
//                startActivityForResult(intent, requestCode);
//            }
        } catch (Exception e) {
            BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
        }
    }

    // Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            BillsLog.Log("Inchoo tutorial", LogLevel.Info, mResult);
        }
    };

    protected void startTestThread() {
        Thread t = new Thread() {
            public void run() {
                BillsLog.Log("Inchoo tutorial", LogLevel.Info, "My thread is running");
                StartCameraFragment(REQUEST_CODE);
//                mResult = "This is my new result";
//                mHandler.post(mUpdateResults);
            }
        };
        t.start();
    }

    @Override
    public void StartSummarizerFragment(List<BillRow> rows, byte[] image, Integer passCode, String relativeDbAndStoragePath) {
        throw new UnsupportedOperationException(Tag + ": StartSummarizerFragment");
    }

    @Override
    public void StartWelcomeFragment() {
        throw new UnsupportedOperationException(Tag + ": StartWelcomeFragment");
    }

    @Override
    public void Finish() {
        throw new UnsupportedOperationException(Tag + ": Finish");
    }

    @Override
    public void StartWelcomeFragment(byte[] image){
        try {
            FilesHandler.toDelete(image);
            setContentView(R.layout.activity_bill_capture_app_main);
            TestsUtilities.InitBillsLogToLogcat();
//
//            //Initialize parameters
//            _clickToSimpleCapture = findViewById(R.id.simpleCaptureButton);
//            _clickToRightCapture = findViewById(R.id.rightCaptureButton);
//            _clickToLeftCapture = findViewById(R.id.leftCaptureButton);
//            _clickToRemotlyCapture = findViewById(R.id.remotlyCaptureButton);
//            _clickToStraightCapture = findViewById(R.id.straightCaptureButton);
//            AddListenerToSimpleCaptureButton();
//            AddListenerToRightCaptureButton();
//            AddListenerToLeftCaptureButton();
//            AddListenerToRemotlyCaptureButton();
//            AddListenerToStraightCaptureButton();
//            mCameraFragment = new CameraFragment();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        BillsLog.Log("Inchoo tutorial", LogLevel.Info, lastCapturedBillPath);
//        throw new UnsupportedOperationException(Tag + ": Finish");
    }
}
