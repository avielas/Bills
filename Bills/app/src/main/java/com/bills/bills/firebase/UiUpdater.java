package com.bills.bills.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bills.bills.R;
import com.bills.billslib.Contracts.BillRow;
import com.bills.billslib.Utilities.FilesHandler;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static android.view.View.GONE;

/**
 * Created by michaelvalershtein on 01/08/2017.
 */

public class UiUpdater implements View.OnClickListener {

    private final String ImageType = "image/jpg";
    private final String ImageWidth = "width";
    private final String ImageHeight = "height";
    private final String Price = "Price";
    private final String Quantity = "Quantity";

    private final String RowsDbKey = "Rows";

    private Context mContext;

    private DatabaseReference mUsersDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;

    //Firebase Storage members
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mBillsPerUserStorageReference;

    private LinearLayout mMyItemsArea;
    private LinearLayout mCommonItemsArea;

    private HashMap<Integer, BillRow> mBillRows;

    private HashMap<Integer, Double> mLineNumToPriceMapper = new HashMap<>();

    private ConcurrentHashMap<Integer, Integer> mCommonLineToQuantityMapper = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, LinearLayout> mCommonLineNumToLineView = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, TextView> mCommonLineNumberToQuantityView = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Integer, Integer> mMyLineToQuantityMapper = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, LinearLayout> mMyLineNumToLineView = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, TextView> mMyLineNumberToQuantityView = new ConcurrentHashMap<>();

    private Double mMyTotalSum = 0.0;
    private TextView mBillSummarizerTotalSumView;
    private EditText mBillSummarizerTipView;
    private double mTip = 0;


