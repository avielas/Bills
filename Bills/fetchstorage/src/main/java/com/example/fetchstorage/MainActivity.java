package com.example.fetchstorage;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
import com.google.firebase.storage.StorageReference;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    Button _button;
    private String mUid;
    private static final int RC_SIGN_IN = 123;

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

                    mUsersDatabaseReference.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Log.d("","");
                            if(mutableData == null || mutableData.getKey() == null){
                                mUsersDatabaseReference.runTransaction(new Transaction.Handler() {
                                    @Override
                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                        Log.d("","");
                                        for (MutableData child : mutableData.getChildren()) {
                                            try {
                                                HashMap<String, Object> value = (HashMap<String, Object>) child.getValue();
                                                Object[] set = value.keySet().toArray();
//                                int curPassCode = ((Long)value.get(mPassCodeDbKey)).intValue();
//                                if(curPassCode == passCode) {
//                                    return Transaction.success(mutableData);
//                                }
                                            } catch (Exception ex) {
                                            }
                                        }

                                        return Transaction.abort();
                                    }

                                    @Override
                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                                    }
                                });
                            }

                            FirebaseStorage mFirebaseStorage;
                            StorageReference mBillsPerUserStorageReference;
                            mFirebaseStorage = FirebaseStorage.getInstance();


                            for (MutableData child : mutableData.getChildren()) {
                                try {
                                    HashMap<String, Object> value = (HashMap<String, Object>) child.getValue();
                                    Object[] set = value.keySet().toArray();

                                    for (String key: value.keySet()) {
                                        String pathToOcrBytes = "BillsPerUser/" + child.getKey().toString() + "/" + key + "/ocrBytes";
                                        mBillsPerUserStorageReference = mFirebaseStorage.getReference().child(pathToOcrBytes);
                                        mBillsPerUserStorageReference.getBytes(1024*1024*1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                Log.d("","");
//                                                ByteBuffer buffer = ByteBuffer.wrap(bytes);
//                                                Bitmap commonItemBitmap = Bitmap.createBitmap(itemWidth, itemHeight, Bitmap.Config.ARGB_8888);
//                                                commonItemBitmap.copyPixelsFromBuffer(buffer);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d("","");
                                            }
                                        });
                                    }

                                } catch (Exception ex) {
                                }
                            }

                            return Transaction.abort();
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
