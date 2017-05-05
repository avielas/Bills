package com.bills.bills;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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


import org.opencv.core.Point;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;
import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class BillsMainActivity extends AppCompatActivity implements IOnCameraFinished, View.OnClickListener {
    private String Tag = this.getClass().getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private static final int TAKE_PICTURE = 1;
    private Uri _cameraOutputFileUri;

    TextureView _cameraPreviewView = null;
    Button _cameraCaptureButton = null;

    LinearLayout _billSummarizerLayout = null;
    EditText _billSummarizerTip = null;
    ScrollView _billSummarizerItemsSection = null;
    LinearLayout _billSummarizerItemsLayout = null;
    LinearLayout _billSummarizerUsersLayout = null;


    RelativeLayout _billsMainView;
    CameraRenderer _renderer;

    private LinkedHashMap billLines = new LinkedHashMap();
    private byte[] _pictureData;

    IOcrEngine _ocrEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills_main);

        _billsMainView = (RelativeLayout)findViewById(R.id.activity_bills_main);

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
            }
        }
    }

    private void StartCameraActivity() {
        try {
            String imagePathToSave = Constants.CAMERA_CAPTURED_PHOTO_PATH;
            File file = new File(imagePathToSave);
            _cameraOutputFileUri = Uri.fromFile(file);

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

            _billsMainView.addView(_cameraPreviewView);

            _cameraCaptureButton = new Button(this);
            _cameraCaptureButton.setText("Capture");
            _cameraCaptureButton.setOnClickListener(this);

            RelativeLayout.LayoutParams buttonLayoutParameters = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            buttonLayoutParameters.addRule(RelativeLayout.CENTER_IN_PARENT);
            buttonLayoutParameters.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

            _billsMainView.addView(_cameraCaptureButton, buttonLayoutParameters);
        } catch (Exception e) {
            Log.e(Tag, e.getMessage());
        }
    }

    @Override
    public void OnCameraFinished(byte[] image) {
        //Add parsing here

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
        _pictureData = image;

        BitmapFactory.Options options = new BitmapFactory.Options();

        bitmap = BitmapFactory.decodeFile(Constants.IMAGES_PATH+"/tmp.bmp", options);

        //removing prvious views
        _billsMainView.removeView(_cameraCaptureButton);
        _billsMainView.removeView(_cameraPreviewView);

        AddBillSummarizerView();


        BillAreaDetector areaDetector = new BillAreaDetector();
        Point topLeft = new Point();
        Point topRight = new Point();
        Point buttomRight = new Point();
        Point buttomLeft = new Point();
        if(!areaDetector.GetBillCorners(bitmap, topLeft,topRight, buttomRight, buttomLeft)){
            //TODO: add drag rect view here
            Log.d(Tag, "Failed\n");
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            _billsMainView.addView(imageView);
            return;
        }

        if(!ImageProcessingLib.WarpPerspective(bitmap, bitmap, topLeft,topRight, buttomRight, buttomLeft)){
            //TODO: decide what to do. Retake the picture? crash the app?
            TextView textView = new TextView(this);
            textView.setText("Failed to warp perspective on the image.");
            _billSummarizerLayout.addView(textView);

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            _billSummarizerLayout.addView(imageView);
            return;
        }

        Bitmap processedWarpedBill =  ImageProcessingLib.PreprocessingForTemplateMatcher(bitmap);
        Bitmap processedWarpedBillForCreateNewBill =  ImageProcessingLib.PreprocessingForParsing(bitmap);
        TemplateMatcher templateMatcher = new TemplateMatcher(_ocrEngine, processedWarpedBillForCreateNewBill, processedWarpedBill);

        Bitmap itemsArea = templateMatcher.MatchWhichReturnCroppedItemsArea();
        Bitmap processedItemsArea =   ImageProcessingLib.PreprocessingForParsing(itemsArea);

        int numOfItems = templateMatcher.priceAndQuantity.size();
        templateMatcher = new TemplateMatcher(_ocrEngine, processedItemsArea);
        templateMatcher.ParsingItemsArea(numOfItems);

        processedWarpedBill.recycle();
        processedWarpedBillForCreateNewBill.recycle();
        bitmap.recycle();
        itemsArea.recycle();
        processedItemsArea.recycle();

        int i = 0;
        int[] colors = {Color.RED, Color.BLUE, Color.GREEN};
        for(Double[] priceQuantity : templateMatcher.priceAndQuantity){
            ItemView itemView = new ItemView(this, priceQuantity[0], templateMatcher.itemLocationsByteArray.get(i));
            itemView.SetItemBackgroundColor(colors[i]);
            _billSummarizerItemsLayout.addView(itemView);
            i++;
        }

        _billSummarizerUsersLayout.addView(new NameView(this, "Aviel", 10));
        _billSummarizerUsersLayout.addView(new NameView(this, "Mike", 10));
    }

    private void AddBillSummarizerView() {
        _billSummarizerLayout = new LinearLayout(this);
        _billSummarizerLayout.setOrientation(LinearLayout.HORIZONTAL);

        _billsMainView.addView(_billSummarizerLayout);

        _billSummarizerTip = new EditText(this);
        _billSummarizerTip.setClickable(true);
        _billSummarizerTip.setOnClickListener(this);
        _billSummarizerTip.setInputType(TYPE_NUMBER_FLAG_DECIMAL);
        _billSummarizerTip.setText("10");
        _billSummarizerLayout.addView(_billSummarizerTip);

        _billSummarizerItemsSection = new ScrollView(this);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(MATCH_PARENT, 60);
        _billSummarizerItemsSection.setLayoutParams(params);
        _billSummarizerLayout.addView(_billSummarizerItemsSection);

        _billSummarizerItemsLayout = new LinearLayout(this);
        params = new RelativeLayout.LayoutParams(MATCH_PARENT, 50);
        _billSummarizerItemsLayout.setLayoutParams(params);
        _billSummarizerItemsLayout.setOrientation(LinearLayout.VERTICAL);
        _billSummarizerItemsSection.addView(_billSummarizerItemsLayout);

        _billSummarizerUsersLayout = new LinearLayout(this);
        _billSummarizerLayout.addView(_billSummarizerUsersLayout);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    StartCameraActivity();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        _renderer.takePicture();
    }
}