    public void StartMainUser(Context context,
                              String dbPath,
                              LinearLayout commonItemsArea,
                              LinearLayout myItemsArea,
                              List<BillRow> billRows,
                              TextView billSummarizerTotalSumView,
                              EditText billSummarizerTipView){
        mContext = context;
        mCommonItemsArea = commonItemsArea;
        mMyItemsArea = myItemsArea;
        mBillSummarizerTotalSumView = billSummarizerTotalSumView;
        mBillSummarizerTipView = billSummarizerTipView;
        mBillSummarizerTipView.addTextChangedListener(new TextWatcher() {
            private String curTip = "10";
            public void afterTextChanged(Editable s) {
                if(s.toString().equalsIgnoreCase("")) {
                    mTip = 0;
                }else {
                    int newTip = Integer.parseInt(s.toString());
                    if (newTip < 0 || newTip > 100) {
                        mBillSummarizerTipView.setText(curTip);
                    } else {
                        curTip = s.toString();
                        mTip = (1.0*newTip)/100;
                        mBillSummarizerTotalSumView.setText(Double.toString(mMyTotalSum *(1+mTip)));

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

        for (BillRow row : billRows) {
            AddRowsToUi(row);
        }


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
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void StartSecondaryUser(final Context context,
                                   String dbPath,
                                   String storagePath,
                                   LinearLayout commonItemsArea,
                                   LinearLayout myItemsArea,
                                   TextView billSummarizerTotalSumView,
                                   EditText billSummarizerTipView){
        mContext = context;
        mCommonItemsArea = commonItemsArea;
        mMyItemsArea = myItemsArea;
        mBillSummarizerTotalSumView = billSummarizerTotalSumView;
        mBillSummarizerTipView = billSummarizerTipView;
        mBillSummarizerTipView.addTextChangedListener(new TextWatcher() {
            private String curTip = "10";
            public void afterTextChanged(Editable s) {
                if(s.toString().equalsIgnoreCase("")) {
                    mTip = 0;
                }else {
                    int newTip = Integer.parseInt(s.toString());
                    if (newTip < 0 || newTip > 100) {
                        mBillSummarizerTipView.setText(curTip);
                    } else {
                        curTip = s.toString();
                        mTip = (1.0*newTip)/100;
                        mBillSummarizerTotalSumView.setText(Double.toString(mMyTotalSum *(1+mTip)));

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
                                LinearLayout commonItemRow = new LinearLayout(mContext);
                                commonItemRow.setOrientation(LinearLayout.VERTICAL);

                                LinearLayout myItemRow = new LinearLayout(mContext);
                                myItemRow.setOrientation(LinearLayout.VERTICAL);

                                TextView commonPrice = new TextView(mContext);
                                commonPrice.setText("" + rowPrice);
                                commonItemRow.addView(commonPrice);

                                TextView myPrice = new TextView(mContext);
                                myPrice.setText("" + rowPrice);
                                myItemRow.addView(myPrice);

                                TextView commonQuantityView = new TextView(mContext);
                                commonQuantityView.setText("" + rowQuantity);
                                commonItemRow.addView(commonQuantityView);

                                TextView myQuantityView = new TextView(mContext);
                                myQuantityView.setText("0");
                                myItemRow.addView(myQuantityView);

                                Bitmap commonItemBitmap = FilesHandler.ConvertFirebaseBytesToBitmap(bytes, itemWidth, itemHeight);
                                Bitmap myItemBitmap = FilesHandler.ConvertFirebaseBytesToBitmap(bytes, itemWidth, itemHeight);

                                ImageView commonImageView = new ImageView(mContext);
                                commonImageView.setImageBitmap(commonItemBitmap);
                                commonItemRow.addView(commonImageView);

                                ImageView myImageView = new ImageView(mContext);
                                myImageView.setImageBitmap(myItemBitmap);
                                myItemRow.addView(myImageView);

                                int rowIndexInUi = GetRowUiIndex(rowIndex);
                                mCommonItemsArea.addView(commonItemRow, rowIndexInUi);

                                mMyItemsArea.addView(myItemRow, rowIndexInUi );
                                myItemRow.setVisibility(GONE);

                                Integer rowQuantityPrsed = Integer.parseInt(rowQuantity);
                                Double rowPriceParsed = Double.parseDouble(rowPrice);
                                mLineNumToPriceMapper.put(rowIndex, rowPriceParsed);

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

    }

    private int GetRowUiIndex(Integer newRowIndex) {
        if(mCommonLineNumToLineView.size() == 0){
            return 1;
        }

        Integer[] rowIndeces = new Integer[mCommonLineNumToLineView.size()];
        mCommonLineNumToLineView.keySet().toArray(rowIndeces);
        Arrays.sort(rowIndeces);
        if(newRowIndex < rowIndeces[0]){
            return 1;
        }
        for(int retVal = 1; retVal < rowIndeces.length; retVal++){
            if(newRowIndex < rowIndeces[retVal] && newRowIndex > rowIndeces[retVal-1]){
                return retVal + 1;
            }
        }

        return rowIndeces.length + 1;
    }

    private void AddRowsToUi(BillRow row) {
        LinearLayout commonItemRow = new LinearLayout(mContext);
        commonItemRow.setOrientation(LinearLayout.VERTICAL);

        LinearLayout myItemRow = new LinearLayout(mContext);
        myItemRow.setOrientation(LinearLayout.VERTICAL);

        TextView commonPrice = new TextView(mContext);
        commonPrice.setText(Double.toString(row.GetPrice()));
        commonItemRow.addView(commonPrice);

        TextView myPrice = new TextView(mContext);
        myPrice.setText(Double.toString(row.GetPrice()));
        myItemRow.addView(myPrice);

        mLineNumToPriceMapper.put(row.GetRowIndex(), row.GetPrice());

        TextView commonQuantityView = new TextView(mContext);
        commonQuantityView.setText(Integer.toString(row.GetQuantity()));
        commonItemRow.addView(commonQuantityView);

        TextView myQuantityView = new TextView(mContext);
        myQuantityView.setText("0");
        myItemRow.addView(myQuantityView);

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
                    mBillSummarizerTotalSumView.setText(Double.toString(mMyTotalSum *(1+mTip)));
                    return;
                }
            }
        }

        //move item from common Bill to my Bill and substract 1 from item's quantity
        if(((LinearLayout) v.getParent()).getId() == R.id.common_items_area_linearlayout){
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


                    mBillSummarizerTotalSumView.setText(Double.toString(mMyTotalSum *(1+mTip)));

                    return;
                }
            }
        }
    }
}
