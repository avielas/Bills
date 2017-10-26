package com.bills.billcaptureapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import com.bills.billcaptureapp.fragments.WelcomeScreenFragment;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Core.MainActivityBase;
import com.bills.billslib.Utilities.FilesHandler;
import com.bills.testslib.CameraFragment;
import com.bills.testslib.TestsUtilities;
import java.util.List;

public class BillCaptureAppMainActivity extends MainActivityBase implements
        com.bills.billslib.Fragments.CameraFragment.OnFragmentInteractionListener,
        com.bills.billcaptureapp.fragments.WelcomeScreenFragment.OnFragmentInteractionListener{
    private String Tag = BillCaptureAppMainActivity.class.getName();
    private CameraFragment mCameraFragment;
    private WelcomeScreenFragment mWelcomScreenFragment;
    private Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_capture_app_main);
        TestsUtilities.InitBillsLogToLogcat();
        mCameraFragment = new CameraFragment();
        mWelcomScreenFragment = new WelcomeScreenFragment();
        StartWelcomeFragment(null);
    }

    public void StartCameraFragment() {
        try {
            mCameraFragment.Init(0, "");
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, mCameraFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
            mCurrentFragment = mCameraFragment;
        } catch (Exception e) {
            BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
        }
    }

    @Override
    public void StartSummarizerFragment(List<BillRow> rows, byte[] image, Integer passCode, String relativeDbAndStoragePath) {
        throw new UnsupportedOperationException(Tag + ": StartSummarizerFragment");
    }

    @Override
    public void onBackPressed(){
        if(mCurrentFragment == mCameraFragment || mCurrentFragment == mWelcomScreenFragment){
            StartWelcomeFragment();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public void StartWelcomeFragment() {
        StartWelcomeFragment(null);
    }

    @Override
    public void Finish() {
        throw new UnsupportedOperationException(Tag + ": Finish");
    }

    @Override
    public void StartWelcomeFragment(final byte[] image) {
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, mWelcomScreenFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
            mCurrentFragment = mWelcomScreenFragment;
            if (null != image) {
                Thread t = new Thread() {
                public void run() {
                    try {
                        FilesHandler.toDelete(image);
                    } catch (Exception e) {
                        BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
                    }
                }
                };
                t.start();
            }
        } catch (Exception e) {
            BillsLog.Log(Tag, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage());
        }
    }
}
