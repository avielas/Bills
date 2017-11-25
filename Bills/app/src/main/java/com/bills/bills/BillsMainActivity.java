package com.bills.bills;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
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
import com.bills.billslib.Utilities.GMailSender;
import com.bills.billslib.Utilities.Utilities;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BillsMainActivity extends MainActivityBase implements
        WelcomeScreenFragment.OnFragmentInteractionListener,
        BillSummarizerFragment.OnFragmentInteractionListener,
        CameraFragment.OnFragmentInteractionListener{

    private String Tag = BillsMainActivity.class.getName();
    private static final int RC_SIGN_IN = 123;

    private static final String UsersDbKey = "users";
    private static final String BillsPerUserDbKey = "BillsPerUser";
    private final String RowsDbKey = "Rows";
    private String mUid;
    private Context mContext;
    private String mNowForFirstSession;
    private static Boolean mFirstEnteringInitCommonSession;
    private static Boolean mFirstEnteringInitPrivateSessionSecondUser;

    //Fragments
    private BillSummarizerFragment mBillSummarizerFragment;
    private WelcomeScreenFragment mWelcomeFragment;
    private CameraFragment mCameraFragment;
    private Fragment mCurrentFragment;

    //Firebase Authentication members
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    //Storage and DB paths
    private String mMyLogRootPath;
    private String mAppLogRootPath;
    private String mAppStoragePath;

    private PassCodeResolver mPassCodeResolver;
    private UUID mSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        SetDefaultUncaughtExceptionHandler();
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
                    InitPrivateSession();
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

    private void InitPrivateSession() {
        String now = Utilities.GetTimeStamp();

        //saving for later use as first Session folder at my logs
        mNowForFirstSession = now;
        mSessionId = UUID.randomUUID();
        mMyLogRootPath  = UsersDbKey + "/" + mUid + "/Logs/" + now;
        mAppLogRootPath = UsersDbKey + "/" + mUid;
        mAppStoragePath = BillsPerUserDbKey + "/" + mUid + "/" + now;
        mFirstEnteringInitCommonSession = true;
        mFirstEnteringInitPrivateSessionSecondUser = true;

        /**** First initialization of private session. Actually we initiate appFirebaseLogPath to  ****/
        /**** root path because at this stage we shouldn't print anything to application log path. ****/
        /**** Same for mAppStoragePath.                                                        ****/
        BillsLog.AddNewSession(mSessionId,
                               new FirebaseLogger(mUid,
                                                  mMyLogRootPath,
                                                  mAppLogRootPath));
        mPassCodeResolver = null;
        mPassCodeResolver = new PassCodeResolver(mUid, mAppLogRootPath, mNowForFirstSession);
    }

    private void InitCommonSession(){
        String now = mFirstEnteringInitCommonSession ? mNowForFirstSession : Utilities.GetTimeStamp();
        mSessionId = UUID.randomUUID();
        mAppStoragePath = BillsPerUserDbKey + "/" + mUid + "/" + now;
        BillsLog.AddNewSession(mSessionId,
                               new FirebaseLogger(mUid,
                                  mMyLogRootPath + "/" + now,
                                 mAppLogRootPath + "/" + now + "/Logs"));
        mFirstEnteringInitCommonSession = false;
        mPassCodeResolver.SetNow(now);
    }

    /***
     * The following function erase the previous set of DB/Storage paths. Actually it erase the set of InitPrivateSession
     * which should be re-defined for second user
     * @param userUid - current user id
     * @param relativeDbAndStoragePath - relative DB path of application. used to extract the application directory timestamp
     */
    private void InitPrivateSessionSecondUser(String userUid, String relativeDbAndStoragePath){
        String now = relativeDbAndStoragePath.split("/")[1];
        mSessionId = UUID.randomUUID();
        String nowNewSessionChild = mFirstEnteringInitPrivateSessionSecondUser ? now : Utilities.GetTimeStamp();
        mMyLogRootPath = UsersDbKey + "/" + userUid + "/Logs/" + now;
        mAppLogRootPath = UsersDbKey + "/" + relativeDbAndStoragePath + "/Logs";
        mAppStoragePath = BillsPerUserDbKey + "/" + userUid + "/" + nowNewSessionChild;
        BillsLog.AddNewSession(mSessionId,
                new FirebaseLogger(userUid,
                        mMyLogRootPath + "/" + nowNewSessionChild,
                        mAppLogRootPath));
        mFirstEnteringInitPrivateSessionSecondUser = false;
        mPassCodeResolver.SetNow(now);
    }

    private void UninitCommonSession(){
        BillsLog.UninitCommonSession(mSessionId, mMyLogRootPath);
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
            UninitCommonSession();
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
        InitCommonSession();
        mPassCodeResolver.GetPassCode(new PassCodeResolver.IPassCodeResolverCallback() {
            @Override
            public void OnPassCodeResovled(Integer passCode, String relativeDbAndStoragePath, String userUid) {
                mCameraFragment.Init(mSessionId, passCode, relativeDbAndStoragePath, mContext);
//                BillsLog.AddNewSession(mSessionId, new FirebaseLogger(userUid, "users/" + userUid, "users/" + relativeDbAndStoragePath));
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
                StartCameraFragment();
            }
        });
    }

    @Override
    public void StartSummarizerFragment(int passCode) {
        //TODO - BUG - see where the second user print logs instead of printing to
        //TODO - app logs
//        InitCommonSession();
        mPassCodeResolver.GetRelativePath(passCode, new PassCodeResolver.IPassCodeResolverCallback() {
            @Override
            public void OnPassCodeResovled(Integer passCode, String relativeDbAndStoragePath, String userUid) {
                InitPrivateSessionSecondUser(userUid, relativeDbAndStoragePath);
                mBillSummarizerFragment.Init(mSessionId,
                        BillsMainActivity.this.getApplicationContext(),
                        passCode,
                        "users/" + relativeDbAndStoragePath + "/" + RowsDbKey,
                        "BillsPerUser/" + relativeDbAndStoragePath);
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
                StartCameraFragment();
            }
        });

    }

    @Override
    public void StartSummarizerFragment(final List<BillRow> rows, final byte[] image,
                                        final Integer passCode, final String relativeDbAndStoragePath) {

        String rowDbKeyPath = UsersDbKey + "/" + relativeDbAndStoragePath + "/" + RowsDbKey;
        mBillSummarizerFragment.Init(mSessionId, BillsMainActivity.this.getApplicationContext(), passCode, rowDbKeyPath, rows);

        FirebaseUploader uploader = new FirebaseUploader(mSessionId, rowDbKeyPath, mAppStoragePath, BillsMainActivity.this);
        uploader.UploadRows(rows, image, new FirebaseUploader.IFirebaseUploaderCallback() {

            @Override
            public void OnSuccess() {}

            @Override
            public void OnFail(String message) {
                Log.e(Tag, "Error accured while uploading bill rows. Error: " + message);
                StartCameraFragment();
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
    public void StartCameraFragment(final byte[] image, String relativeDbAndStoragePath) {
        UploadBillImageToStorage(image, relativeDbAndStoragePath);
        StartCameraFragment();
    }

    private void UploadBillImageToStorage(byte[] image, String relativeDbAndStoragePath) {
        String relativeDbAndStoragePathToUpload = UsersDbKey + "/" + relativeDbAndStoragePath;
        FirebaseUploader uploader = new FirebaseUploader(mSessionId, relativeDbAndStoragePathToUpload, mAppStoragePath, BillsMainActivity.this);
        uploader.UploadFullBillImage(image);
    }

    @Override
    public void Finish() {
        finish();
    }

    @Override
    public void onFragmentInteraction() {

    }

    private void SetDefaultUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                GMailSender sender = new GMailSender("billsplitapplication@gmail.com", "billsplitapplicationisthebest");
                sender.SendEmail("Uncaught exception has been thrown",
                        paramThrowable.getMessage().toString(),
                        "billsplitapplication@gmail.com",
                        "billsplitapplication@gmail.com");
                System.exit(2);
                Log.e("BillsMainActivity", paramThrowable.getMessage());
            }
        });
    }
}
