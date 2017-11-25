package com.bills.testslib;

import android.util.Log;

import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Contracts.Interfaces.ILogger;
import com.bills.billslib.Core.BillsLog;

import java.util.UUID;

/**
 * Created by aviel on 24/10/17.
 */

public class TestsUtilities {
    private static String Tag = TestsUtilities.class.getName();

    public static void InitBillsLogToLogcat(final UUID sessionId) {

        BillsLog.AddNewSession(sessionId, new ILogger() {
            @Override
            public void Log(String tag, LogLevel logLevel, String message, LogsDestination logsDestination) {
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

            @Override
            public void UninitCommonSession(String myFirebaseLogPath) {
                throw new UnsupportedOperationException(Tag + ": Function UninitCommonSession doesn't implement for this class");
            }
        });
    }
}
