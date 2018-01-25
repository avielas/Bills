package com.bills.bills.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import org.opencv.core.Scalar;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.View.GONE;

/**
 * Created by michaelvalershtein on 01/08/2017.
 */

public class UiUpdater implements View.OnClickListener {
    private static String Tag = UiUpdater.class.getName();
    private final String ImageWidth = "width";
    private final String ImageHeight = "height";
    private final String Price = "Price";
    private final UUID mSessionId;

    private Context mContext;

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

    public UiUpdater(final UUID sessionId) {
        mSessionId = sessionId;
    }

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

    public void StartMainUser(Context context,
                              String dbPath,
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
                              ImageView screenSpliter){
        mContext = context;
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

        mUsersDatabaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Integer index = Integer.parseInt(dataSnapshot.getKey());
                Integer newQuantity = dataSnapshot.getValue(Integer.class);

                if(newQuantity < mCommonLineToQuantityMapper.get(index)) {
                    mCommonTotalSum -= mCommonLineNumberToPriceMapper.get(index);
                }else{
                    mCommonTotalSum += mCommonLineNumberToPriceMapper.get(index);
                }
                mCommonTotalSumView.setText(format(mCommonTotalSum));

                if(newQuantity <= 0){
                    //nothing to update at common items view
                    if(!mCommonLineNumToLineView.containsKey(index)){
                        return;
                    }

                    mCommonLineNumberToQuantityView.get(index).setText("0");
                    mCommonLineNumToLineView.get(index).setVisibility(GONE);
                    return;
                }else{
                    mCommonLineNumberToQuantityView.get(index).setText(""+newQuantity);
                    mCommonLineToQuantityMapper.put(index, newQuantity);
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

    public void StartSecondaryUser(final Context context,
                                   String dbPath,
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
        mContext = context;
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

                                    Bitmap tmpBitmap = Utilities.ConvertFirebaseBytesToBitmap(bytes, itemWidth, itemHeight);
                                    Bitmap commonItemBitmap = Bitmap.createScaledBitmap(tmpBitmap, 500, 60, false);
                                    tmpBitmap.recycle();

                                    tmpBitmap = Utilities.ConvertFirebaseBytesToBitmap(bytes, itemWidth, itemHeight);
                                    Bitmap myItemBitmap = Bitmap.createScaledBitmap(tmpBitmap, 500, 60, false);
                                    ImageView commonImageView = new ImageView(mContext);

                                    commonImageView.setImageBitmap(commonItemBitmap);
                                    commonItemRow.addView(commonImageView);

                                    ImageView myImageView = new ImageView(mContext);
                                    myImageView.setImageBitmap(myItemBitmap);
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
                Integer index = Integer.parseInt(dataSnapshot.getKey());
                Integer newQuantity = dataSnapshot.getValue(Integer.class);

                mCommonItemsCount.addAndGet(newQuantity - mCommonLineToQuantityMapper.get(index));
                if(newQuantity < mCommonLineToQuantityMapper.get(index)) {
                    mCommonTotalSum -= mCommonLineNumberToPriceMapper.get(index);
                    mCommonTotalSumView.setText(format(mCommonTotalSum));
                }else if(newQuantity > mCommonLineToQuantityMapper.get(index)){
                    mCommonTotalSum += mCommonLineNumberToPriceMapper.get(index);
                    mCommonTotalSumView.setText(format(mCommonTotalSum));
                }

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
        mMySumTipView.setText("0");
        mMyPercentTipView.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s.toString().equalsIgnoreCase("")) {
                    mTipPercent = 0;
                    mTipSum = 0;
                    mMySumTipView.setText("0");
                } else {

                    double newTip = Double.parseDouble(s.toString());
                    if (newTip != mTipPercent) {
                        String curTip = s.toString();
                        mTipPercent = (1.0 * newTip) / 100;
                        mTipSum = mMyTotalSum * mTipPercent;
                        mMySumTipView.setText(format(mTipSum));
                        mMyTotalSumView.setText(format(mMyTotalSum + mTipSum));
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

        mMySumTipView.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s.toString().equalsIgnoreCase("")) {
                    mTipSum = 0;
                    mTipPercent = 0;
                    mMyPercentTipView.setText("0");
                } else {
                    double newTip = Double.parseDouble(s.toString());
                    if (newTip != mTipSum) {
                        mTipPercent = (1.0 * newTip) / mMyTotalSum;
                        mMyPercentTipView.setText(format(mTipPercent));
                        mMyTotalSumView.setText(format(mMyTotalSum + newTip));

                    }
                }

            }
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });

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

        LinearLayout myItemRow = new LinearLayout(mContext);
        myItemRow.setOrientation(LinearLayout.HORIZONTAL);

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
        myPrice.setTypeface(fontRegular);
        myPrice.setGravity(Gravity.RIGHT);
        myPrice.setTextColor(mContext.getResources().getColor(R.color.summarizer_dark));

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

        ImageView commonImageView = new ImageView(mContext);
        commonImageView.setImageBitmap(row.GetItem());
        commonItemRow.addView(commonImageView);

        ImageView myImageView = new ImageView(mContext);
        myImageView.setImageBitmap(row.GetItem());
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

