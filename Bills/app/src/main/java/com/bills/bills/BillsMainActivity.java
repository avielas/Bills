package com.bills.bills;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bills.billslib.Camera.CameraRenderer;
import com.bills.billslib.Camera.IOnCameraFinished;
import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.Language;
import com.bills.billslib.Contracts.Interfaces.IOcrEngine;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Core.MainActivityBase;
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;
import com.bills.billslib.CustomViews.ItemView;
import com.bills.billslib.CustomViews.NameView;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.View.GONE;

public class BillsMainActivity extends MainActivityBase implements IOnCameraFinished, View.OnClickListener {
    private String Tag = this.getClass().getSimpleName();
    private static final int RC_SIGN_IN = 123;
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private boolean mCameraViewEnabled = false;

    private ConcurrentHashMap<Integer, Integer> mCommonLineToQuantityMapper = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, LinearLayout> mCommonLineNumToLineView = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, TextView> mCommonLineNumberToQuantityView = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Integer, Integer> mMyLineToQuantityMapper = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, LinearLayout> mMyLineNumToLineView = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, TextView> mMyLineNumberToQuantityView = new ConcurrentHashMap<>();

    private HashMap<Integer, Double> mLineNumToPriceMapper = new HashMap<>();
    private Double mMyTotalSum = 0.0;

    //Camera Elements
    private RelativeLayout mCameraPreviewLayout = null;
    private TextureView mCameraPreviewView = null;
    private Button mCameraCaptureButton = null;

    //Summarizer Elements
    private LinearLayout mBillSummarizerContainerView = null;
    private EditText mBillSummarizerTip = null;
    private LinearLayout mBillSummarizerCommonItemsLayout = null;
    private LinearLayout mBillSummarizerMyItemsLayout = null;
    private TextView mBillSummarizerTotalSum = null;

    //Main bills layout
    private LinearLayout mBillsMainView;

    //PassCode resolver elements
    private Button mStartCameraButton = null;
    private Button mCheckPassCodeButton = null;
    private EditText mPassCodeTextBox = null;

    //Camera Renderer
    private CameraRenderer mRenderer;

    private double mTip = 10;

    private IOcrEngine mOcrEngine;

    //Firebase members

    //Firebase Authentication members
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

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

    private String mBillRelativePath;

    private Hashtable<Integer, String> mPasCodeToUsersDbPathMapper = new Hashtable<>();

    //Firebase Storage members
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mBillsPerUserStorageReference;

