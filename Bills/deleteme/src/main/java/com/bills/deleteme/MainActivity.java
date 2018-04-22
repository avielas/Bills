package com.bills.deleteme;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bills.billslib.Utilities.Utilities;
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
import com.google.firebase.database.Transaction.Result;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

//TODO: sync database and storage timestamps
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String ANONYMOUS = "anonymous";
    private static final int RC_SIGN_IN = 123;

    private Integer passCode = -1;
    private List<PriceQuantityItem> mRows;
    static Random random = new Random(10);

    final Object transactionLock = new Object();
    final ConcurrentHashMap<String, Integer> passCodes = new ConcurrentHashMap<>();
    final AtomicInteger newPassCode = new AtomicInteger(Integer.MIN_VALUE);
    final AtomicBoolean transactionFinished = new AtomicBoolean(false);


    private ArrayList<Integer> mLineToQuantityMapper = new ArrayList<>();

    private String mBillStoragePath;

    private Button mUploadButton;
    private Button mUpadteButton;

    private Uri mPhotoUri;

    //Firebase Authentication members
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private String mUsername;


    //Firebase Database members
    private FirebaseDatabase mFirebaseDataBase;
    private DatabaseReference mUsersDatabaseReference;
    private DatabaseReference mUserIdsDatabaseReference;
    private ChildEventListener mChildEventListener;

    //Firebase Storage members
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mBillsPerUserStorageReference;

    private String mUid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUploadButton = (Button)findViewById(R.id.uploadButton);
        mUploadButton.setOnClickListener(this);

        mUpadteButton = (Button)findViewById(R.id.updateButton);
        mUpadteButton.setOnClickListener(this);

        mUsername = ANONYMOUS;
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    //onSignedInInitialize(user.getDisplayName());
                    mUid = user.getUid();
                    String timeStamp = Utilities.GetTimeStamp();

                    mBillStoragePath = mUid + "/" + timeStamp;

                    mUsersDatabaseReference = mFirebaseDataBase.getReference().child("users/" + "/" + mBillStoragePath);
                    mUserIdsDatabaseReference = mFirebaseDataBase.getReference().child("userIds");

                    Toast.makeText(MainActivity.this, "You are now signed in. Welcome", Toast.LENGTH_LONG).show();
                }else{
                    //user is signed out
                    //onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        mFirebaseDataBase = FirebaseDatabase.getInstance();

        mFirebaseStorage = FirebaseStorage.getInstance();
        mBillsPerUserStorageReference = mFirebaseStorage.getReference().child("BillsPerUser");
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
        //TODO: clear all displayed data
    }

    @Override
    public void onActivityResult(int requesCode, int resultCode, Intent data){
        super.onActivityResult(requesCode, resultCode, data);

        if(requesCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                Toast.makeText(this, "Signed in!!!", Toast.LENGTH_LONG).show();
            }else if(resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void UploadBillToDB() {
        mRows = GetBillRows(10);

        passCode = GetPassCode();

    }

    private Integer GetPassCode() {
            MyHandler handler = new MyHandler();
            mUserIdsDatabaseReference.runTransaction(handler);
//
//            while (!transactionFinished.get()) {
//            }

//            if (newPassCode.get() >= 0) {
//                break;
//            }

        return newPassCode.get();
    }

    private List<PriceQuantityItem> GetBillRows(int numOfRows) {
        List<PriceQuantityItem> items = new ArrayList<>();
        double startingPrice = 1.0;
        int startingQuantity = 1;
        for(int i = 0; i < numOfRows; i++){
            items.add(new PriceQuantityItem(startingPrice++, startingQuantity++, GetBitmap(""+i+i+i)));
        }
        return items;
    }

    private Bitmap GetBitmap(String text) {
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

        canvas.drawText(text, 30, 30, paint);

        return res;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.uploadButton) {
            UploadBillToDB();
        }
        else if(v.getId() == R.id.updateButton){
            UpdateDb();
        }
    }

    private void UpdateDb() {
        final int lineNum = random.nextInt(9) + 1;
        int newQuantity = random.nextInt(9) + 1;

        final Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(""+lineNum, newQuantity);

        mUserIdsDatabaseReference.orderByValue().equalTo("1234").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                DatabaseReference billLineDBReference = mFirebaseDataBase.getReference().child("users/" + dataSnapshot.getRef().getParent().getKey()+"/"+lineNum);
                billLineDBReference.runTransaction(new Transaction.Handler() {
                    @Override
                    public Result doTransaction(MutableData mutableData) {
                        int curValue;
                        try {
                            curValue = (int) mutableData.getValue();
                        }catch (Exception ex){
                            //something went wrong, there sopposed to be no problems with parsing the value!!!
                            return Transaction.success(mutableData);
                        }
                        if(curValue <= 0){
                            //dont update anything and write something to the user
                            return Transaction.success(mutableData);
                        }else{
                            mutableData.setValue(curValue-1);
                            return Transaction.success(mutableData);
                        }
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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

    private void AttachDatabaseListener(){
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Integer index = Integer.parseInt(dataSnapshot.getKey());
                    Integer quantity = dataSnapshot.getValue(Integer.class);

                    mLineToQuantityMapper.add(index, quantity);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    Integer index = Integer.parseInt(dataSnapshot.getKey());
                    Integer quantity = dataSnapshot.getValue(Integer.class);

                    mLineToQuantityMapper.set(index, quantity);
                    UpdateUIElement(index, quantity);
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };

            mUsersDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void UpdateUIElement(Integer lineNumber, Integer newQuantity) {

    }

    public class MyHandler implements Transaction.Handler {
        @Override
        public Result doTransaction(final MutableData mutableData) {
            synchronized (this) {
                if (mutableData != null && mutableData.hasChildren()) {
                    if (mutableData.hasChild(mUid)) {
                        newPassCode.set(mutableData.child(mUid).getValue(Integer.class));
                    } else {
                        for (MutableData childMutableData : mutableData.getChildren()) {
                            passCodes.put(childMutableData.getKey(), childMutableData.getValue(Integer.class));
                        }

                        //find an unused pass code
                        for (int i = 0; i < 10000; i++) {
                            if (!passCodes.containsValue(i)) {
                                newPassCode.set(i);
                                mutableData.child(mUid).setValue(i);
                                break;
                            }
                        }
                    }
                }
                return Transaction.success(mutableData);
            }
        }

        @Override
        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

            passCode = newPassCode.get();
            Toast.makeText(MainActivity.this, "new pass code=" + passCode, Toast.LENGTH_LONG).show();


            LinearLayout itemsView = (LinearLayout) findViewById(R.id.itemsView);

            int i = 0;
            Map<String, Object> dbItems = new HashMap<>();

            for (PriceQuantityItem row : mRows) {
                Buffer buffer = ByteBuffer.allocate(row.Item.getByteCount());
                row.Item.copyPixelsToBuffer(buffer);
                byte[] data = (byte[]) buffer.array();

                final StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType("image/jpg")
                        .setCustomMetadata("width", Integer.toString(row.Item.getWidth()))
                        .setCustomMetadata("height", Integer.toString(row.Item.getHeight()))
                        .setCustomMetadata("Price", "" + row.Price)
                        .setCustomMetadata("Quantity", "" + row.Quantity)
                        .build();


                final String item = "" + i;
                final Integer itemIndex = new Integer(i);
                final Integer itemQuantity = new Integer(row.Quantity);
                final StorageReference billRef = mBillsPerUserStorageReference.child(mBillStoragePath + "/" + item);
                billRef.putBytes(data).addOnSuccessListener(MainActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        billRef.updateMetadata(metadata);
                    }
                });

                dbItems.put("" + i, row.Quantity);

                LinearLayout itemRow = new LinearLayout(MainActivity.this);
                itemRow.setOrientation(LinearLayout.VERTICAL);

                TextView price = new TextView(MainActivity.this);
                price.setText("" + row.Price);
                itemRow.addView(price);

                TextView quantity = new TextView(MainActivity.this);
                quantity.setText("" + row.Quantity);
                itemRow.addView(quantity);

                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setImageBitmap(row.Item);
                itemRow.addView(imageView);

                itemsView.addView(itemRow);
                i++;
            }

            mUsersDatabaseReference.updateChildren(dbItems);

            EditText editText = (EditText) findViewById(R.id.passCode);
            editText.setText(""+passCode);

            AttachDatabaseListener();
        }

    }
}
