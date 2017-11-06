package com.bills.testslib;

import android.util.Log;

import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsPathToPrintTo;
import com.bills.billslib.Contracts.Interfaces.ILogger;
import com.bills.billslib.Core.BillsLog;

/**
 * Created by aviel on 24/10/17.
 */

public class TestsUtilities {
    private static String Tag = TestsUtilities.class.getName();

    public static void InitBillsLogToLogcat() {
        BillsLog.Init(new ILogger() {
            @Override
            public void Log(String tag, LogLevel logLevel, String message, LogsPathToPrintTo logsPathToPrintTo) {
                switch (logLevel){
                    case Error:
                        Log.e(tag, message);
                        break;
                    case Warning:
                        Log.w(tag, message);
                        break;
                    case Info:
                        Log.i(tag, message);
                        break;
                    default:
                        Log.v(tag, "this LogLevel enum doesn't exists: " + message);
                }
            }
        });
    }
}
