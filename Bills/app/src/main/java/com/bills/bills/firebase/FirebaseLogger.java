package com.bills.bills.firebase;

import android.util.Log;

import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Contracts.Interfaces.ILogger;
import com.bills.billslib.Utilities.Utilities;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

/**
 * Created by michaelvalershtein on 09/09/2017.
 */

public class FirebaseLogger implements ILogger {

    private static String Tag = FirebaseLogger.class.getName();
    private String mUserUid;
    private DatabaseReference mMyFirebaseLogReference;
    private DatabaseReference mAppFirebaseLogReference;
    private long mRowCountAppLog;
    private long mRowCountMyLog;
    private Object mLock = new Object();

    public FirebaseLogger(String userUid, String myFirebaseLogPath, String appFirebaseLogPath){
        mUserUid = userUid;
        mRowCountAppLog = 0;
        mRowCountMyLog = 1;
        mMyFirebaseLogReference  = FirebaseDatabase.getInstance().getReference().child(myFirebaseLogPath);
        mMyFirebaseLogReference.keepSynced(true);
        mAppFirebaseLogReference = FirebaseDatabase.getInstance().getReference().child(appFirebaseLogPath);
        mAppFirebaseLogReference.keepSynced(true);
    }

    @Override
    public void Log(final String tag, final LogLevel logLevel, final String message, LogsDestination logsDestination) {
        //print to Logcat
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

        if (logsDestination == LogsDestination.BothUsers ||
            logsDestination == LogsDestination.MainUser) {
            //print to main user(application Logs folder) log
            mAppFirebaseLogReference.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    synchronized (mLock) {
                        Long childrenCount = mutableData.getChildrenCount();
                        mRowCountAppLog = childrenCount + 1;
                        String now = Utilities.GetTimeStamp();
                        mutableData.child(String.valueOf(mRowCountAppLog)).setValue(/*TODO: How to add it on new line ?? +*/
                                logLevel.toString() + ": " + tag + ": " + message +
                                " (" + now + " ," + mUserUid + ")");
                        return Transaction.success(mutableData);
                    }
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    Log.v(tag, "enter onComplete");
                    if (databaseError != null &&
                            (databaseError.toString().length() == 0 || databaseError.toString().isEmpty())) {
                        Log(Tag, LogLevel.Error, "Error Message: " + databaseError.getMessage() + ", Details: " + databaseError.getDetails(), LogsDestination.BothUsers);
                    }
                }
            });
        }

        if (logsDestination == LogsDestination.BothUsers ||
            logsDestination == LogsDestination.SecondaryUser) {
            //print to secondary user log
            mMyFirebaseLogReference.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    synchronized (mLock) {
                        String now = Utilities.GetTimeStamp();
                        mutableData.child(String.valueOf(mRowCountMyLog++)).setValue(/*TODO: How to add it on new line ?? +*/
                                logLevel.toString() + ": " + tag + ": " + message + " (" + now + ")");
                        return Transaction.success(mutableData);
                    }
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    Log.v(tag, "enter onComplete");
                    if (databaseError != null &&
                            (databaseError.toString().length() == 0 || databaseError.toString().isEmpty())) {
                        Log(Tag, LogLevel.Error, "Error Message: " + databaseError.getMessage() + ", Details: " + databaseError.getDetails(), LogsDestination.BothUsers);
                    }
                }
            });
        }
    }

    @Override
    public void UninitCommonSession(String myFirebaseLogPath) {
        mMyFirebaseLogReference  = FirebaseDatabase.getInstance().getReference().child(myFirebaseLogPath);
        mMyFirebaseLogReference.keepSynced(true);
    }
}
