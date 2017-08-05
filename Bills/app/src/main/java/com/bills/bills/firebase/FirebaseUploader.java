package com.bills.bills.firebase;

import android.app.Activity;
import android.graphics.Bitmap;

import com.bills.billslib.Contracts.BillRow;
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

    public FirebaseUploader(String dbPath, String storagePath, Activity activity){
        mFirebseDatabase = FirebaseDatabase.getInstance();
        mUsersDatabaseReference = mFirebseDatabase.getReference().child(dbPath);

        mFirebaseStorage = FirebaseStorage.getInstance();
        mBillsPerUserStorageReference = mFirebaseStorage.getReference().child(storagePath);

        mActivity = activity;
    }

    public void UploadRows(List<BillRow> rows, Bitmap fullBillImage, IFirebaseUploaderCallback callback) {
        //Upload full bill image
        Buffer fullBillBuffer = ByteBuffer.allocate(fullBillImage.getByteCount());
        fullBillImage.copyPixelsToBuffer(fullBillBuffer);
        byte[] fullBillData = (byte[]) fullBillBuffer.array();

        mBillsPerUserStorageReference.child("ocrBytes").putBytes(fullBillData);

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

            final Double rowPrice = row.GetPrice();
            final Integer rowQuantity = row.GetQuantity();
            final Integer rowIndex = row.GetRowIndex();
            final StorageReference storageBillRef = mBillsPerUserStorageReference.child(Integer.toString(row.GetRowIndex()));

            storageBillRef.putBytes(data).addOnSuccessListener(mActivity, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageBillRef.updateMetadata(metadata);
                }
            });
            dbItems.put(Integer.toString(rowIndex), rowQuantity);
        }

        mUsersDatabaseReference.updateChildren(dbItems);
    }

    public interface IFirebaseUploaderCallback{
        void OnSuccess();
        void OnFail(String message);
    }
}
