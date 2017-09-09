package com.bills.bills.firebase;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Utilities.FilesHandler;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
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

        mFirebaseStorage = FirebaseStorage.getInstance();
        mBillsPerUserStorageReference = mFirebaseStorage.getReference().child(storagePath);

        mActivity = activity;
    }

    public void UploadRows(final List<BillRow> rows, Bitmap fullBillImage, final IFirebaseUploaderCallback callback) {
        mUploadFailed.set(false);
        mUplodedRowsCounter.set(0);
        //Upload full bill image
        Buffer fullBillBuffer = ByteBuffer.allocate(fullBillImage.getByteCount());
        fullBillImage.copyPixelsToBuffer(fullBillBuffer);
        byte[] fullBillData = new byte[0];
        try {
            fullBillData = FilesHandler.ImageTxtFile2ByteArray (Constants.IMAGES_PATH + "/ocrBytes.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        final StorageMetadata ocrBytesMetadata = new StorageMetadata.Builder()
                .setContentType("text/plain")
                .setCustomMetadata(ImageWidth, Integer.toString(fullBillImage.getWidth()))
                .setCustomMetadata(ImageHeight, Integer.toString(fullBillImage.getHeight()))
                .build();

        final StorageReference storageFullBillRef = mBillsPerUserStorageReference.child("ocrBytes.txt");

        storageFullBillRef.putBytes(fullBillData).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                mUsersDatabaseReference.child("Logs").child("Info").setValue("Uploaded full bill image");
                storageFullBillRef.updateMetadata(ocrBytesMetadata).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mUsersDatabaseReference.child("Logs").child("Info").setValue("Uploaded MetaData");
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

        mUsersDatabaseReference.child(RowsDbKey).updateChildren(dbItems);
    }

    public void UploadFullBillImage(Bitmap fullBillImage, final String category, final String message) {
        //Upload full bill image
        Buffer fullBillBuffer = ByteBuffer.allocate(fullBillImage.getByteCount());
        fullBillImage.copyPixelsToBuffer(fullBillBuffer);
        byte[] fullBillData = new byte[0];
        try {
            fullBillData = FilesHandler.ImageTxtFile2ByteArray (Constants.IMAGES_PATH + "/ocrBytes.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        final StorageMetadata ocrBytesMetadata = new StorageMetadata.Builder()
                .setContentType("text/plain")
                .setCustomMetadata(ImageWidth, Integer.toString(fullBillImage.getWidth()))
                .setCustomMetadata(ImageHeight, Integer.toString(fullBillImage.getHeight()))
                .build();

        final StorageReference storageFullBillRef = mBillsPerUserStorageReference.child("ocrBytes.txt");

        storageFullBillRef.putBytes(fullBillData).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                storageFullBillRef.updateMetadata(ocrBytesMetadata).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("","");
                    }
                }).addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        mUsersDatabaseReference.child("Logs").child(category).setValue(message);
                    }
                });
            }
        });
    }

    public interface IFirebaseUploaderCallback{
        void OnSuccess();
        void OnFail(String message);
    }
}
