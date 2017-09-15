package com.example.fetchstorage;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Utilities.FilesHandler;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    Button _button;
    private String mUid;
    private static final int RC_SIGN_IN = 123;
    private static final String TAG = "MainActivity";
    private final String ImageWidth = "width";
    private final String ImageHeight = "height";
    private final String RowsDbKey = "Rows";
    private long dbUsersChildrenCount = 0;
    private long storageUserChildrenCount = 0;
    private long currBillParsedRowsCount = 0;
    private boolean isDownloading = false;
    public enum FileType{ Txt, Jpg};

    //Firebase Authentication members
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _button = (Button) findViewById(R.id.button);
        AddListenerPrintWordsLocationButton();

        //Firebase Authentication initialization
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    mUid = user.getUid();
                    Toast.makeText(MainActivity.this, "You are now signed in. Welcome", Toast.LENGTH_LONG).show();
                }else{
                    //user is signed out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.PHONE_VERIFICATION_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    public void AddListenerPrintWordsLocationButton() {
        _button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    FirebaseDatabase mFirebaseDatabase;
                    final DatabaseReference mUsersDatabaseReference;

                    String dbPath = "users";
                    mFirebaseDatabase = FirebaseDatabase.getInstance();
                    mUsersDatabaseReference = mFirebaseDatabase.getReference().child(dbPath);
                    mUsersDatabaseReference.keepSynced(true);

                    mUsersDatabaseReference.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(final MutableData mutableData) {
                            FirebaseStorage mFirebaseStorage;
                            StorageReference mBillsPerUserStorageReference;
                            mFirebaseStorage = FirebaseStorage.getInstance();
                            dbUsersChildrenCount = mutableData.getChildrenCount();
                            int i = 0;
                            for (final MutableData userId : mutableData.getChildren()) {
                                try {
                                    final HashMap<String, Object> timeStamps = (HashMap<String, Object>) userId.getValue();
                                    isDownloading = true;
                                    storageUserChildrenCount = timeStamps.keySet().size();
                                    int j = 0;
                                    for (final String timeStamp: timeStamps.keySet()) {
                                        String pathToOcrBytes = "BillsPerUser/" + userId.getKey().toString() + "/" + timeStamp + "/ocrBytes.txt";
                                        mBillsPerUserStorageReference = mFirebaseStorage.getReference().child(pathToOcrBytes);
                                        mBillsPerUserStorageReference.getBytes(1024 * 1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                Log.d(timeStamp, userId.getKey());
                                                String phone = userId.getKey();
                                                String path = Constants.FIREBASE_LOCAL_STORAGE + "/" + phone + "/" + timeStamp;
                                                SaveImageToDisk(path, bytes, "ocrBytes.txt", FileType.Txt, 0, 0);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                            }
                                        });

                                        ArrayList<Long> billQuantityLines = (ArrayList<Long>) mutableData.child(userId.getKey()).child(timeStamp).child(RowsDbKey).getValue();
                                        if (null != billQuantityLines){
                                            currBillParsedRowsCount = billQuantityLines.size();
                                            for (int row=0; row < billQuantityLines.size(); row++) {
                                                final int tempRow = row;
                                                String pathToRowQuantityImage = "BillsPerUser/" + userId.getKey().toString() + "/" + timeStamp + "/" + tempRow;
                                                mBillsPerUserStorageReference = mFirebaseStorage.getReference().child(pathToRowQuantityImage);
                                                final StorageReference billsPerUserStorageReference = mBillsPerUserStorageReference;
                                                final int finalI = i;
                                                final int finalJ = j;
                                                final int finalRow = row;
                                                billsPerUserStorageReference.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                                    @Override
                                                    public void onSuccess(final byte[] bytes) {
                                                        billsPerUserStorageReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                                            @Override
                                                            public void onSuccess(StorageMetadata storageMetadata) {
                                                                Integer itemHeight = Integer.parseInt(storageMetadata.getCustomMetadata(ImageHeight));
                                                                Integer itemWidth = Integer.parseInt(storageMetadata.getCustomMetadata(ImageWidth));
                                                                String phone = userId.getKey();
                                                                String path = Constants.FIREBASE_LOCAL_STORAGE + "/" + phone + "/" + timeStamp;
                                                                SaveImageToDisk(path, bytes, tempRow + ".jpg", FileType.Jpg, itemWidth, itemHeight);
                                                                if(finalI == dbUsersChildrenCount - 1 &&
                                                                   finalJ == storageUserChildrenCount - 1 &&
                                                                   finalRow == currBillParsedRowsCount - 1) {
                                                                    Toast.makeText(MainActivity.this, "finished! the location is " + Constants.FIREBASE_LOCAL_STORAGE, Toast.LENGTH_LONG).show();
                                                                }
                                                            }
                                                        });

                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                    }
                                                });
                                            }
                                        }
                                        j++;
                                    }

                                } catch (Exception ex) {
                                    Log.d("fetchstorage", ex.getMessage());
                                }
                                i++;
                            }
                            return Transaction.abort();
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                            if(isDownloading) {
                                Toast.makeText(MainActivity.this, "Downloading Storage from Firebase to "+ Constants.FIREBASE_LOCAL_STORAGE, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void SaveImageToDisk(String path, byte[] bytes, String fileName, FileType fileType, Integer itemWidth, Integer itemHeight) {

        File dir = new File(path);

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "ERROR: Creation of directory " + path + " failed, check does Android Manifest have permission to write to external storage.");
            }
        } else {
            Log.i(TAG, "Created directory " + path);
        }

        File file = new File(dir + "/" + fileName);
        if(!file.exists()) {
            switch (fileType) {
                case Jpg:
                    Bitmap bitmap = FilesHandler.ConvertFirebaseBytesToBitmap(bytes, itemWidth, itemHeight); //FilesHandler.ByteArrayToBitmap(bytes);
                    FilesHandler.SaveToJPGFile(bitmap, path + "/" + fileName);
                    bitmap.recycle();
                    break;
                case Txt:
                    FilesHandler.SaveToTXTFile(bytes, path + "/" + fileName);
                    break;
            }
        }
    }

    public void onResume(){
        super.onResume();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
//        //TODO: clear all displayed data
    }
}
