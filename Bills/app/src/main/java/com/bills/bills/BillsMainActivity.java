package com.bills.bills;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
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
import com.bills.billslib.Contracts.IOcrEngine;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Core.TemplateMatcher;
import com.bills.billslib.Core.TesseractOCREngine;
import com.bills.billslib.CustomViews.ItemView;
import com.bills.billslib.CustomViews.NameView;


import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.HashMap;

import static android.view.View.GONE;

public class BillsMainActivity extends AppCompatActivity implements IOnCameraFinished, View.OnClickListener {
    private String Tag = this.getClass().getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSION = 101;


    RelativeLayout _cameraPreviewLayout = null;
    TextureView _cameraPreviewView = null;
    Button _cameraCaptureButton = null;

    LinearLayout _billSummarizerContainerView = null;
    EditText _billSummarizerTip = null;
    LinearLayout _billSummarizerItemsLayout = null;
    LinearLayout _billSummarizerUsersLayout = null;
    TextView _billSummarizerTotalSum = null;

    LinearLayout _billsMainView;
    CameraRenderer _renderer;



    HashMap<Integer, NameView> _billSummarizerColorToViewMapper = new HashMap<>();
    HashMap<ItemView, Integer> _billSummarizerItemToColorMapper = new HashMap<>();
    Double _marked = 0.0;
    Double _markedWithTip = 0.0;
    Double _total = 0.0;

    IOcrEngine _ocrEngine;
    private int tip = 10;
    private int currentColorIndex = -1;
    private NameView curNameView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills_main);

        _billsMainView = (LinearLayout) findViewById(R.id.activity_bills_main);

        _billSummarizerTotalSum = (TextView)findViewById(R.id.totalSum);
        _billSummarizerTotalSum.setVisibility(GONE);
        _billSummarizerTip = (EditText)findViewById(R.id.tipTextView);
        _billSummarizerTip.setVisibility(GONE);
        _billSummarizerItemsLayout = (LinearLayout)findViewById(R.id.itemsView);
        _billSummarizerItemsLayout.setVisibility(GONE);
        _billSummarizerUsersLayout = (LinearLayout)findViewById(R.id.namesView);
        _billSummarizerUsersLayout.setVisibility(GONE);
        _billSummarizerContainerView = (LinearLayout)findViewById(R.id.summarizerContainerView);
        _billSummarizerContainerView.setVisibility(GONE);

        _renderer = new CameraRenderer(this);
        _renderer.SetOnCameraFinishedListener(this);

        if(_ocrEngine == null){
            try {
                _ocrEngine = new TesseractOCREngine();
                _ocrEngine.Init(Constants.TESSERACT_SAMPLE_DIRECTORY, Language.Hebrew);
            }catch (Exception ex){
                TextView textView = new TextView(this);
                textView.setText("Failed to initialize " + _ocrEngine.getClass().getSimpleName() + ". Error: " + ex.getMessage());
                _billsMainView.addView(textView);
                return;
            }
        }

        //first visit of on create
        if(savedInstanceState == null){
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Camera access is required.", Toast.LENGTH_SHORT).show();

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION);
                }

            }else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            1);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                StartCameraActivity();
//                StartSummarizerView();
            }
        }
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
            _billSummarizerItemsLayout.addView(itemView, i);
            _billSummarizerItemToColorMapper.put(itemView, color);
            _total+=prices[i];
        }

        NameView nameView = new NameView(this, "Aviel", 10);
        nameView.setBackgroundColor(Color.RED);
        nameView.setOnClickListener(this);
        _billSummarizerUsersLayout.addView(nameView);
        _billSummarizerColorToViewMapper.put(Color.RED, nameView);
        nameView = new NameView(this, "Mike", 10);
        nameView.setBackgroundColor(Color.BLUE);
        nameView.setOnClickListener(this);
        _billSummarizerUsersLayout.addView(nameView);
        _billSummarizerColorToViewMapper.put(Color.BLUE, nameView);


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
            _cameraPreviewLayout = new RelativeLayout(this);
            _billsMainView.addView(_cameraPreviewLayout);

            _cameraPreviewView = new TextureView(this);
            _cameraPreviewView.setSurfaceTextureListener(_renderer);
            _cameraPreviewView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            //_renderer.set_selectedFilter(R.id.filter0);
                            _renderer.setAutoFocus();
                            break;
                    }
                    return true;
                }
            });
            _cameraPreviewView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    _renderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
                }
            });

            _cameraPreviewLayout.addView(_cameraPreviewView);

            _cameraCaptureButton = new Button(this);
            _cameraCaptureButton.setText("Capture");
            _cameraCaptureButton.setOnClickListener(this);

            RelativeLayout.LayoutParams buttonLayoutParameters = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            buttonLayoutParameters.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            buttonLayoutParameters.addRule(RelativeLayout.CENTER_HORIZONTAL);

            _cameraPreviewLayout.addView(_cameraCaptureButton, buttonLayoutParameters);
        } catch (Exception e) {
            Log.e(Tag, e.getMessage());
        }
    }

    @Override
    public void OnCameraFinished(byte[] image) {

//        StartSummarizerView();
//        return;
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
//            _billsMainView.addView(imageVieww);
//            return;
        }

