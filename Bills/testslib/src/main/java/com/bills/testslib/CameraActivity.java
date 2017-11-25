package com.bills.testslib;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bills.billslib.Camera.CameraRenderer;
import com.bills.billslib.Camera.IOnCameraFinished;
import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Utilities.Utilities;

import java.util.UUID;

public class CameraActivity extends AppCompatActivity implements IOnCameraFinished, View.OnClickListener{
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private CameraRenderer _renderer;
    private UUID _sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills_main);
        _renderer = new CameraRenderer(this);
        _renderer.SetOnCameraFinishedListener(this);

        Bundle extras = getIntent().getExtras();
        _sessionId = (UUID)extras.get("UUID");

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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.captureButton) {
            _renderer.takePicture();
            return;
        }
    }

    private void StartCameraActivity() {
        try {
            TextureView textureView = (TextureView)findViewById(R.id.textureView);
            textureView.setSurfaceTextureListener(_renderer);
            textureView.setOnTouchListener(new View.OnTouchListener() {
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
            textureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    _renderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
                }
            });
            Button _cameraCaptureButton = (Button)findViewById(R.id.captureButton);
            _cameraCaptureButton.setOnClickListener(this);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void FinishActivity() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void OnCameraFinished(byte[] bytes){
        String timeStamp = Utilities.GetTimeStamp();
        String fileFullName = Constants.IMAGES_PATH + "/ocrBytes_" + timeStamp + ".txt";
        Utilities.SaveToTXTFile(_sessionId, bytes, fileFullName);
        FinishActivity();
    }
}




