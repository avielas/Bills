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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by michaelvalershtein on 09/09/2017.
 */

public class FirebaseLogger implements ILogger {

    private static String Tag = FirebaseLogger.class.getName();
    private String mUserUid;
    private DatabaseReference mMyFirebaseLogReference;
    private DatabaseReference mFirebaseLogReferenceApp;
    private String mNow;

    private long mRowCount;
    private AtomicInteger mCurEntryCount = new AtomicInteger(0);

    private AtomicBoolean mTransactionInProgress = new AtomicBoolean(false);

    private AtomicBoolean mTerminateOnFinish = new AtomicBoolean(false);
    private Object mLock = new Object();

    private ConcurrentLinkedQueue<LogEntry> mLogEntries = new ConcurrentLinkedQueue<>();

    public FirebaseLogger(String userUid, String firebaseDBCurrUserReference, String firebaseDBMainUserReference){
        mUserUid = userUid;
        mRowCount = 0;
        mNow = Utilities.GetTimeStamp();
        mMyFirebaseLogReference  = FirebaseDatabase.getInstance().getReference().child(firebaseDBCurrUserReference + "/Logs/" + mNow);
        mMyFirebaseLogReference.keepSynced(true);
        mFirebaseLogReferenceApp = FirebaseDatabase.getInstance().getReference().child(firebaseDBMainUserReference + "/Logs");
        mFirebaseLogReferenceApp.keepSynced(true);
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
            mFirebaseLogReferenceApp.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    synchronized (mLock) {
                        Long childrenCount = mutableData.getChildrenCount();
                        mRowCount = childrenCount + 1;
                        String now = Utilities.GetTimeStamp();
                        mutableData.child(String.valueOf(mRowCount)).setValue(/*TODO: How to add it on new line ?? +*/
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
                        String mNow = Utilities.GetTimeStamp();
                        mutableData.child(mNow).setValue(/*TODO: How to add it on new line ?? +*/
                                mRowCount++ + ", " + logLevel.toString() + ": " + tag + ": " + message);
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

    private class LogEntry{
        public final LogLevel mlogLevel;
        public final String mMessage;

        public LogEntry(LogLevel logLevel, String message){
            mlogLevel = logLevel;
            mMessage = message;
        }
    }

//    private Thread GetWorkerThread(){
//        return new Thread(new Runnable() {
//            @Override
//            public void run() {
//                ConcurrentLinkedQueue<LogEntry> logEntries = mLogEntries;
//                AtomicBoolean terminateOnFinish = mTerminateOnFinish;
//
//                while(true){
//                    if(logEntries.isEmpty()){
//                        if(terminateOnFinish.get()){
//                            return;
//                        }
//
//                        try {
//                            synchronized (this) {
//                                this.wait(500);
//                            }
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }else {
//                        if (mTransactionInProgress.get()) {
//                            continue;
//                        }
//
//                        mTransactionInProgress.set(true);
//                        final LogEntry logEntry = logEntries.poll();
//                        mFirebaseLogReferenceApp.runTransaction(new Transaction.Handler() {
//                            @Override
//                            public Transaction.Result doTransaction(MutableData mutableData) {
//                                if (mutableData == null || mutableData.getKey() == null) {
//                                    return Transaction.abort();
//                                }
//                                mutableData.child(Integer.toString(mCurEntryCount.incrementAndGet())).
//                                        setValue(logEntry.mlogLevel + ":" + logEntry.mMessage);
//                                return Transaction.success(mutableData);
//                            }
//
//                            @Override
//                            public void onComplete(DatabaseError databaseError, boolean commited, DataSnapshot dataSnapshot) {
//                                mTransactionInProgress.set(false);
//                                synchronized (this) {
//                                    this.notifyAll();
//                                }
//                            }
//                        });
//
//                    }
//                }
//            }
//        });
//    }
}
