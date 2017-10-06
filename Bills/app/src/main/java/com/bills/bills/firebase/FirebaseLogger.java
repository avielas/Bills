package com.bills.bills.firebase;

import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Interfaces.ILogger;
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

    private DatabaseReference mFirebaseLogReference;

    private int mRowCount = 0;
    private AtomicInteger mCurEntryCount = new AtomicInteger(0);

    private AtomicBoolean mTransactionInProgress = new AtomicBoolean(false);

    private AtomicBoolean mTerminateOnFinish = new AtomicBoolean(false);
    private Object mLock = new Object();

    private ConcurrentLinkedQueue<LogEntry> mLogEntries = new ConcurrentLinkedQueue<>();

    public FirebaseLogger(String firebaseLogReference){
        mFirebaseLogReference = FirebaseDatabase.getInstance().getReference().child(firebaseLogReference + "/Logs");
        mFirebaseLogReference.keepSynced(true);
    }

    @Override
    public void Log(final String Tag, final LogLevel logLevel, final String message) {
        mFirebaseLogReference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                synchronized (mLock) {
                    mutableData.child(Integer.toString(mRowCount++)).setValue(logLevel.toString() + ": " + message);
                    return Transaction.success(mutableData);
                }
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

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
//                        mFirebaseLogReference.runTransaction(new Transaction.Handler() {
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
