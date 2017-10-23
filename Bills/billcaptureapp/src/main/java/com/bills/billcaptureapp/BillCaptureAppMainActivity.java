package com.bills.billcaptureapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bills.billslib.Core.MainActivityBase;

public class BillCaptureAppMainActivity extends MainActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_capture_app_main);
    }
}
