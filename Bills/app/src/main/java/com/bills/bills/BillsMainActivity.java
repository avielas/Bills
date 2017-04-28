package com.bills.bills;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bills.billslib.Camera.CameraRenderer;
import com.bills.billslib.Camera.IOnCameraFinished;
import com.bills.billslib.Contracts.Constants;


import java.io.File;
import java.util.LinkedHashMap;

public class BillsMainActivity extends AppCompatActivity implements IOnCameraFinished, View.OnClickListener {
    private String Tag = this.getClass().getSimpleName();
    private static final int REQUEST_CAMERA_PERMISSION = 101;

    private static final int TAKE_PICTURE = 1;
    private Uri _cameraOutputFileUri;

    TextureView _cameraPreviewView = null;
    Button _cameraCaptureButton = null;

    ScrollView _billSummarizerContainer = null;
    LinearLayout _billSummarizerLayout = null;

    RelativeLayout _billsMainView;
    CameraRenderer _renderer;

    private LinkedHashMap billLines = new LinkedHashMap();
    private byte[] _pictureData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills_main);

        _billsMainView = (RelativeLayout)findViewById(R.id.activity_bills_main);

        _renderer = new CameraRenderer(this);
        _renderer.SetOnCameraFinishedListener(this);
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

        //removing prvious views
        _pictureData = image;

        _billsMainView.removeView(_cameraCaptureButton);
        _billsMainView.removeView(_cameraPreviewView);

        //adding bill summarizer view
        _billSummarizerContainer = new ScrollView(this);
        _billsMainView.addView(_billSummarizerContainer);
        _billSummarizerLayout = new LinearLayout(this);
        _billSummarizerContainer.addView(_billSummarizerLayout);

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