//        BitmapFactory.Options options = new BitmapFactory.Options();
//
//        bitmap = BitmapFactory.decodeFile(Constants.IMAGES_PATH+"/tmp.bmp", options);

        //removing prvious views
        _billsMainView.removeView(_cameraCaptureButton);
        _billsMainView.removeView(_cameraPreviewView);
        _billsMainView.removeView(_cameraPreviewLayout);

        AddBillSummarizerView();


        BillAreaDetector areaDetector = new BillAreaDetector();
        Point topLeft = new Point();
        Point topRight = new Point();
        Point buttomRight = new Point();
        Point buttomLeft = new Point();
        if (!OpenCVLoader.initDebug()) {
            Log.d("aa", "Failed to initialize OpenCV.");
        }

        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        if(!areaDetector.GetBillCorners(mat, topLeft,topRight, buttomRight, buttomLeft)){
            //TODO: add drag rect view here
            Log.d(Tag, "Failed\n");
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            _billsMainView.addView(imageView);
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
            TextView textView = new TextView(this);
            textView.setText("Failed to warp perspective on the image.");
            _billSummarizerContainerView.addView(textView);
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
//        _billsMainView.addView(imageView);
//
//        imageView = new ImageView(this);
//        imageView.setImageBitmap(warpedBitmap);
//        _billsMainView.addView(imageView);
//        return;

        Bitmap processedBillBitmap = Bitmap.createBitmap(warpedMat.width(), warpedMat.height(), Bitmap.Config.ARGB_8888);
        ImageProcessingLib.PreprocessingForTM(warpedMat);
        Utils.matToBitmap(warpedMat, processedBillBitmap);

        TemplateMatcher templateMatcher = new TemplateMatcher(_ocrEngine, processedBillBitmap);
        templateMatcher.Match();

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

        int i = 0;
        int[] colors = {Color.RED/*, Color.BLUE, Color.GREEN, Color.BLACK*/};
        for(Double[] priceQuantity : templateMatcher.priceAndQuantity){
            ItemView itemView = new ItemView(this, priceQuantity[0], templateMatcher.itemLocationsByteArray.get(i));
            itemView.SetItemBackgroundColor(colors[0]);
            _billSummarizerItemsLayout.addView(itemView);
            i++;
        }

        _billSummarizerUsersLayout.addView(new NameView(this, "Aviel", 10));
        _billSummarizerUsersLayout.addView(new NameView(this, "Mike", 10));
    }

    private void AddBillSummarizerView() {
        _billSummarizerContainerView.setVisibility(View.VISIBLE);
        _billSummarizerTip.setVisibility(View.VISIBLE);
        _billSummarizerItemsLayout.setVisibility(View.VISIBLE);
        _billSummarizerTotalSum.setVisibility(View.VISIBLE);
        _billSummarizerUsersLayout.setVisibility(View.VISIBLE);

        _billSummarizerTip.setClickable(true);
        _billSummarizerTip.addTextChangedListener(new TextWatcher() {
            private String curTip = "10";
            public void afterTextChanged(Editable s) {
                if(s.toString().equalsIgnoreCase("")) {
                    tip = 0;
                }else {
                    int newTip = Integer.parseInt(s.toString());
                    if (newTip < 0 || newTip > 100) {
                        _billSummarizerTip.setText(curTip);
                    } else {
                        curTip = s.toString();
                        tip = newTip;
                        _markedWithTip = _marked * (1 + (double) tip / 100);

                        _billSummarizerTotalSum.setText("Total: " + _total + "\n Marked: " + _marked + "(" + String.format("%.2f", _markedWithTip) + ")");
                    }
                }
                for(NameView name : _billSummarizerColorToViewMapper.values()){
                    name.SetTip(tip);
                }
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });
        _billSummarizerTotalSum.setClickable(false);
        _billSummarizerTotalSum.setText("Total: " + _total + "\n Marked: " + _marked + "(" + String.format("%.2f", _markedWithTip) + ")");
        _billSummarizerTip.setText("10", TextView.BufferType.EDITABLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    StartCameraActivity();
//                    StartSummarizerView();

                }
            }
        }
    }

    @Override
    public void onClick(View v) {

        if(v == _billSummarizerTip) {

            return;
        }
        if (v == _cameraCaptureButton){
            _renderer.takePicture();
            return;
        }
        if(((LinearLayout)v.getParent()).getId() == R.id.namesView) {
            currentColorIndex = ((ColorDrawable) v.getBackground()).getColor();
            curNameView = (NameView) v;
            return;
        }

        //item selected
        if(((LinearLayout) v.getParent()).getId() == R.id.itemsView){
            //no color was chosen, nothing to do
            if(currentColorIndex == Color.WHITE){
                return;
            }

            int color = 0;
            ItemView currentItemView;
            if (v instanceof ItemView)
            {
                currentItemView = (ItemView)v;
            }
            else{
                currentItemView = (ItemView)(v.getParent());
            }
            color = ((ColorDrawable)(v.getBackground())).getColor();

            //the item has not been marked yet
            if(color == Color.WHITE) {
                _marked += currentItemView.Price;
                curNameView.AddToBill(((ItemView)v).Price);
            }
            else {
                _billSummarizerColorToViewMapper.get(color).RemvoeFromBill(((ItemView)v).Price);
                _billSummarizerColorToViewMapper.get(currentColorIndex).AddToBill(((ItemView)v).Price);
            }

            currentItemView.SetItemBackgroundColor(currentColorIndex);
            _markedWithTip = _marked * (1+ (double)tip/100);
            _billSummarizerTotalSum.setText("Total/Marked: " + _total + "/" + _marked + "(" + String.format("%.2f", _markedWithTip) + ")");
        }

    }
}
