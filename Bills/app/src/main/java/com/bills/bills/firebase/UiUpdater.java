package com.bills.bills.firebase;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.bills.bills.R;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Utilities.Utilities;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.View.GONE;

/**
 * Created by michaelvalershtein on 23/02/2018.
 */

public class UiUpdater implements View.OnClickListener, NumberPicker.OnValueChangeListener {
    private static String Tag = UiUpdater.class.getName();
    private final String ImageWidth = "width";
    private final String ImageHeight = "height";
    private final String Price = "Price";
    private final UUID mSessionId;
    private int mDefaultScreenWidth = 500;

    private DatabaseReference mUsersDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;

    //Firebase Storage members
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mBillsPerUserStorageReference;

    private LinearLayout mMyItemsArea;
    private LinearLayout mCommonItemsArea;

    private HashMap<Integer, Double> mLineNumToPriceMapper = new HashMap<>();
    private HashMap<Integer, BillRow> mBillRows;

    private HashMap<Integer, Double> mMyLineNumToPriceMapper = new HashMap<>();

    private ConcurrentHashMap<Integer, Integer> mCommonLineToQuantityMapper = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, LinearLayout> mCommonLineNumToLineView = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, TextView> mCommonLineNumberToQuantityView = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Double> mCommonLineNumberToPriceMapper = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Integer, Integer> mMyLineToQuantityMapper = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, LinearLayout> mMyLineNumToLineView = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, TextView> mMyLineNumberToQuantityView = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Double> mMyLineNumberToPriceMapper = new ConcurrentHashMap<>();


    private Double mMyTotalSum = 0.0;
    private Double mCommonTotalSum = 0.0;

    private int mScreenWidth = Integer.MIN_VALUE;
    private Context mContext;
    private Activity mActivity;

    private TextView mCommonTotalSumView;
    private TextView mMyTotalSumView;
    private EditText mMyPercentTipView;
    private EditText mMySumTipView;
    private double mTipPercent = 0.1;
    private double mTipSum = 0;

    private TextView mCommonItemsCountTV = null;
    private AtomicInteger mCommonItemsCount = new AtomicInteger(0);

    private TextView mMyItemsCountTV = null;
    private AtomicInteger mMyItemsCount = new AtomicInteger(0);

    private ImageView mScreenSplitter;

    ScrollView mCommonItemsContainer;
    ScrollView mMyItemsContainer;

    TextView mDotsTextView;
    private final Object mItemsUpdateLock = new Object();

