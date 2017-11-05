package com.bills.bills;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.bills.bills.firebase.FirebaseLogger;
import com.bills.bills.firebase.FirebaseUploader;
import com.bills.bills.firebase.PassCodeResolver;
import com.bills.bills.fragments.BillSummarizerFragment;
import com.bills.billslib.Fragments.CameraFragment;
import com.bills.bills.fragments.WelcomeScreenFragment;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Core.MainActivityBase;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class BillsMainActivity extends MainActivityBase implements
        WelcomeScreenFragment.OnFragmentInteractionListener,
        BillSummarizerFragment.OnFragmentInteractionListener,
        CameraFragment.OnFragmentInteractionListener{

    private String Tag = BillsMainActivity.class.getName();
    private static final int RC_SIGN_IN = 123;
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private static final String UsersDbKey = "users";
    private static final String BillsPerUserDbKey = "BillsPerUser";
    private final String RowsDbKey = "Rows";
    private String mUid;
    private Context mContext;
    //Fragments
    private BillSummarizerFragment mBillSummarizerFragment;
    private WelcomeScreenFragment mWelcomeFragment;
    private CameraFragment mCameraFragment;
    private Fragment mCurrentFragment;

    //Firebase Authentication members
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;


    private PassCodeResolver mPassCodeResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_bills_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mBillSummarizerFragment = new BillSummarizerFragment();
        mWelcomeFragment = new WelcomeScreenFragment();
        mCameraFragment = new CameraFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, mWelcomeFragment).commit();
        mCurrentFragment = mWelcomeFragment;

        //Firebase Authentication initialization
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    mUid = user.getUid();
                    mPassCodeResolver = new PassCodeResolver(mUid);

                    Toast.makeText(BillsMainActivity.this, "You are now signed in. Welcome", Toast.LENGTH_LONG).show();
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
        StartWelcomeScreen();
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

    @Override
    public void onBackPressed(){
        if(mCurrentFragment == mCameraFragment || mCurrentFragment == mBillSummarizerFragment){
            StartWelcomeScreen();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    public void StartWelcomeScreen() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, mWelcomeFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
        mCurrentFragment = mWelcomeFragment;
    }

    @Override
    public void StartCameraFragment() {
        mPassCodeResolver.GetPassCode(new PassCodeResolver.IPassCodeResolverCallback() {
            @Override
            public void OnPassCodeResovled(Integer passCode, String relativeDbAndStoragePath, String userUid) {

                mCameraFragment.Init(mContext, passCode, relativeDbAndStoragePath);
                BillsLog.Init(new FirebaseLogger(userUid, "users/" + userUid, "users/" + relativeDbAndStoragePath));
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                // Replace whatever is in the fragment_container view with this fragment,
                // and add the transaction to the back stack so the user can navigate back
                transaction.replace(R.id.fragment_container, mCameraFragment);
                transaction.addToBackStack(null);

                // Commit the transaction
                transaction.commit();
                mCurrentFragment = mCameraFragment;
            }

            @Override
            public void OnPassCodeResolveFail(String error) {
                Toast.makeText(BillsMainActivity.this, "Failed to get passCode...", Toast.LENGTH_SHORT).show();
                StartWelcomeScreen();
            }
        });
    }

    @Override
    public void StartSummarizerFragment(int passCode) {

        mPassCodeResolver.GetRelativePath(passCode, new PassCodeResolver.IPassCodeResolverCallback() {
            @Override
            public void OnPassCodeResovled(Integer passCode, String relativeDbAndStoragePath, String userUid) {
                mBillSummarizerFragment.Init(BillsMainActivity.this.getApplicationContext(),
                        passCode,
                        "users/" + relativeDbAndStoragePath + "/" + RowsDbKey,
                        "BillsPerUser/" + relativeDbAndStoragePath);
                BillsLog.Init(new FirebaseLogger(userUid, "users/" + userUid, "users/" + relativeDbAndStoragePath));
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, mBillSummarizerFragment);
                transaction.addToBackStack(null);

                // Commit the transaction
                transaction.commit();
                mCurrentFragment = mBillSummarizerFragment;
            }

            @Override
            public void OnPassCodeResolveFail(String error) {
                Toast.makeText(BillsMainActivity.this, "Failed to get passCode...", Toast.LENGTH_SHORT).show();
                StartWelcomeScreen();
            }
        });

    }

    @Override
    public void StartSummarizerFragment(final List<BillRow> rows, final byte[] image,
                                        final Integer passCode, final String relativeDbAndStoragePath) {

                mBillSummarizerFragment.Init(BillsMainActivity.this.getApplicationContext(), passCode,
                        "users/" + relativeDbAndStoragePath + "/" + RowsDbKey, rows);

        FirebaseUploader uploader = new FirebaseUploader(UsersDbKey + "/" + relativeDbAndStoragePath + "/" + RowsDbKey,
                BillsPerUserDbKey + "/" + relativeDbAndStoragePath, BillsMainActivity.this);
        uploader.UploadRows(rows, image, new FirebaseUploader.IFirebaseUploaderCallback() {

            @Override
            public void OnSuccess() {}

            @Override
            public void OnFail(String message) {
                Log.e(Tag, "Error accured while uploading bill rows. Error: " + message);
                StartWelcomeScreen();
            }
        });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, mBillSummarizerFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
        mCurrentFragment = mBillSummarizerFragment;
    }

    @Override
    public void StartWelcomeFragment() {
        StartWelcomeScreen();
    }

    @Override
    public void StartWelcomeFragment(final byte[] image) {
        mPassCodeResolver.GetPassCode(new PassCodeResolver.IPassCodeResolverCallback(){
            @Override
            public void OnPassCodeResovled(final Integer passCode, final String relativeDbAndStoragePath, final String userUid) {
                FirebaseUploader uploader = new FirebaseUploader(UsersDbKey + "/" + relativeDbAndStoragePath,
                        BillsPerUserDbKey + "/" + relativeDbAndStoragePath, BillsMainActivity.this);
                uploader.UploadFullBillImage(image);
            }

            @Override
            public void OnPassCodeResolveFail(String error) {
                Toast.makeText(BillsMainActivity.this, "Failed to get passCode...", Toast.LENGTH_SHORT).show();
            }
        });

        StartWelcomeScreen();
    }

    @Override
    public void Finish() {
        finish();
    }

    @Override
    public void onFragmentInteraction() {

    }
}