    private String mUid;
    final AtomicInteger newPassCode = new AtomicInteger(Integer.MIN_VALUE);
    final AtomicBoolean newPassCodeRetrieved = new AtomicBoolean(false);
    final ConcurrentHashMap<String, Integer> passCodes = new ConcurrentHashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bills_main);

        mBillsMainView = (LinearLayout) findViewById(R.id.activity_bills_main);
        mBillSummarizerTotalSum = (TextView)findViewById(R.id.totalSum);
        mBillSummarizerTip = (EditText)findViewById(R.id.tipTextView);
        mBillSummarizerCommonItemsLayout = (LinearLayout)findViewById(R.id.commonBillView);
        mBillSummarizerMyItemsLayout = (LinearLayout)findViewById(R.id.myBillViewView);
        mBillSummarizerContainerView = (LinearLayout)findViewById(R.id.summarizerContainerView);

        mBillSummarizerTotalSum.setVisibility(GONE);
        mBillSummarizerTip.setVisibility(GONE);
        mBillSummarizerCommonItemsLayout.setVisibility(GONE);
        mBillSummarizerMyItemsLayout.setVisibility(GONE);
        mBillSummarizerContainerView.setVisibility(GONE);

        mRenderer = new CameraRenderer(this);
        mRenderer.SetOnCameraFinishedListener(this);

        if(mOcrEngine == null){
            try {
                mOcrEngine = new TesseractOCREngine();
                mOcrEngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
            }catch (Exception ex){
                TextView textView = new TextView(this);
                textView.setText("Failed to initialize " + mOcrEngine.getClass().getSimpleName() + ". Error: " + ex.getMessage());
                mBillsMainView.addView(textView);
                return;
            }
        }

        //Firebase Authentication initialization
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    mUid = user.getUid();
                    StartPassCodeResolver();
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

        //Firebase init DB and Storage
        mFirebaseDataBase = FirebaseDatabase.getInstance();

        mUserIdsDatabaseReference = mFirebaseDataBase.getReference().child("userIds/");
        //Add listener to populate userIds from DB. this is useful for all BillConsumer users(not taking the bill photo)
        mUserIdsDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Integer passCode = 0;
                String usersDbPath;
                try{
                    HashMap<String, Object> value = ((HashMap<String, Object>)dataSnapshot.getValue());
                    passCode = ((Long)value.get(mPassCodeDbKey)).intValue();
                    usersDbPath = (String)value.get(mRelativePathDbKey);
                }catch (Exception ex){
                    return;
                }
                mPasCodeToUsersDbPathMapper.put(passCode, usersDbPath);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Integer passCode = 0;
                try{
                    passCode = dataSnapshot.getValue(Integer.class);
                }catch (Exception ex){
                    return;
                }

                mPasCodeToUsersDbPathMapper.put(passCode, dataSnapshot.getKey());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                try {
                    mPasCodeToUsersDbPathMapper.remove(dataSnapshot.getValue(Integer.class));
                }catch (Exception ex){
                    return;
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        mFirebaseStorage = FirebaseStorage.getInstance();
        mBillsPerUserStorageReference = mFirebaseStorage.getReference().child("BillsPerUser");

    }

    private void StartPassCodeResolver() {

        if(mCameraViewEnabled) {
            mBillsMainView.addView(mStartCameraButton);
            mBillsMainView.addView(mCheckPassCodeButton);
            mBillsMainView.addView(mPassCodeTextBox);
            mCameraViewEnabled = false;
            return;
        }

        mStartCameraButton = new Button(this);
        mStartCameraButton.setText("Start Camera");
        mStartCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBillsMainView.removeView(mStartCameraButton);
                mBillsMainView.removeView(mCheckPassCodeButton);
                mBillsMainView.removeView(mPassCodeTextBox);

                ResolvePassCode();

                StartCameraActivity();
            }
        });

        mBillsMainView.addView(mStartCameraButton);

        mCheckPassCodeButton = new Button(this);
        mCheckPassCodeButton.setText("Check Pass Code");
        mCheckPassCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Integer passCode;
                try{
                    passCode = Integer.parseInt(mPassCodeTextBox.getText().toString());
                }catch(Exception ex){
                    Toast.makeText(BillsMainActivity.this, "Invalid Pass Code", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(mPasCodeToUsersDbPathMapper.containsKey(passCode)){
                    mBillsMainView.removeView(mStartCameraButton);
                    mBillsMainView.removeView(mCheckPassCodeButton);
                    mBillsMainView.removeView(mPassCodeTextBox);

                    mUsersDatabaseReference = mFirebaseDataBase.getReference().child("users").child(mPasCodeToUsersDbPathMapper.get(passCode));

                    AddBillSummarizerView();

                    mUsersDatabaseReference.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            final Integer rowIndex = Integer.parseInt(dataSnapshot.getKey());

                            final StorageReference curLineStorageReference = mBillsPerUserStorageReference.child(mPasCodeToUsersDbPathMapper.get(passCode)).child(Integer.toString(rowIndex));

                            curLineStorageReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                @Override
                                public void onSuccess(StorageMetadata storageMetadata) {

                                    final long ONE_MEGABYTE = 1024 * 1024;
                                    final String rowPrice;
                                    final String rowQuantity;
                                    final Integer itemHeight;
                                    final Integer itemWidth;
                                    try {
                                        rowPrice = storageMetadata.getCustomMetadata(Price);
                                        rowQuantity = storageMetadata.getCustomMetadata(Quantity);
                                        itemHeight = Integer.parseInt(storageMetadata.getCustomMetadata(ImageHeight));
                                        itemWidth = Integer.parseInt(storageMetadata.getCustomMetadata(ImageWidth));
                                    }catch (Exception e){
                                        Log.d("","");
                                        return;
                                    }
                                    curLineStorageReference.getBytes(3 * ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                        @Override
                                        public void onSuccess(byte[] bytes) {
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

                                            TextView commonQuantityView = new TextView(BillsMainActivity.this);
                                            commonQuantityView.setText("" + rowQuantity);
                                            commonItemRow.addView(commonQuantityView);

                                            TextView myQuantityView = new TextView(BillsMainActivity.this);
                                            myQuantityView.setText("0");
                                            myItemRow.addView(myQuantityView);

                                            ByteBuffer buffer = ByteBuffer.wrap(bytes);
                                            Bitmap commonItemBitmap = Bitmap.createBitmap(itemWidth, itemHeight, Bitmap.Config.ARGB_8888);
                                            commonItemBitmap.copyPixelsFromBuffer(buffer);

                                            buffer = ByteBuffer.wrap(bytes);
                                            Bitmap myItemBitmap = Bitmap.createBitmap(itemWidth, itemHeight, Bitmap.Config.ARGB_8888);
                                            myItemBitmap.copyPixelsFromBuffer(buffer);

                                            ImageView commonImageView = new ImageView(BillsMainActivity.this);
                                            commonImageView.setImageBitmap(commonItemBitmap);
                                            commonItemRow.addView(commonImageView);

                                            ImageView myImageView = new ImageView(BillsMainActivity.this);
                                            myImageView.setImageBitmap(myItemBitmap);
                                            myItemRow.addView(myImageView);

                                            LinearLayout commonItemsView = (LinearLayout) findViewById(R.id.commonBillView);
                                            commonItemsView.addView(commonItemRow);

                                            LinearLayout myItemsView = (LinearLayout) findViewById(R.id.myBillViewView);
                                            myItemsView.addView(myItemRow);
                                            myItemRow.setVisibility(GONE);

                                            Integer rowIndexParsed = rowIndex;
                                            Integer rowQuantityPrsed = Integer.parseInt(rowQuantity);
                                            Double rowPriceParsed = Double.parseDouble(rowPrice);
                                            mLineNumToPriceMapper.put(rowIndex, rowPriceParsed);

                                            commonItemRow.setOnClickListener(BillsMainActivity.this);

                                            mCommonLineToQuantityMapper.put(rowIndexParsed, rowQuantityPrsed);
                                            mCommonLineNumToLineView.put(rowIndexParsed, commonItemRow);
                                            mCommonLineNumberToQuantityView.put(rowIndexParsed, commonQuantityView);

                                            myItemRow.setOnClickListener(BillsMainActivity.this);

                                            mMyLineToQuantityMapper.put(rowIndexParsed, 0);
                                            mMyLineNumToLineView.put(rowIndexParsed, myItemRow);
                                            mMyLineNumberToQuantityView.put(rowIndexParsed, myQuantityView);
                                        }
                                    });
                                }
                            });

                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                            Integer index = Integer.parseInt(dataSnapshot.getKey());
                            Integer newQuantity = dataSnapshot.getValue(Integer.class);

                            if(newQuantity <= 0){
                                //nothing to update at common items view
                                if(!mCommonLineNumToLineView.containsKey(index)){
                                    return;
                                }

                                mCommonLineNumberToQuantityView.get(index).setText("0");
                                mCommonLineNumToLineView.get(index).setVisibility(GONE);
                                return;
                            }else{

                                mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                                mCommonLineNumberToQuantityView.get(index).setText(""+newQuantity);
                                mCommonLineToQuantityMapper.put(index, newQuantity);
                            }
                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }else{
                    Toast.makeText(BillsMainActivity.this, "Pass code not found, Try again", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBillsMainView.addView(mCheckPassCodeButton);

        mPassCodeTextBox = new EditText(this);
        mPassCodeTextBox.setInputType(InputType.TYPE_CLASS_NUMBER);
        mPassCodeTextBox.setHint("Enter pass code");

        mBillsMainView.addView(mPassCodeTextBox);
    }

    private void ResolvePassCode() {
        //Generate unique PassCode and DB path
        mUserIdsDatabaseReference.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(final MutableData mutableData) {
                if (mutableData.hasChild(mUid)) {
                    HashMap<String, Object> value = (HashMap<String, Object>)mutableData.child(mUid).getValue();
                    DateFormat sdf = new SimpleDateFormat("yyyy_MM_dd___HH_mm_ss");
                    Date date = new Date();
                    String now = sdf.format(date);

                    mBillRelativePath = mUid + "/" + now;
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
                            DateFormat sdf = new SimpleDateFormat("yyyy_MM_dd___HH_mm_ss");
                            Date date = new Date();
                            String now = sdf.format(date);

                            mBillRelativePath = mUid + "/" + now;

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
    }

    private void StartSummarizerView() {
        AddBillSummarizerView();


        int numOfEntries = 5;
        int color = Color.WHITE;
        Bitmap[] Items = CreateItems(numOfEntries);
        Double[] prices = {12.3, 34.0, 50.0, 45.0, 55.0};
        for(int i = 0; i < numOfEntries; i++){
            ItemView itemView = new ItemView(this, prices[i], Items[i]);
            itemView.SetItemBackgroundColor(color);
            itemView.setOnClickListener(this);
            mBillSummarizerCommonItemsLayout.addView(itemView, i);
            mMyTotalSum +=prices[i];
        }

        NameView nameView = new NameView(this, "Aviel", 10);
        nameView.setBackgroundColor(Color.RED);
        nameView.setOnClickListener(this);
        mBillSummarizerMyItemsLayout.addView(nameView);
        nameView = new NameView(this, "Mike", 10);
        nameView.setBackgroundColor(Color.BLUE);
        nameView.setOnClickListener(this);
        mBillSummarizerMyItemsLayout.addView(nameView);


    }

    private Bitmap[] CreateItems(int numOfEntries) {
        Bitmap[] res = new Bitmap[numOfEntries];

        for (int i = 0; i < numOfEntries; i++){
            res[i] = CreateItemBitmap(i);
        }
        return res;
    }

    private Bitmap CreateItemBitmap(int i) {
        int width = 50;
        int height = 30;

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        // The gesture threshold expressed in dip
        float GESTURE_THRESHOLD_DIP = 12.0f;

        final float scale = getResources().getDisplayMetrics().density;
        int gestureThreshold = (int) (GESTURE_THRESHOLD_DIP * scale + 0.5f);

        paint.setTextSize(gestureThreshold);
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap res = Bitmap.createBitmap((int) (width * scale + 0.5f),(int) (height * scale + 0.5f), conf);

        Canvas canvas = new Canvas(res);

        canvas.drawText("" + i + i + i, 30, 30, paint);

        return res;
    }

    private void StartCameraActivity() {
        try {
            mCameraViewEnabled = true;
            mCameraPreviewLayout = new RelativeLayout(this);
            mBillsMainView.addView(mCameraPreviewLayout);

            mCameraPreviewView = new TextureView(this);
            mCameraPreviewView.setSurfaceTextureListener(mRenderer);
            mCameraPreviewView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            //mRenderer.set_selectedFilter(R.id.filter0);
                            mRenderer.setAutoFocus();
                            break;
                    }
                    return true;
                }
            });
            mCameraPreviewView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    mRenderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
                }
            });

            mCameraPreviewLayout.addView(mCameraPreviewView);

            mCameraCaptureButton = new Button(this);
            mCameraCaptureButton.setText("Capture");
            mCameraCaptureButton.setOnClickListener(this);

            RelativeLayout.LayoutParams buttonLayoutParameters = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            buttonLayoutParameters.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            buttonLayoutParameters.addRule(RelativeLayout.CENTER_HORIZONTAL);

            mCameraPreviewLayout.addView(mCameraCaptureButton, buttonLayoutParameters);
        } catch (Exception e) {
            Log.e(Tag, e.getMessage());
        }
    }

    @Override
    public void OnCameraFinished(byte[] image) {

//        StartSummarizerView();
//        return;
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

            dbItems.put(itemIndex, row[1].intValue());

            i++;
        }

        mUsersDatabaseReference.updateChildren(dbItems);

        TextView passCodeTextView = (TextView) findViewById(R.id.passCode);
        passCodeTextView.setText(""+newPassCode.get());

        AddBillSummarizerView();

        mUsersDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Integer index = Integer.parseInt(dataSnapshot.getKey());
                Integer newQuantity = dataSnapshot.getValue(Integer.class);

                //Add one to common items
                if(newQuantity > mCommonLineToQuantityMapper.get(index)){

                }
                //remove one from common items
                else if(newQuantity < mCommonLineToQuantityMapper.get(index)){

                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void AddBillSummarizerView() {
        mBillSummarizerContainerView.setVisibility(View.VISIBLE);
        mBillSummarizerTip.setVisibility(View.VISIBLE);
        mBillSummarizerCommonItemsLayout.setVisibility(View.VISIBLE);
        mBillSummarizerMyItemsLayout.setVisibility(View.VISIBLE);
        mBillSummarizerTotalSum.setVisibility(View.VISIBLE);

        mBillSummarizerTip.setClickable(true);
        mBillSummarizerTip.addTextChangedListener(new TextWatcher() {
            private String curTip = "10";
            public void afterTextChanged(Editable s) {
                if(s.toString().equalsIgnoreCase("")) {
                    mTip = 0;
                }else {
                    int newTip = Integer.parseInt(s.toString());
                    if (newTip < 0 || newTip > 100) {
                        mBillSummarizerTip.setText(curTip);
                    } else {
                        curTip = s.toString();
                        mTip = (1.0*newTip)/100;

                    }
                }

            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });
        mBillSummarizerTotalSum.setClickable(false);
        mBillSummarizerTotalSum.setText("");
        mBillSummarizerTip.setText("10", TextView.BufferType.EDITABLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    StartPassCodeResolver();

//                    StartCameraActivity();
//                    StartSummarizerView();

                }
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

    @Override
    public void onBackPressed() {
        if(mCameraViewEnabled) {
            StartPassCodeResolver();
        }else{
            super.onBackPressed();
        }
    }
    @Override
    public void onClick(View v) {

        if(v == mBillSummarizerTip) {

            return;
        }
        if (v == mCameraCaptureButton){
            mRenderer.takePicture();
            return;
        }

        //move item from my Bill to common Bill
        if(((LinearLayout)v.getParent()).getId() == R.id.myBillViewView) {
            //find relevant entry
            for (HashMap.Entry<Integer, LinearLayout> entry : mMyLineNumToLineView.entrySet()) {
                if(entry.getValue() == v){
                    Integer index = entry.getKey();
                    if(mMyLineToQuantityMapper.get(index) == 1){ // Line should be removed from my view and added to common view
                        mMyLineNumToLineView.get(index).setVisibility(GONE);
                        mMyLineToQuantityMapper.put(index, 0);
                        mMyLineNumberToQuantityView.get(index).setText("0");
                    }else if(mMyLineToQuantityMapper.get(index) > 1){ //Line should be moved to common view
                        mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index) - 1);
                        mMyLineNumberToQuantityView.get(index).setText(""+mMyLineToQuantityMapper.get(index));
                    }

                    //Line in common view should be updated
                    if(mCommonLineToQuantityMapper.get(index) > 0){
                        mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index ) + 1);
                        mCommonLineNumberToQuantityView.get(index).setText(""+mCommonLineToQuantityMapper.get(index));
                    }else{ //Line in common view shlould be added
                        mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mCommonLineNumberToQuantityView.get(index).setText("1");
                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index) + 1);
                    }

                    mUsersDatabaseReference.child(Integer.toString(index)).setValue(mCommonLineToQuantityMapper.get(index));

                    mMyTotalSum -= mLineNumToPriceMapper.get(index);
                    mBillSummarizerTotalSum.setText(Double.toString(mMyTotalSum *(1+mTip)));
                    return;
                }
            }
            //TODO: what to do if entry not found?
            Log.e(Tag, "did not find the line: " + v.getId());
        }

        //move item from common Bill to my Bill and substract 1 from item's quantity
        if(((LinearLayout) v.getParent()).getId() == R.id.commonBillView){
            for (HashMap.Entry<Integer, LinearLayout> entry : mCommonLineNumToLineView.entrySet()) {
                if(entry.getValue() == v){
                    Integer index = entry.getKey();
                    if(mCommonLineToQuantityMapper.get(index) <= 1){ // Line should be removed from common view and added to my view
                        mCommonLineNumToLineView.get(index).setVisibility(GONE);
                        mCommonLineToQuantityMapper.put(index, 0);
                        mCommonLineNumberToQuantityView.get(index).setText("0");
                    }else{ //Line should be moved to my view
                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index) - 1);
                        mCommonLineNumberToQuantityView.get(index).setText(""+mCommonLineToQuantityMapper.get(index));
                    }

                    //Line in my view should be updated
                    if(mMyLineToQuantityMapper.get(index) > 0){
                        mMyLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index ) + 1);
                        mMyLineNumberToQuantityView.get(index).setText(""+mMyLineToQuantityMapper.get(index));
                    }else{ //Line in My view shlould be added
                        mMyLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mMyLineNumberToQuantityView.get(index).setText("1");
                        mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index) + 1);
                    }

                    mUsersDatabaseReference.child(Integer.toString(index)).setValue(mCommonLineToQuantityMapper.get(index));

                    mMyTotalSum += mLineNumToPriceMapper.get(index);
                    mBillSummarizerTotalSum.setText(Double.toString(mMyTotalSum *(1+mTip)));

                    return;
                }
            }
        }

    }
}