    public UiUpdater(final UUID sessionId, final Context context, final Activity activity) {
        mSessionId = sessionId;
        mContext = context;
        mActivity = activity;
        final RelativeLayout summarizerRootLayout = activity.findViewById(R.id.bill_summarizer_frame_layout);

        final ViewTreeObserver viewTreeObserver = summarizerRootLayout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            summarizerRootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            mScreenWidth = summarizerRootLayout.getWidth();

                            synchronized (viewTreeObserver) {
                                viewTreeObserver.notifyAll();
                            }
                        }
                    });
                }
            });
        }
        Thread screenWidthUpdater = new Thread(new Runnable() {
            @Override
            public void run() {

                if(mScreenWidth < 0){
                    try {
                        for(int i = 0; i < 10; i++) {
                            viewTreeObserver.wait(200);
                        }
                    }catch (Exception ex){}
                    if(mScreenWidth < 0){
                        BillsLog.Log(sessionId,
                                LogLevel.Error,
                                "Failed to get screen size, setting to default: " + mDefaultScreenWidth,
                                LogsDestination.BothUsers, Tag);
                    }
                }
            }
        });
        screenWidthUpdater.start();
    }


    public void StartMainUser(String dbPath,
                              LinearLayout commonItemsArea,
                              LinearLayout myItemsArea,
                              ScrollView commonItemsContainer,
                              ScrollView myItemsContainer,
                              List<BillRow> billRows,
                              TextView myTotalSumView,
                              TextView commonTotalSumView,
                              EditText tipPercentView,
                              EditText mTipSumView,
                              TextView commonItemsCount,
                              TextView myItemsCount,
                              ImageView screenSpliter,
                              int screenWidth){
        mCommonItemsArea = commonItemsArea;
        mMyItemsArea = myItemsArea;
        mCommonTotalSumView = commonTotalSumView;
        mMyTotalSumView = myTotalSumView;

        mMyPercentTipView = tipPercentView;
        mMySumTipView = mTipSumView;

        mCommonItemsCountTV = commonItemsCount;
        mMyItemsCountTV = myItemsCount;
        mMyItemsArea = myItemsArea;

        mCommonItemsContainer = commonItemsContainer;
        mMyItemsContainer = myItemsContainer;

        mScreenWidth = screenWidth;

        InitScreenSplitter(screenSpliter);
        InitTipFields();

        for (BillRow row : billRows) {
            AddRowToUi(row);
        }

        mCommonItemsCountTV.setText("[" + Integer.toString(mCommonItemsCount.get()) + "]");
        mCommonTotalSumView.setText(format(mCommonTotalSum));

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child(dbPath);
        mUsersDatabaseReference.keepSynced(true);

        mUsersDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int commonQuanity = 0;
                double commonTotalSum = 0;
                for(DataSnapshot childData : dataSnapshot.getChildren()){
                    Double linePrice = mCommonLineNumberToPriceMapper.get(Integer.parseInt(childData.getKey()));
                    if(linePrice == null){
                        continue;
                    }
                    commonQuanity += childData.getValue(Integer.class);
                    commonTotalSum += childData.getValue(Integer.class) * linePrice;
                }

                mCommonTotalSum = commonTotalSum;
                mCommonItemsCount.set(commonQuanity);

                mCommonTotalSumView.setText(format(mCommonTotalSum));
                mCommonItemsCount.set(commonQuanity);
                mCommonItemsCountTV.setText("[" + Integer.toString(mCommonItemsCount.get()) + "]");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mUsersDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                synchronized (mItemsUpdateLock) {
                    Integer index = Integer.parseInt(dataSnapshot.getKey());
                    Integer newQuantity = dataSnapshot.getValue(Integer.class);

                    Integer curQuantity = mCommonLineToQuantityMapper.get(index);
                    int quanttityDiff = newQuantity - curQuantity;
                    mCommonTotalSum += mCommonLineNumberToPriceMapper.get(index) * quanttityDiff;

                    mCommonTotalSumView.setText(format(mCommonTotalSum));

                    if (newQuantity <= 0) {
                        //nothing to update at common items view
                        if (!mCommonLineNumToLineView.containsKey(index)) {
                            return;
                        }

                        mCommonLineNumberToQuantityView.get(index).setText("0");
                        mCommonLineNumToLineView.get(index).setVisibility(GONE);
                        mCommonLineToQuantityMapper.put(index, 0);
                        return;
                    } else {
                        mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mCommonLineNumberToQuantityView.get(index).setText("" + newQuantity);
                        mCommonLineToQuantityMapper.put(index, newQuantity);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
        BillsLog.Log(mSessionId, LogLevel.Info, "StartMainUser succeeded!", LogsDestination.BothUsers, Tag);
    }

    public void StartSecondaryUser(String dbPath,
                                   String storagePath,
                                   LinearLayout commonItemsArea,
                                   LinearLayout myItemsArea,
                                   ScrollView commonItemsContainer,
                                   ScrollView myItemsContainer,
                                   TextView myTotalSumView,
                                   TextView commonTotalSumView,
                                   EditText tipPercentView,
                                   EditText tipSumView,
                                   TextView commonItemsCount,
                                   TextView myItemsCount,
                                   ImageView screenSpliter){
        mCommonItemsArea = commonItemsArea;
        mMyItemsArea = myItemsArea;
        mCommonTotalSumView = commonTotalSumView;
        mMyTotalSumView = myTotalSumView;
        mMyPercentTipView = tipPercentView;
        mMySumTipView = tipSumView;
        mCommonItemsCountTV = commonItemsCount;
        mMyItemsCountTV = myItemsCount;

        mCommonItemsContainer = commonItemsContainer;
        mMyItemsContainer = myItemsContainer;

        InitScreenSplitter(screenSpliter);

        InitTipFields();

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child(dbPath);
        mUsersDatabaseReference.keepSynced(true);

        mFirebaseStorage = FirebaseStorage.getInstance();
        mBillsPerUserStorageReference = mFirebaseStorage.getReference().child(storagePath);

        mUsersDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int commonQuanity = 0;
                double commonTotalSum = 0;
                for(DataSnapshot childData : dataSnapshot.getChildren()){
                    Double linePrice = mCommonLineNumberToPriceMapper.get(Integer.parseInt(childData.getKey()));
                    if(linePrice == null){
                        continue;
                    }
                    commonQuanity += childData.getValue(Integer.class);
                    commonTotalSum += childData.getValue(Integer.class) * linePrice;
                }
                mCommonTotalSum = commonTotalSum;
                mCommonItemsCount.set(commonQuanity);

                mCommonTotalSumView.setText(format(mCommonTotalSum));
                mCommonItemsCount.set(commonQuanity);
                mCommonItemsCountTV.setText("[" + Integer.toString(mCommonItemsCount.get()) + "]");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mUsersDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                final Integer rowIndex = Integer.parseInt(dataSnapshot.getKey());
                final StorageReference curLineStorageReference = mBillsPerUserStorageReference.child(Integer.toString(rowIndex));
                final String rowCurentQuantity = dataSnapshot.getValue().toString();
                final Integer rowCurQuantityInt = Integer.parseInt(dataSnapshot.getValue().toString());
                if(rowCurQuantityInt > 0) {
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
                                rowQuantity = rowCurentQuantity;
                                itemHeight = Integer.parseInt(storageMetadata.getCustomMetadata(ImageHeight));
                                itemWidth = Integer.parseInt(storageMetadata.getCustomMetadata(ImageWidth));
                            } catch (Exception e) {
                                BillsLog.Log(mSessionId, LogLevel.Error, "StackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage(), LogsDestination.BothUsers, Tag);
                                return;
                            }
                            curLineStorageReference.getBytes(3 * ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {

                                    Typeface fontRegular = Typeface.createFromAsset(mContext.getAssets(),"fonts/highland_gothic_flf.ttf");
                                    Typeface fontLight = Typeface.createFromAsset(mContext.getAssets(),"fonts/highland_gothic_light_flf.ttf");

                                    LinearLayout commonItemRow = new LinearLayout(mContext);
                                    commonItemRow.setOrientation(LinearLayout.HORIZONTAL);
                                    commonItemRow.setGravity(Gravity.RIGHT);

                                    LinearLayout myItemRow = new LinearLayout(mContext);
                                    myItemRow.setOrientation(LinearLayout.HORIZONTAL);
                                    myItemRow.setGravity(Gravity.RIGHT);

                                    mCommonTotalSum += Double.parseDouble(rowPrice) * rowCurQuantityInt;
                                    mCommonLineNumberToPriceMapper.put(rowIndex, Double.parseDouble(rowPrice));
                                    mCommonTotalSumView.setText(format(mCommonTotalSum));

                                    TextView commonPrice = new TextView(mContext);
                                    commonPrice.setText("" + rowPrice);
                                    commonPrice.setTextSize(20);
                                    commonPrice.setBackgroundColor(mContext.getResources().getColor(R.color.summarizer_light));
                                    commonPrice.setTextColor(mContext.getResources().getColor(R.color.summarizer_dark));
                                    commonPrice.setTypeface(fontRegular);
                                    commonPrice.setGravity(Gravity.RIGHT);
                                    commonItemRow.addView(commonPrice);

                                    TextView myPrice = new TextView(mContext);
                                    myPrice.setText("" + rowPrice);
                                    myPrice.setTextSize(20);
                                    myPrice.setBackgroundColor(mContext.getResources().getColor(R.color.summarizer_light));
                                    myPrice.setTextColor(mContext.getResources().getColor(R.color.summarizer_dark));
                                    myPrice.setTypeface(fontRegular);
                                    myPrice.setGravity(Gravity.RIGHT);
                                    myItemRow.addView(myPrice);

                                    Space space = new Space(mContext);
                                    space.setMinimumWidth(180);
                                    commonItemRow.addView(space);

                                    mCommonItemsCount.addAndGet(Integer.parseInt(rowQuantity));
                                    mCommonItemsCountTV.setText("[" + Integer.toString(mCommonItemsCount.get()) + "]");

                                    TextView commonQuantityView = new TextView(mContext);
                                    commonQuantityView.setText("" + rowQuantity);
                                    commonQuantityView.setTextSize(20);
                                    commonQuantityView.setTextColor(mContext.getResources().getColor(R.color.summarizer_light));
                                    commonQuantityView.setTypeface(fontLight);
                                    commonItemRow.addView(commonQuantityView);

                                    space = new Space(mContext);
                                    space.setMinimumWidth(180);
                                    myItemRow.addView(space);

                                    TextView myQuantityView = new TextView(mContext);
                                    myQuantityView.setText("0");
                                    myQuantityView.setTextSize(20);
                                    myQuantityView.setTextColor(mContext.getResources().getColor(R.color.summarizer_light));
                                    myQuantityView.setTypeface(fontLight);
                                    myItemRow.addView(myQuantityView);

                                    space = new Space(mContext);
                                    space.setMinimumWidth(130);
                                    commonItemRow.addView(space);

                                    space = new Space(mContext);
                                    space.setMinimumWidth(130);
                                    myItemRow.addView(space);

                                    if(mScreenWidth < 0){
                                        mScreenWidth = mDefaultScreenWidth;
                                    }
                                    Bitmap tmpBitmap = Utilities.ConvertFirebaseBytesToBitmap(bytes, itemWidth, itemHeight);
                                    Bitmap scaledCommonItemBitmap = Bitmap.createScaledBitmap(tmpBitmap, mScreenWidth / 2, 60, false);
                                    tmpBitmap.recycle();
                                    Bitmap finalCommonItem = ChangeBackgroundColor(scaledCommonItemBitmap, new Scalar(255, 93, 113));
                                    scaledCommonItemBitmap.recycle();

                                    tmpBitmap = Utilities.ConvertFirebaseBytesToBitmap(bytes, itemWidth, itemHeight);
                                    Bitmap scaledMyItemBitmap = Bitmap.createScaledBitmap(tmpBitmap, mScreenWidth / 2, 60, false);
                                    tmpBitmap.recycle();
                                    Bitmap finalMyItem = ChangeBackgroundColor(scaledMyItemBitmap, new Scalar(255, 93, 113));
                                    scaledMyItemBitmap.recycle();
                                    ImageView commonImageView = new ImageView(mContext);

                                    commonImageView.setImageBitmap(finalCommonItem);
                                    commonItemRow.addView(commonImageView);

                                    ImageView myImageView = new ImageView(mContext);
                                    myImageView.setImageBitmap(finalMyItem);
                                    myItemRow.addView(myImageView);

                                    int rowIndexInUi = GetRowUiIndex(rowIndex);
                                    mCommonItemsArea.addView(commonItemRow, rowIndexInUi);

                                    mMyItemsArea.addView(myItemRow, rowIndexInUi);
                                    myItemRow.setVisibility(GONE);

                                    Integer rowQuantityPrsed = Integer.parseInt(rowQuantity);
                                    Double rowPriceParsed = Double.parseDouble(rowPrice);
                                    mMyLineNumToPriceMapper.put(rowIndex, rowPriceParsed);

                                    commonItemRow.setOnClickListener(UiUpdater.this);

                                    mCommonLineToQuantityMapper.put(rowIndex, rowQuantityPrsed);
                                    mCommonLineNumToLineView.put(rowIndex, commonItemRow);
                                    mCommonLineNumberToQuantityView.put(rowIndex, commonQuantityView);

                                    myItemRow.setOnClickListener(UiUpdater.this);

                                    mMyLineToQuantityMapper.put(rowIndex, 0);
                                    mMyLineNumToLineView.put(rowIndex, myItemRow);
                                    mMyLineNumberToQuantityView.put(rowIndex, myQuantityView);
                                }
                            });
                            BillsLog.Log(mSessionId, LogLevel.Info, "onChildAdded of row " + rowIndex + " succeeded!", LogsDestination.SecondaryUser, Tag);
                        }
                    });
                }else{
                    BillsLog.Log(mSessionId, LogLevel.Info, "onChildAdded of row " + rowIndex + " failed due to quantity "+ rowCurQuantityInt +"!", LogsDestination.SecondaryUser, Tag);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                synchronized (mItemsUpdateLock) {
                    Integer index = Integer.parseInt(dataSnapshot.getKey());
                    Integer newQuantity = dataSnapshot.getValue(Integer.class);

                    Integer curQuantity = mCommonLineToQuantityMapper.get(index);

                    if (newQuantity <= 0) {
                        //nothing to update at common items view
                        if (!mCommonLineNumToLineView.containsKey(index)) {
                            return;
                        }

                        mCommonLineNumberToQuantityView.get(index).setText("0");
                        mCommonLineNumToLineView.get(index).setVisibility(GONE);
                        mCommonLineToQuantityMapper.put(index, 0);
                        return;
                    } else {

                        mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mCommonLineNumberToQuantityView.get(index).setText("" + newQuantity);
                        mCommonLineToQuantityMapper.put(index, newQuantity);
                    }
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
        BillsLog.Log(mSessionId, LogLevel.Info, "StartSecondaryUser succeeded!", LogsDestination.BothUsers, Tag);
    }

    private void InitScreenSplitter(ImageView screenSpliter) {
        mScreenSplitter = screenSpliter;
        screenSpliter.setOnTouchListener(new View.OnTouchListener() {
            float lastY = Float.MIN_VALUE;
            float commonAreaInitialHeight = mCommonItemsContainer.getLayoutParams().height;
            float myAreaInitialHeight = mMyItemsContainer.getLayoutParams().height;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        lastY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        lastY = Float.MIN_VALUE;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (lastY != Float.MIN_VALUE) {
                            float dist = event.getRawY() - lastY;

                            ViewGroup.LayoutParams commonItemsContainerLp = mCommonItemsContainer.getLayoutParams();
                            ViewGroup.LayoutParams myItemsContainerLp = mMyItemsContainer.getLayoutParams();

                            if(commonItemsContainerLp.height + dist > 0 && myItemsContainerLp.height - dist > 0) {
                                commonItemsContainerLp.height += dist;
                                myItemsContainerLp.height -= dist;
                            }

                            if(commonItemsContainerLp.height + myItemsContainerLp.height  < commonAreaInitialHeight + myAreaInitialHeight){
                                float delta = commonAreaInitialHeight + myAreaInitialHeight - (commonItemsContainerLp.height + myItemsContainerLp.height);
                                commonItemsContainerLp.height += delta / 2;
                                myItemsContainerLp.height += delta / 2;
                            }

                            mCommonItemsContainer.setLayoutParams(commonItemsContainerLp);
                            mMyItemsContainer.setLayoutParams(myItemsContainerLp);

                        }
                        lastY = event.getRawY();
                        break;
                    default:
                        break;
                }

                return true;
            }
        });
    }

    private void InitTipFields() {
        mTipPercent = 0;

        mMyPercentTipView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Show(UiUpdater.TipFieldTipe.tipPercent, "Tip Percent", 0, 100);
            }
        });

        mMySumTipView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Show(UiUpdater.TipFieldTipe.tipSum, "Tip Sum", 0, 1000);
            }
        });

        mDotsTextView = new TextView(mContext);
    }

    private int GetRowUiIndex(Integer newRowIndex) {
        if(mCommonLineNumToLineView.size() == 0){
            return 0 ;
        }

        Integer[] rowIndeces = new Integer[mCommonLineNumToLineView.size()];
        mCommonLineNumToLineView.keySet().toArray(rowIndeces);
        Arrays.sort(rowIndeces);
        if(newRowIndex < rowIndeces[0]){
            return 0;
        }
        for(int retVal = 1; retVal < rowIndeces.length; retVal++){
            if(newRowIndex < rowIndeces[retVal] && newRowIndex > rowIndeces[retVal-1]){
                return retVal;
            }
        }

        return rowIndeces.length;
    }

    private void AddRowToUi(BillRow row) {
        Typeface fontRegular = Typeface.createFromAsset(mContext.getAssets(),"fonts/highland_gothic_flf.ttf");
        Typeface fontLight = Typeface.createFromAsset(mContext.getAssets(),"fonts/highland_gothic_light_flf.ttf");

        LinearLayout commonItemRow = new LinearLayout(mContext);
        commonItemRow.setOrientation(LinearLayout.HORIZONTAL);
        commonItemRow.setGravity(Gravity.RIGHT);

        LinearLayout myItemRow = new LinearLayout(mContext);
        myItemRow.setOrientation(LinearLayout.HORIZONTAL);
        myItemRow.setGravity(Gravity.RIGHT);

        mCommonTotalSum += (row.GetPrice() * row.GetQuantity());
        mCommonLineNumberToPriceMapper.put(row.GetRowIndex(), row.GetPrice());

        TextView commonPrice = new TextView(mContext);
        commonPrice.setText(format(row.GetPrice()));
        commonPrice.setBackgroundColor(mContext.getResources().getColor(R.color.summarizer_light));
        commonPrice.setTextColor(mContext.getResources().getColor(R.color.summarizer_dark));
        commonPrice.setTypeface(fontRegular);
        commonPrice.setGravity(Gravity.RIGHT);
        commonItemRow.addView(commonPrice);

        TextView myPrice = new TextView(mContext);
        myPrice.setText(format(row.GetPrice()));
        myPrice.setBackgroundColor(mContext.getResources().getColor(R.color.summarizer_light));
        myPrice.setTextColor(mContext.getResources().getColor(R.color.summarizer_dark));
        myPrice.setTypeface(fontRegular);
        myPrice.setGravity(Gravity.RIGHT);
        myItemRow.addView(myPrice);

        Space space = new Space(mContext);
        space.setMinimumWidth(180);
        commonItemRow.addView(space);

        mMyLineNumToPriceMapper.put(row.GetRowIndex(), row.GetPrice());

        mCommonItemsCount.addAndGet(row.GetQuantity());

        TextView commonQuantityView = new TextView(mContext);
        commonQuantityView.setText(Integer.toString(row.GetQuantity()));
        commonQuantityView.setTextColor(mContext.getResources().getColor(R.color.summarizer_light));
        commonQuantityView.setTypeface(fontLight);
        commonItemRow.addView(commonQuantityView);

        space = new Space(mContext);
        space.setMinimumWidth(180);
        myItemRow.addView(space);

        TextView myQuantityView = new TextView(mContext);
        myQuantityView.setText("0");
        myQuantityView .setTextColor(mContext.getResources().getColor(R.color.summarizer_light));
        myQuantityView.setTypeface(fontLight);
        myItemRow.addView(myQuantityView);

        space = new Space(mContext);
        space.setMinimumWidth(130);
        commonItemRow.addView(space);

        space = new Space(mContext);
        space.setMinimumWidth(130);
        myItemRow.addView(space);

        if(mScreenWidth < 0){
            mScreenWidth = mDefaultScreenWidth;
        }
        ImageView commonImageView = new ImageView(mContext);
        Bitmap scaledCommonItemBitmap = Bitmap.createScaledBitmap(row.GetItem(), mScreenWidth / 2, 60, false);
        Bitmap finalCommonItemBitmap = ChangeBackgroundColor(scaledCommonItemBitmap, new Scalar(255, 93, 113));
        scaledCommonItemBitmap.recycle();
        commonImageView.setImageBitmap(finalCommonItemBitmap);
        commonItemRow.addView(commonImageView);

        ImageView myImageView = new ImageView(mContext);
        Bitmap scaledMyItemBitmap = Bitmap.createScaledBitmap(row.GetItem(), mScreenWidth / 2, 60, false);
        Bitmap finalMyItemBitmap = ChangeBackgroundColor(scaledMyItemBitmap, new Scalar(255, 93, 113));
        scaledMyItemBitmap.recycle();
        myImageView.setImageBitmap(finalMyItemBitmap);
        myItemRow.addView(myImageView);

        mCommonItemsArea.addView(commonItemRow);

        myItemRow.setVisibility(GONE);
        mMyItemsArea.addView(myItemRow);

        commonItemRow.setOnClickListener(this);

        mCommonLineToQuantityMapper.put(row.GetRowIndex(), row.GetQuantity());
        mCommonLineNumToLineView.put(row.GetRowIndex(), commonItemRow);
        mCommonLineNumberToQuantityView.put(row.GetRowIndex(), commonQuantityView);

        myItemRow.setOnClickListener(this);

        mMyLineToQuantityMapper.put(row.GetRowIndex(), 0);
        mMyLineNumToLineView.put(row.GetRowIndex(), myItemRow);
        mMyLineNumberToQuantityView.put(row.GetRowIndex(), myQuantityView);
    }

    /**
     * Expected behavior:
     *  - MyItem clicked:
     *      - UI updated instantly
     *      - DB updated in background
     *  - CommonIten clicked:
     *      - clicked item grayed out
     *      - flickering three dots appear next to clicked item
     *      - after DB updated, UI updated as follows:
     *          - if transaction succeed the item is added to MyItems and removed from CommonItems
     *          - if transaction fails the item removed from CommonItems and message to user pops out
     */
    @Override
    public void onClick(final View v) {
        synchronized (mItemsUpdateLock) {
            //move item from my Bill to common Bill
            if (((LinearLayout) v.getParent()).getId() == R.id.my_items_area_linearlayout) {
                //find relevant entry
                for (final HashMap.Entry<Integer, LinearLayout> entry : mMyLineNumToLineView.entrySet()) {
                    if (entry.getValue() == v) {
                        final Integer index = entry.getKey();
                        mMyItemsCountTV.setText("[" + Integer.toString(mMyItemsCount.decrementAndGet()) + "]");

                        if (mMyLineToQuantityMapper.get(index) == 1) { // Line should be removed from my view and added to common view
                            mMyLineNumToLineView.get(index).setVisibility(GONE);
                            mMyLineToQuantityMapper.put(index, 0);
                            mMyLineNumberToQuantityView.get(index).setText("0");
                            BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " removed from My view and added to Common view", LogsDestination.BothUsers, Tag);
                        } else if (mMyLineToQuantityMapper.get(index) > 1) { //Line should be moved to common view
                            mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index) - 1);
                            mMyLineNumberToQuantityView.get(index).setText("" + mMyLineToQuantityMapper.get(index));
                            BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " removed from My view and added to Common view", LogsDestination.BothUsers, Tag);
                            mMyLineNumberToQuantityView.get(index).setText("" + mMyLineToQuantityMapper.get(index));
                            BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " moved from My to Common view (in case of quantity > 1)", LogsDestination.BothUsers, Tag);
                        }

                        mMyTotalSum -= mMyLineNumToPriceMapper.get(index);
                        mMyTotalSumView.setText(format(mMyTotalSum * (1 + mTipPercent / 100)));
                        mTipSum = mMyTotalSum * mTipPercent / 100;
                        mMySumTipView.setText(format(mTipSum));

                        mUsersDatabaseReference.runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                Integer curValue = mutableData.child(Integer.toString(index)).getValue(Integer.class);
                                mutableData.child(Integer.toString(index)).setValue(curValue + 1);
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean commited, DataSnapshot dataSnapshot) {
//                                if(databaseError == null && commited) {
//
//                                    mCommonItemsCountTV.setText("[" + Integer.toString(mCommonItemsCount.incrementAndGet()) + "]");
//
//                                    mCommonTotalSum += mCommonLineNumberToPriceMapper.get(index);
//                                    mCommonTotalSumView.setText(format(mCommonTotalSum));
//
//                                    //Line in common view should be updated
//                                    if (mCommonLineToQuantityMapper.get(index) > 0) {
//                                        mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
//                                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index) + 1);
//                                        mCommonLineNumberToQuantityView.get(index).setText("" + mCommonLineToQuantityMapper.get(index));
//                                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + ", in Common view, updated", LogsDestination.BothUsers, Tag);
//                                    } else { //Line in common view shlould be added
//                                        mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
//                                        mCommonLineNumberToQuantityView.get(index).setText("1");
//                                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index) + 1);
//                                        BillsLog.Log(mSessionId, LogLevel.Info, "Added line " + index + " to Common view", LogsDestination.BothUsers, Tag);
//                                    }
//
//                                    return;
//                                }
                            }
                        });
                    }
                }
            }
        }
        //move item from common Bill to my Bill and substract 1 from item's quantity
        if(((LinearLayout) v.getParent()).getId() == R.id.common_items_area_linearlayout){
            for (final HashMap.Entry<Integer, LinearLayout> entry : mCommonLineNumToLineView.entrySet()) {
                if(entry.getValue() == v){
                    final Integer index = entry.getKey();
                    v.setAlpha((float)0.5);
                    mUsersDatabaseReference.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            Integer curValue = mutableData.child(Integer.toString(index)).getValue(Integer.class);
                            if(curValue > 0) {
                                mutableData.child(Integer.toString(index)).setValue(curValue - 1);
                                return Transaction.success(mutableData);
                            }else{
                                return Transaction.abort();
                            }
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean commited, DataSnapshot dataSnapshot) {
                            synchronized (mItemsUpdateLock) {
                                if (databaseError == null && commited) {
                                    mMyItemsCountTV.setText("[" + Integer.toString(mMyItemsCount.incrementAndGet()) + "]");
//                                    mCommonItemsCountTV.setText("[" + Integer.toString(mCommonItemsCount.decrementAndGet()) + "]");
//
//                                    mCommonTotalSum -= mCommonLineNumberToPriceMapper.get(index);
//                                    mCommonTotalSumView.setText(format(mCommonTotalSum));
//
//                                    if (mCommonLineToQuantityMapper.get(index) <= 1) { // Line should be removed from common view and added to my view
//                                        mCommonLineNumToLineView.get(index).setVisibility(GONE);
//                                        mCommonLineToQuantityMapper.put(index, 0);
//                                        mCommonLineNumberToQuantityView.get(index).setText("0");
//                                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " removed from Common view and added to My view", LogsDestination.BothUsers, Tag);
//                                    } else { //Line should be moved to my view
//                                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index) - 1);
//                                        mCommonLineNumberToQuantityView.get(index).setText("" + mCommonLineToQuantityMapper.get(index));
//                                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " moved from Common to My view (in case of quantity > 1)", LogsDestination.BothUsers, Tag);
//                                    }

                                    //Line in my view should be updated
                                    if (mMyLineToQuantityMapper.get(index) > 0) {
                                        mMyLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                                        mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index) + 1);
                                        mMyLineNumberToQuantityView.get(index).setText("" + mMyLineToQuantityMapper.get(index));
                                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + ", in My view, updated", LogsDestination.BothUsers, Tag);
                                    } else { //Line in My view shlould be added
                                        mMyLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                                        mMyLineNumberToQuantityView.get(index).setText("1");
                                        mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index) + 1);
                                        BillsLog.Log(mSessionId, LogLevel.Info, "Added line " + index + " to My view", LogsDestination.BothUsers, Tag);
                                    }

                                    mMyTotalSum += mMyLineNumToPriceMapper.get(index);

                                    mMyTotalSumView.setText(format(mMyTotalSum * (1 + mTipPercent / 100)));
                                    mTipSum = mMyTotalSum * mTipPercent / 100;
                                    mMySumTipView.setText(format(mTipSum));
                                } else {
                                    Toast.makeText(mContext, "מישהו הקדים אותך :)", Toast.LENGTH_SHORT);
                                }
                                v.setAlpha((float) 1);
                            }
                        }
                    });
                    return;
                }
            }
        }
    }

    private Bitmap ChangeBackgroundColor(Bitmap src, Scalar color) {
        if(!OpenCVLoader.initDebug()){
            throw new RuntimeException("failed to init opencv");
        }
        Mat srcMat = new Mat();
        Mat grayscaleMat = new Mat();
        Mat mask = new Mat();

        try {
            Utils.bitmapToMat(src, srcMat);

            Imgproc.cvtColor(srcMat, grayscaleMat, Imgproc.COLOR_RGBA2GRAY);

            Imgproc.threshold(grayscaleMat, mask, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

            srcMat.setTo(color, mask);

            Bitmap dst = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());

            Utils.matToBitmap(srcMat, dst);
            return dst;
        }catch (Exception ex){
            throw ex;
        }finally {
            srcMat.release();
            grayscaleMat.release();
            mask.release();
        }
    }

    public static String format(double d) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(d);
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
    }

    public void Show(final UiUpdater.TipFieldTipe tipType, String title, int min, int max) {
        final Dialog dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        dialog.setTitle(title);
        dialog.setContentView(R.layout.dialog);
        Button set = (Button) dialog.findViewById(R.id.set);
        Button cancel = (Button) dialog.findViewById(R.id.cancel);
        final NumberPicker numberPicker = (NumberPicker) dialog.findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(max); // max value 100
        numberPicker.setMinValue(min);   // min value 0
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setOnValueChangedListener(this);
        set.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                switch (tipType){
                    case tipPercent:
                        mTipPercent = numberPicker.getValue();
                        mTipSum = mTipPercent * mMyTotalSum / 100;
                        break;
                    case tipSum:
                        mTipSum = numberPicker.getValue();
                        mTipPercent = mMyTotalSum == 0 ? mTipPercent : mTipSum / mMyTotalSum * 100;
                        break;
                    default:
                        break;
                }
                mMyPercentTipView.setText(format(mTipPercent));
                mMySumTipView.setText(format(mTipSum));
                mMyTotalSumView.setText(format(mMyTotalSum + mTipSum));
                dialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // dismiss the dialog
            }
        });
        dialog.show();
    }

    private enum TipFieldTipe{
        tipSum,
        tipPercent
    }

    private void StartDots(){
        mCommonItemsArea.addView(mDotsTextView);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    //shows:  .->..->...
                    for (int j = 0; j < 3; j++) {

                        String s = "";
                        for(int k = 0; k < j; k++){
                            s += ". ";
                        }
                        for(int k = j; k < 3; k++){
                            s += " ";
                        }
                        final String fs = s;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mDotsTextView.setText(fs);
                                    synchronized (mDotsTextView) {
                                        mDotsTextView.notifyAll();
                                    }
                                }catch(Exception ex){
                                    ex.printStackTrace();
                                }
                            }
                        });
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Log.d("", "");
                        }
                        try{
                            mDotsTextView.wait(300);
                        }catch(Exception ex){
                            ex.printStackTrace();
                        }

                    }
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mDotsTextView.setText("");
                                synchronized (mDotsTextView) {
                                    mDotsTextView.notifyAll();
                                }
                            }catch(Exception ex){
                                ex.printStackTrace();

                            }
                        }
                    });
                    try{
                        mDotsTextView.wait(1000);
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }

                    for(int j = 0; j < 3;j++){
                        String s = "";
                        for(int k = 0; k < j; k++){
                            s += " ";
                        }
                        for(int k = j; k < 3; k++){
                            s += ". ";
                        }
                        final String fs = s;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mDotsTextView.setText(fs);
                                    synchronized (mDotsTextView) {
                                        mDotsTextView.notifyAll();
                                    }
                                }catch(Exception ex){
                                    ex.printStackTrace();
                                }
                            }
                        });
                        try{
                            mDotsTextView.wait(1000);
                        }catch(Exception ex){
                            ex.printStackTrace();
                        }
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            Log.d("", "");
                        }
                    }
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mDotsTextView.setText("");
                                synchronized (mDotsTextView) {
                                    mDotsTextView.notifyAll();
                                }
                            }catch(Exception ex){
                                ex.printStackTrace();

                            }
                        }
                    });
                    try{
                        mDotsTextView.wait(1000);
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        });
        t.start();

    }
}
