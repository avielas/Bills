package com.bills.bills;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.bills.bills.firebase.FirebaseUploader;
import com.bills.bills.firebase.PassCodeResolver;
import com.bills.bills.fragments.BillSummarizerFragment;
import com.bills.bills.fragments.CameraFragment;
import com.bills.bills.fragments.WelcomeScreenFragment;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Core.MainActivityBase;
<<<<<<< Updated upstream
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;
import com.bills.billslib.CustomViews.ItemView;
import com.bills.billslib.CustomViews.NameView;
import com.bills.billslib.Utilities.FilesHandler;
=======
>>>>>>> Stashed changes
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class BillsMainActivity extends MainActivityBase implements
        WelcomeScreenFragment.OnFragmentInteractionListener,
        BillSummarizerFragment.OnFragmentInteractionListener,
        CameraFragment.OnFragmentInteractionListener{

    private String Tag = this.getClass().getSimpleName();
    private static final int RC_SIGN_IN = 123;
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private String mUid;

    //Fragments
    private BillSummarizerFragment mBillSummarizerFragment;
    private WelcomeScreenFragment mWelcomeFragment;
    private CameraFragment mCameraFragment;
    private Fragment mCurrentFragment;

    //Firebase Authentication members
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

<<<<<<< Updated upstream
    //Firebase Database members
    private FirebaseDatabase mFirebaseDataBase;
    private DatabaseReference mUsersDatabaseReference;
    private DatabaseReference mUserIdsDatabaseReference;
    private ChildEventListener mChildEventListener;

    private final String ImageType = "image/jpg";
    private final String ImageWidth = "width";
    private final String ImageHeight = "height";
    private final String Price = "Price";
    private final String Quantity = "Quantity";

    private final String mRelativePathDbKey = "RelativeDbPath";
    private final String mPassCodeDbKey = "PassCode";

    private String mNow;
    private String mBillRelativePath;

    private Hashtable<Integer, String> mPasCodeToUsersDbPathMapper = new Hashtable<>();

    //Firebase Storage members
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mBillsPerUserStorageReference;

    private String mUid;
    final AtomicInteger newPassCode = new AtomicInteger(Integer.MIN_VALUE);
    final AtomicBoolean newPassCodeRetrieved = new AtomicBoolean(false);
    final ConcurrentHashMap<String, Integer> passCodes = new ConcurrentHashMap<>();

=======
    private PassCodeResolver mPassCodeResolver;
>>>>>>> Stashed changes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

<<<<<<< Updated upstream
    private void ResolvePassCode() {
        //Generate unique PassCode and DB path
        mUserIdsDatabaseReference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData mutableData) {
                if (mutableData.hasChild(mUid)) {
                    HashMap<String, Object> value = (HashMap<String, Object>)mutableData.child(mUid).getValue();
                    DateFormat sdf = new SimpleDateFormat("yyyy_MM_dd___HH_mm_ss");
                    Date date = new Date();
                    mNow = sdf.format(date);

                    mBillRelativePath = mUid + "/" + mNow;
                    value.put(mRelativePathDbKey, mBillRelativePath);

                    newPassCode.set(((Long)(value.get(mPassCodeDbKey))).intValue());

                    mutableData.child(mUid).setValue(value);
                } else {
                        for (MutableData childMutableData : mutableData.getChildren()) {
                            passCodes.put(childMutableData.getKey(), ((Long)(((HashMap<String, Object>)childMutableData.getValue()).get(mPassCodeDbKey))).intValue());
                        }
                    //find an unused pass code
                    for (int i = 0; i < 10000; i++) {
                        if (!passCodes.containsValue(i)) {
                            mBillRelativePath = mUid + "/" + mNow;
                            Map<String, Object> userIdsValue = new HashMap<>();
                            userIdsValue.put(mPassCodeDbKey, i);
                            userIdsValue.put(mRelativePathDbKey, mBillRelativePath);
                            newPassCode.set(i);
                            mutableData.child(mUid).setValue(userIdsValue);
                            break;
                        }
                    }
                }
                return Transaction.success(mutableData);
            }

            //finished getting new PassCode
            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if(databaseError != null){
                    Toast.makeText(BillsMainActivity.this,
                            "Failed to get new pass code. Error: " + databaseError.getDetails(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                mUsersDatabaseReference = mFirebaseDataBase.getReference().child("users/" + mBillRelativePath);
                mPassCodeTextBox.setText(""+newPassCode.get());
                newPassCodeRetrieved.set(true);
            }
        });
=======
    public void onResume(){
        super.onResume();
        mAuth.addAuthStateListener(mAuthListener);
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
    public void OnCameraFinished(byte[] image) {
        String fileFullName = Constants.IMAGES_PATH + "/ocrBytes_" + mNow + ".txt";
        FilesHandler.SaveToTXTFile(image, fileFullName);
//        StartSummarizerView();
//        return;
        //upload the raw image to Storage
        mBillsPerUserStorageReference.child(mBillRelativePath + "/ocr.jpg").putBytes(image);

        mBillsMainView.removeView(mCameraCaptureButton);
        mBillsMainView.removeView(mCameraPreviewView);
        mBillsMainView.removeView(mCameraPreviewLayout);

        if(!newPassCodeRetrieved.get()){
            Toast.makeText(BillsMainActivity.this, "Failed to generate PassCode", Toast.LENGTH_LONG);
            StartPassCodeResolver();
            return;
        }
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap  bitmap = BitmapFactory.decodeByteArray(image, 0, image.length, bitmapOptions);

        if(bitmap.getHeight() < bitmap.getWidth()) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = rotatedImage;
//            ImageView imageVieww = new ImageView(this);
//            imageVieww.setImageBitmap(bitmap);
//            mBillsMainView.addView(imageVieww);
//            return;
        }

//        BitmapFactory.Options options = new BitmapFactory.Options();
//
//        bitmap = BitmapFactory.decodeFile(Constants.IMAGES_PATH+"/tmp.bmp", options);

        //removing prvious views

        AddBillSummarizerView();


        BillAreaDetector areaDetector = new BillAreaDetector();
        Point topLeft = new Point();
        Point topRight = new Point();
        Point buttomRight = new Point();
        Point buttomLeft = new Point();
        if (!OpenCVLoader.initDebug()) {
            Log.d(Tag, "Failed to initialize OpenCV.");
            finish();
        }

        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        if(!areaDetector.GetBillCorners(mat, topLeft,topRight, buttomRight, buttomLeft)){
            //TODO: add drag rect view here
            Log.d(Tag, "Failed\n");
            StartPassCodeResolver();
            return;
        }

        Mat warpedMat = new Mat();
        Mat warpedMatCopy = new Mat();
        try {
            warpedMat = ImageProcessingLib.WarpPerspective(mat, topLeft,topRight, buttomRight, buttomLeft);
            warpedMatCopy = warpedMat.clone();
        } catch (Exception e) {
            e.printStackTrace();
            //TODO: decide what to do. Retake the picture? crash the app?
            StartPassCodeResolver();
            return;
        }

//        Paint paint = new Paint();
//        paint.setColor(Color.RED);
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawCircle(Math.round(topLeft.x), Math.round(topLeft.y), 10, paint);
//        canvas.drawCircle(Math.round(topRight.x), Math.round(topRight.y), 10, paint);
//        canvas.drawCircle(Math.round(buttomLeft.x), Math.round(buttomLeft.y), 10, paint);
//        canvas.drawCircle(Math.round(buttomRight.x), Math.round(buttomRight.y), 10, paint);
//
//        ImageView imageView = new ImageView(this);
//        imageView.setImageBitmap(bitmap);
//        mBillsMainView.addView(imageView);
//
//        imageView = new ImageView(this);
//        imageView.setImageBitmap(warpedBitmap);
//        mBillsMainView.addView(imageView);
//        return;

        Bitmap processedBillBitmap = Bitmap.createBitmap(warpedMat.width(), warpedMat.height(), Bitmap.Config.ARGB_8888);
        ImageProcessingLib.PreprocessingForTM(warpedMat);
        Utils.matToBitmap(warpedMat, processedBillBitmap);

        TemplateMatcher templateMatcher = new TemplateMatcher(mOcrEngine, processedBillBitmap);
        try{
            templateMatcher.Match();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        ImageProcessingLib.PreprocessingForParsing(warpedMatCopy);
        int numOfItems = templateMatcher.priceAndQuantity.size();

        /***** we use processedBillBitmap second time to prevent another Bitmap allocation due to *****/
        /***** Out Of Memory when running 4 threads parallel                                      *****/
        Utils.matToBitmap(warpedMatCopy, processedBillBitmap);
        templateMatcher.InitializeBeforeSecondUse(processedBillBitmap);
        templateMatcher.Parsing(numOfItems);

        processedBillBitmap.recycle();
        warpedMat.release();
        warpedMatCopy.release();
        mat.release();

        //Upload all data to DB and Storage
        Map<String, Object> dbItems = new HashMap<>();

        int i = 0;
        for (Double[] row : templateMatcher.priceAndQuantity) {
            Buffer buffer = ByteBuffer.allocate(templateMatcher.itemLocationsByteArray.get(i).getByteCount());
            final Bitmap item = templateMatcher.itemLocationsByteArray.get(i);
            item.copyPixelsToBuffer(buffer);
            byte[] data = (byte[]) buffer.array();

            final StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType(ImageType)
                    .setCustomMetadata(ImageWidth, Integer.toString(templateMatcher.itemLocationsByteArray.get(i).getWidth()))
                    .setCustomMetadata(ImageHeight, Integer.toString(templateMatcher.itemLocationsByteArray.get(i).getHeight()))
                    .setCustomMetadata(Price, "" + row[0])
                    .setCustomMetadata(Quantity, "" + row[1].intValue())
                    .build();

            final Double rowPrice = row[0];
            final Double rowQuantity = row[1];
            final String itemIndex = "" + i;
            final Integer rowIndex = i;
            final StorageReference storageBillRef = mBillsPerUserStorageReference.child(mBillRelativePath + "/" + itemIndex);

            storageBillRef.putBytes(data).addOnSuccessListener(BillsMainActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                //upaded the data to Storage and DB, updating the UI
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageBillRef.updateMetadata(metadata);

                    if(mCommonLineToQuantityMapper.containsKey(rowIndex)){
                        return;
                    }

                    LinearLayout commonItemRow = new LinearLayout(BillsMainActivity.this);
                    commonItemRow.setOrientation(LinearLayout.VERTICAL);

                    LinearLayout myItemRow = new LinearLayout(BillsMainActivity.this);
                    myItemRow.setOrientation(LinearLayout.VERTICAL);

                    TextView commonPrice = new TextView(BillsMainActivity.this);
                    commonPrice.setText("" + rowPrice);
                    commonItemRow.addView(commonPrice);

                    TextView myPrice = new TextView(BillsMainActivity.this);
                    myPrice.setText("" + rowPrice);
                    myItemRow.addView(myPrice);

                    mLineNumToPriceMapper.put(rowIndex, rowPrice);

                    TextView commonQuantityView = new TextView(BillsMainActivity.this);
                    commonQuantityView.setText("" + rowQuantity);
                    commonItemRow.addView(commonQuantityView);

                    TextView myQuantityView = new TextView(BillsMainActivity.this);
                    myQuantityView.setText("0");
                    myItemRow.addView(myQuantityView);

                    ImageView commonImageView = new ImageView(BillsMainActivity.this);
                    commonImageView.setImageBitmap(item);
                    commonItemRow.addView(commonImageView);

                    ImageView myImageView = new ImageView(BillsMainActivity.this);
                    myImageView.setImageBitmap(item);
                    myItemRow.addView(myImageView);

                    LinearLayout commonItemsView = (LinearLayout) findViewById(R.id.commonBillView);
                    commonItemsView.addView(commonItemRow);

                    myItemRow.setVisibility(GONE);
                    LinearLayout myItemsView = (LinearLayout) findViewById(R.id.myBillViewView);
                    myItemsView.addView(myItemRow);

                    commonItemRow.setOnClickListener(BillsMainActivity.this);

                    mCommonLineToQuantityMapper.put(rowIndex, rowQuantity.intValue());
                    mCommonLineNumToLineView.put(rowIndex, commonItemRow);
                    mCommonLineNumberToQuantityView.put(rowIndex, commonQuantityView);

                    myItemRow.setOnClickListener(BillsMainActivity.this);

                    mMyLineToQuantityMapper.put(rowIndex, 0);
                    mMyLineNumToLineView.put(rowIndex, myItemRow);
                    mMyLineNumberToQuantityView.put(rowIndex, myQuantityView);
                }
            });
=======
    public void StartCameraFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
>>>>>>> Stashed changes

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, mCameraFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
        mCurrentFragment = mCameraFragment;
    }

    @Override
    public void StartSummarizerFragment(int passCode) {

        mPassCodeResolver.GetRelativePath(passCode, new PassCodeResolver.IPassCodeResolverCallback() {
            @Override
            public void OnPassCodeResovled(Integer passCode, String relativeDbAndStoragePath) {
                mBillSummarizerFragment.Init(BillsMainActivity.this.getApplicationContext(),
                        passCode,
                        "users/" + relativeDbAndStoragePath,
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
                StartWelcomeScreen();
            }
        });

    }

    @Override
    public void StartSummarizerFragment(final List<BillRow> rows, final Bitmap image) {
        mPassCodeResolver.GetPassCode(new PassCodeResolver.IPassCodeResolverCallback() {
            @Override
            public void OnPassCodeResovled(final Integer passCode, final String relativeDbAndStoragePath) {
                FirebaseUploader uploader = new FirebaseUploader("users/" + relativeDbAndStoragePath, "BillsPerUser/" + relativeDbAndStoragePath, BillsMainActivity.this);
                uploader.UploadRows(rows, image, new FirebaseUploader.IFirebaseUploaderCallback(){

                    @Override
                    public void OnSuccess() {
                        image.recycle();
                    }

                    @Override
                    public void OnFail(String message) {
                        Log.e(Tag, "Error accured while uploading bill rows. Error: " + message);
                        StartWelcomeScreen();
                    }
                });

                mBillSummarizerFragment.Init(BillsMainActivity.this.getApplicationContext(), passCode, relativeDbAndStoragePath, rows);

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
    public void StartWelcomeFragment() {
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
