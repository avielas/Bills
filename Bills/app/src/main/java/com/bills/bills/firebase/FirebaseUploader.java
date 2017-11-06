package com.bills.bills.firebase;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsPathToPrintTo;
import com.bills.billslib.Core.BillsLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by michaelvalershtein on 01/08/2017.
 */

public class FirebaseUploader {
    Activity mActivity;
    private String Tag = FirebaseUploader.class.getName();
    private FirebaseDatabase mFirebseDatabase;
    private DatabaseReference mUsersDatabaseReference;

    private FirebaseStorage mFirebaseStorage;
    private StorageReference mBillsPerUserStorageReference;

    private final String ImageType = "image/jpg";
    private final String ImageWidth = "width";
    private final String ImageHeight = "height";
    private final String Price = "Price";
    private final String Quantity = "Quantity";
    private final String RowsDbKey = "Rows";

    private final AtomicInteger mUplodedRowsCounter = new AtomicInteger(0);
    private AtomicBoolean mUploadFailed = new AtomicBoolean(false);

    public FirebaseUploader(String dbPath, String storagePath, Activity activity){
        mFirebseDatabase = FirebaseDatabase.getInstance();
        mUsersDatabaseReference = mFirebseDatabase.getReference().child(dbPath);
        mUsersDatabaseReference.keepSynced(true);

        mFirebaseStorage = FirebaseStorage.getInstance();
        mBillsPerUserStorageReference = mFirebaseStorage.getReference().child(storagePath);

        mActivity = activity;
    }

    public void UploadRows(final List<BillRow> rows, byte[] fullBillImage, final IFirebaseUploaderCallback callback) {
        mUploadFailed.set(false);
        mUplodedRowsCounter.set(0);
        //Upload full bill image
        final StorageMetadata ocrBytesMetadata = new StorageMetadata.Builder()
                .setContentType("text/plain")
                .build();

        final StorageReference storageFullBillRef = mBillsPerUserStorageReference.child("ocrBytes.txt");

        storageFullBillRef.putBytes(fullBillImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                BillsLog.Log(Tag, LogLevel.Info, "Uploaded full bill image", LogsPathToPrintTo.BothUsers);
                storageFullBillRef.updateMetadata(ocrBytesMetadata).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        BillsLog.Log(Tag, LogLevel.Info, "Uploaded full bill image MetaData", LogsPathToPrintTo.BothUsers);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mUploadFailed.set(true);
                callback.OnFail("Failed to upload full bill image");
            }
        });

        //Upload rows to Storage and to DB
        Map<String, Object> dbItems = new HashMap<>();

        for (BillRow row : rows) {
            Buffer buffer = ByteBuffer.allocate(row.GetItem().getByteCount());
            row.GetItem().copyPixelsToBuffer(buffer);
            byte[] data = (byte[]) buffer.array();

            final StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType(ImageType)
                    .setCustomMetadata(ImageWidth, Integer.toString(row.GetItem().getWidth()))
                    .setCustomMetadata(ImageHeight, Integer.toString(row.GetItem().getHeight()))
                    .setCustomMetadata(Price, Double.toString(row.GetPrice()))
                    .setCustomMetadata(Quantity, Integer.toString(row.GetQuantity()))
                    .build();

            final Integer rowQuantity = row.GetQuantity();
            final Integer rowIndex = row.GetRowIndex();
            final StorageReference storageBillRef = mBillsPerUserStorageReference.child(Integer.toString(row.GetRowIndex()));

            storageBillRef.putBytes(data).addOnSuccessListener(mActivity, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    if(mUploadFailed.get()){
                        return;
                    }
                    storageBillRef.updateMetadata(metadata);
                    if(mUplodedRowsCounter.incrementAndGet() == rows.size()){
                        callback.OnSuccess();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    mUploadFailed.set(true);
                    callback.OnFail("Failed to upload row: " + Integer.toString(rowIndex));
                }
            });
            dbItems.put(Integer.toString(rowIndex), rowQuantity);
        }

        mUsersDatabaseReference.updateChildren(dbItems);
    }

    public void UploadFullBillImage(byte[] fullBillImage) {
        //Upload full bill image
        final StorageMetadata ocrBytesMetadata = new StorageMetadata.Builder()
                .setContentType("text/plain")
                .build();

        final StorageReference storageFullBillRef = mBillsPerUserStorageReference.child("ocrBytes.txt");

        storageFullBillRef.putBytes(fullBillImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                BillsLog.Log(Tag, LogLevel.Info, "Uploaded full bill image", LogsPathToPrintTo.BothUsers);
                storageFullBillRef.updateMetadata(ocrBytesMetadata).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        BillsLog.Log(Tag, LogLevel.Info, "Uploaded full bill image MetaData", LogsPathToPrintTo.BothUsers);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                BillsLog.Log(Tag, LogLevel.Error, "Failed to upload full bill image", LogsPathToPrintTo.BothUsers);
            }
        });
    }

    public interface IFirebaseUploaderCallback{
        void OnSuccess();
        void OnFail(String message);
    }
}