    @Override
    public void onClick(View v) {
        //move item from my Bill to common Bill
        if(((LinearLayout)v.getParent()).getId() == R.id.my_items_area_linearlayout) {
            mMyItemsCountTV.setText("[" + Integer.toString(mMyItemsCount.decrementAndGet()) + "]");
            mCommonItemsCountTV.setText("[" + Integer.toString(mCommonItemsCount.incrementAndGet()) + "]");
            //find relevant entry
            for (HashMap.Entry<Integer, LinearLayout> entry : mMyLineNumToLineView.entrySet()) {
                if(entry.getValue() == v){
                    Integer index = entry.getKey();
                    mCommonTotalSum += mCommonLineNumberToPriceMapper.get(index);
                    mCommonTotalSumView.setText(format(mCommonTotalSum));

                    if(mMyLineToQuantityMapper.get(index) == 1){ // Line should be removed from my view and added to common view
                        mMyLineNumToLineView.get(index).setVisibility(GONE);
                        mMyLineToQuantityMapper.put(index, 0);
                        mMyLineNumberToQuantityView.get(index).setText("0");
                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " removed from My view and added to Common view", LogsDestination.BothUsers, Tag);
                    }else if(mMyLineToQuantityMapper.get(index) > 1){ //Line should be moved to common view
                        mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index) - 1);
                        mMyLineNumberToQuantityView.get(index).setText("" + mMyLineToQuantityMapper.get(index));
                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " removed from My view and added to Common view", LogsDestination.BothUsers, Tag);
                        mMyLineNumberToQuantityView.get(index).setText(""+mMyLineToQuantityMapper.get(index));
                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " moved from My to Common view (in case of quantity > 1)", LogsDestination.BothUsers, Tag);
                    }

                    //Line in common view should be updated
                    if(mCommonLineToQuantityMapper.get(index) > 0){
                        mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index ) + 1);
                        mCommonLineNumberToQuantityView.get(index).setText(""+mCommonLineToQuantityMapper.get(index));
                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + ", in Common view, updated", LogsDestination.BothUsers, Tag);
                    }else{ //Line in common view shlould be added
                        mCommonLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mCommonLineNumberToQuantityView.get(index).setText("1");
                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index) + 1);
                        BillsLog.Log(mSessionId, LogLevel.Info, "Added line " + index + " to Common view", LogsDestination.BothUsers, Tag);
                    }

                    mUsersDatabaseReference.child(Integer.toString(index)).setValue(mCommonLineToQuantityMapper.get(index));

                    mMyTotalSum -= mMyLineNumToPriceMapper.get(index);
                    mMyTotalSumView.setText(format(mMyTotalSum *(1+ mTipPercent)));
                    mTipSum =  mMyTotalSum * mTipPercent;
                    mMySumTipView.setText(format(mTipSum));
                    return;
                }
            }
        }

        //move item from common Bill to my Bill and substract 1 from item's quantity
        if(((LinearLayout) v.getParent()).getId() == R.id.common_items_area_linearlayout){
            mMyItemsCountTV.setText("[" + Integer.toString(mMyItemsCount.incrementAndGet()) + "]");
            mCommonItemsCountTV.setText("[" + Integer.toString(mCommonItemsCount.decrementAndGet()) + "]");
            for (HashMap.Entry<Integer, LinearLayout> entry : mCommonLineNumToLineView.entrySet()) {
                if(entry.getValue() == v){
                    Integer index = entry.getKey();
                    mCommonTotalSum -= mCommonLineNumberToPriceMapper.get(index);
                    mCommonTotalSumView.setText(format(mCommonTotalSum));

                    if(mCommonLineToQuantityMapper.get(index) <= 1){ // Line should be removed from common view and added to my view
                        mCommonLineNumToLineView.get(index).setVisibility(GONE);
                        mCommonLineToQuantityMapper.put(index, 0);
                        mCommonLineNumberToQuantityView.get(index).setText("0");
                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " removed from Common view and added to My view", LogsDestination.BothUsers, Tag);
                    }else{ //Line should be moved to my view
                        mCommonLineToQuantityMapper.put(index, mCommonLineToQuantityMapper.get(index) - 1);
                        mCommonLineNumberToQuantityView.get(index).setText(""+mCommonLineToQuantityMapper.get(index));
                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + " moved from Common to My view (in case of quantity > 1)", LogsDestination.BothUsers, Tag);
                    }

                    //Line in my view should be updated
                    if(mMyLineToQuantityMapper.get(index) > 0){
                        mMyLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index ) + 1);
                        mMyLineNumberToQuantityView.get(index).setText(""+mMyLineToQuantityMapper.get(index));
                        BillsLog.Log(mSessionId, LogLevel.Info, "Line " + index + ", in My view, updated", LogsDestination.BothUsers, Tag);
                    }else{ //Line in My view shlould be added
                        mMyLineNumToLineView.get(index).setVisibility(View.VISIBLE);
                        mMyLineNumberToQuantityView.get(index).setText("1");
                        mMyLineToQuantityMapper.put(index, mMyLineToQuantityMapper.get(index) + 1);
                        BillsLog.Log(mSessionId, LogLevel.Info, "Added line " + index + " to My view", LogsDestination.BothUsers, Tag);
                    }

                    mUsersDatabaseReference.child(Integer.toString(index)).setValue(mCommonLineToQuantityMapper.get(index));

                    mMyTotalSum += mMyLineNumToPriceMapper.get(index);


                    mMyTotalSumView.setText(format(mMyTotalSum *(1+ mTipPercent)));
                    mTipSum =  mMyTotalSum * mTipPercent;
                    mMySumTipView.setText(format(mTipSum));
                    return;
                }
            }
        }
    }

    public static String format(double d) {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(d);
    }
}
