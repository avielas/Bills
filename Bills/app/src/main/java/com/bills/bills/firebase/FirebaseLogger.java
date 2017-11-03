package com.bills.bills.firebase;

import android.util.Log;

import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Interfaces.ILogger;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by michaelvalershtein on 09/09/2017.
 */

public class FirebaseLogger implements ILogger {

    private static String Tag = FirebaseLogger.class.getName();
    private DatabaseReference mFirebaseLogReferenceApp;
    private String mUserUid;

    private int mRowCount = 0;
    private AtomicInteger mCurEntryCount = new AtomicInteger(0);

    private AtomicBoolean mTransactionInProgress = new AtomicBoolean(false);

    private AtomicBoolean mTerminateOnFinish = new AtomicBoolean(false);
    private Object mLock = new Object();

    private ConcurrentLinkedQueue<LogEntry> mLogEntries = new ConcurrentLinkedQueue<>();

    public FirebaseLogger(String firebaseLogReferenceApp, String userUid){
        mUserUid = userUid;
        mFirebaseLogReferenceApp = FirebaseDatabase.getInstance().getReference().child(firebaseLogReferenceApp + "/Logs");
        mFirebaseLogReferenceApp.keepSynced(true);
    }

    @Override
    public void Log(final String tag, final LogLevel logLevel, final String message) {
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

        //print to FireBase log
        mFirebaseLogReferenceApp.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                synchronized (mLock) {
                    DateFormat sdf = new SimpleDateFormat("dd_MM_yyyy HH:mm:ss_SSS");
                    Date date = new Date();
                    String now = sdf.format(date);
                    mutableData.child("(" + now + " ," + mUserUid + ")").setValue(/*TODO: How to add it on new line ?? +*/
                                                                                   mRowCount++ + ", " + logLevel.toString() + ": " + tag + ": " + message);
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Log.v(tag, "enter onComplete");
                if(databaseError != null &&
                  (databaseError.toString().length() == 0 || databaseError.toString().isEmpty())){
                    Log(Tag, LogLevel.Error, "Error Message: " + databaseError.getMessage() + ", Details: " + databaseError.getDetails());
                }
            }
        });
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
