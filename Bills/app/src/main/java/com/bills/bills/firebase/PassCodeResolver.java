package com.bills.bills.firebase;

import com.bills.billslib.Utilities.Utilities;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by michaelvalershtein on 29/07/2017.
 */

public class PassCodeResolver {
    private final String Tag = PassCodeResolver.class.getName();

    private DatabaseReference mUserIdsDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    private String mNow;
    private String mUid;
    private String mBillRelativePath;

    private final String mRelativePathDbKey = "RelativeDbPath";
    private final String mPassCodeDbKey = "PassCode";

    private AtomicInteger newPassCode = new AtomicInteger(Integer.MIN_VALUE);
    private ConcurrentHashMap<String, HashMap<String, Object>> mPassCodes = new ConcurrentHashMap<>();

    private final AtomicBoolean mNewPassCodeRetrieved = new AtomicBoolean(false);
    private final AtomicInteger mTransactionRetries = new AtomicInteger(0);
    public PassCodeResolver(String uid, String appLogRootPath, String nowForFirstSession){
        mNow = nowForFirstSession;
        mUid = uid;
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUserIdsDatabaseReference = mFirebaseDatabase.getReference().child("userIds/");
        mUserIdsDatabaseReference.keepSynced(true);
    }

    public void SetNow(String now){
        mNow = now;
    }

    //Generate unique PassCode and DB path
    public void GetPassCode(final IPassCodeResolverCallback callback) {
        mUserIdsDatabaseReference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData mutableData) {
                if (mutableData.hasChild(mUid)) {
                    HashMap<String, Object> value = (HashMap<String, Object>)mutableData.child(mUid).getValue();

                    //String now = Utilities.GetTimeStamp();

                    mBillRelativePath = mUid + "/" + mNow;
                    value.put(mRelativePathDbKey, mBillRelativePath);

                    newPassCode.set(((Long)(value.get(mPassCodeDbKey))).intValue());
                    mPassCodes.put(mUid, value);

                    mutableData.child(mUid).setValue(value);
                } else {
                    ArrayList<Integer> usedPassCodes = new ArrayList<>();

                    for (MutableData childMutableData : mutableData.getChildren()) {
                        mPassCodes.put(childMutableData.getKey(), (HashMap<String, Object>)childMutableData.getValue());
                        usedPassCodes.add(((Long)((HashMap<String, Object>)childMutableData.getValue()).get(mPassCodeDbKey)).intValue());
                    }
                    //find an unused pass code
                    for (int i = 0; i < 10000; i++) {
                        if (!usedPassCodes.contains(i)) {
                            String timeStamp = Utilities.GetTimeStamp();

                            mBillRelativePath = mUid + "/" + timeStamp;
                            Map<String, Object> userIdsValue = new HashMap<>();
                            userIdsValue.put(mPassCodeDbKey, i);
                            userIdsValue.put(mRelativePathDbKey, mBillRelativePath);
                            newPassCode.set(i);
                            mutableData.child(mUid).setValue(userIdsValue);

                            HashMap<String, Object> curUserData = new HashMap<String, Object>();
                            curUserData.put(mPassCodeDbKey, i);
                            curUserData.put(mRelativePathDbKey, mBillRelativePath);
                            mPassCodes.put(mUid, curUserData);
                            break;
                        }
                    }
                }
                return Transaction.success(mutableData);
            }

            //finished getting new PassCode
            @Override
            public void onComplete(DatabaseError databaseError, boolean commited, DataSnapshot dataSnapshot) {
                if(databaseError != null){
                    callback.OnPassCodeResolveFail("Error: " + databaseError.getDetails() + "\nMessage: "+databaseError.getMessage());
                    return;
                }
                mNewPassCodeRetrieved.set(true);
                callback.OnPassCodeResovled(newPassCode.get(), (String) mPassCodes.get(mUid).get(mRelativePathDbKey), mUid);
            }
        });
    }

    //Get relative path according to passcode
    public void GetRelativePath(final int passCode, final IPassCodeResolverCallback callback){
        mUserIdsDatabaseReference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData child : mutableData.getChildren()) {
                    try{
                        HashMap<String, Object> value = (HashMap<String, Object>)child.getValue();
                        int curPassCode = ((Long)value.get(mPassCodeDbKey)).intValue();
                        if(curPassCode == passCode) {
//                            callback.OnPassCodeResovled(, (String) value.get(mRelativePathDbKey));
                            return Transaction.success(mutableData);
                        }
                    }catch (Exception ex){}
                }

                return Transaction.abort();
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean commited, DataSnapshot dataSnapshot) {
                if(databaseError != null || !commited){
                    if(mTransactionRetries.get() < 3){
                        mTransactionRetries.incrementAndGet();
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {}
                        GetRelativePath(passCode, callback);
                    }else {
                        if (databaseError != null) {
                            callback.OnPassCodeResolveFail("Error: " + databaseError.getDetails() + "\nMessage: " + databaseError.getMessage());
                        } else {
                            callback.OnPassCodeResolveFail("Failed to get db path. Unknow Error.");
                        }
                        mTransactionRetries.set(0);
                    }
                    return;
                }
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    try{
                        HashMap<String, Object> value = (HashMap<String, Object>)child.getValue();
                        int curPassCode = ((Long)value.get(mPassCodeDbKey)).intValue();
                        if(curPassCode == passCode) {
                            callback.OnPassCodeResovled(curPassCode, (String) value.get(mRelativePathDbKey), mUid);
                        }
                    }catch (Exception ex){}
                }
                mTransactionRetries.set(0);
            }
        });
    }

    public interface IPassCodeResolverCallback{
        void OnPassCodeResovled(Integer passCode, String relativeDbAndStoragePath, String userUid);
        void OnPassCodeResolveFail(String error);
    }
}
