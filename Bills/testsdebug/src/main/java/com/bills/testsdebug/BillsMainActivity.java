package com.bills.testsdebug;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bills.billslib.Camera.CameraRenderer;
import com.bills.billslib.Camera.IOnCameraFinished;
import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.ImageProcessingLib;
import com.bills.billslib.Utilities.FilesHandler;

import com.bills.billslib.CustomViews.DragRectView;
import org.opencv.core.Point;

import java.io.IOException;

public class BillsMainActivity extends AppCompatActivity implements IOnCameraFinished, View.OnClickListener{

    public static final String BILLS_CROPPED_PHOTO_EXTRA_NAME = "imagePathToSave";
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private Button mUserCropFinished;
    private DragRectView mDragRectView;
    private Point mTopLeft = new Point();
    private Point mTopRight = new Point();
    private Point mButtomLeft = new Point();
    private Point mButtomRight = new Point();
    private RelativeLayout mMainLayout;
    private Bitmap mOriginalImage;
    private String _imagePathToSave;
    private CameraRenderer _renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bills_main);

        Intent intent = getIntent();
        _imagePathToSave = intent.getStringExtra(BILLS_CROPPED_PHOTO_EXTRA_NAME);

        mMainLayout = (RelativeLayout)findViewById(R.id.activity_bills_main);
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
//        Starting the main flow
        if(v.getId() == R.id.captureButton) {
            _renderer.takePicture();
            return;
        }
        // User finished marking the bill corners
        else if(v == mUserCropFinished){
            double stretchFactorX = (1.0 * mOriginalImage.getWidth()) / mDragRectView.getBackground().getBounds().width();
            double stretchFactorY = (1.0 * mOriginalImage.getHeight()) / mDragRectView.getBackground().getBounds().height();

            double x = mDragRectView.TopLeft.x * stretchFactorX;
            double y = mDragRectView.TopLeft.y * stretchFactorY ;
            mTopLeft = new Point(x,y);

            x = mDragRectView.TopRight.x * stretchFactorX;
            y = mDragRectView.TopRight.y * stretchFactorY;
            mTopRight = new Point(x,y);

            x = mDragRectView.ButtomLeft.x * stretchFactorX;
            y = mDragRectView.ButtomLeft.y * stretchFactorY;
            mButtomLeft= new Point(x,y);

            x = mDragRectView.ButtomRight.x * stretchFactorX;
            y = mDragRectView.ButtomRight.y * stretchFactorY;
            mButtomRight = new Point(x,y);

            /** Preparing Warp Perspective Dimensions **/
            int newWidth = (int) Math.max(mButtomRight.x - mButtomLeft.x, mTopRight.x - mTopLeft.x);
            int newHeight = (int) Math.max(mButtomRight.y - mTopRight.y, mButtomLeft.y - mTopLeft.y);
            int xBegin = (int) Math.min(mTopLeft.x, mButtomLeft.x);
            int yBegin = (int) Math.min(mTopLeft.y, mTopRight.y);
            Bitmap resizedBitmap = Bitmap.createBitmap(mOriginalImage, xBegin, yBegin, newWidth, newHeight);
            Bitmap warpedBitmap = Bitmap.createBitmap(newWidth , newHeight, mOriginalImage.getConfig());
            mTopLeft.x = mTopLeft.x - xBegin;
            mTopLeft.y = mTopLeft.y - yBegin;
            mTopRight.x = mTopRight.x - xBegin;
            mTopRight.y = mTopRight.y - yBegin;
            mButtomRight.x = mButtomRight.x - xBegin;
            mButtomRight.y = mButtomRight.y - yBegin;
            mButtomLeft.x = mButtomLeft.x - xBegin;
            mButtomLeft.y = mButtomLeft.y - yBegin;

            if(!ImageProcessingLib.WarpPerspective(resizedBitmap, warpedBitmap, mTopLeft, mTopRight, mButtomRight, mButtomLeft)) {
                Log.d(this.getClass().getSimpleName(), "Failed to warp perspective");
                mOriginalImage.recycle();
                warpedBitmap.recycle();
                return;
            }

            mOriginalImage.recycle();
            resizedBitmap.recycle();

            String warpedPath = Constants.WARPED_PHOTO_PATH;
            byte[] bytes;

            try {
                bytes = FilesHandler.BitmapToByteArray(warpedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(this.getClass().getSimpleName(), "BitmapToByteArray Failed");
                warpedBitmap.recycle();
                return;
            }

            if (!FilesHandler.SaveToTXTFile(bytes, Constants.WARPED_TXT_PHOTO_PATH)) {
                    Log.d(this.getClass().getSimpleName(), "Failed to store proccessed image to " + warpedPath);
                    warpedBitmap.recycle();
                    return;
            }
            FilesHandler.SaveToJPGFile(warpedBitmap, Constants.WARPED_JPG_PHOTO_PATH);
            warpedBitmap.recycle();
            FinishActivity();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){}

    private void FinishActivity() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void OnCameraFinished(byte[] image){
        FilesHandler.SaveToTXTFile(image, Constants.CAMERA_CAPTURED_TXT_PHOTO_PATH);
//        byte[] readImage = null;
//        readImage = FilesHandler.ReadFromTXTFile(Constants.CAMERA_CAPTURED_TXT_PHOTO_PATH);
        mOriginalImage = FilesHandler.ByteArrayToBitmap(image);
        mOriginalImage = FilesHandler.Rotating(mOriginalImage);
        FilesHandler.SaveToJPGFile(mOriginalImage, Constants.CAMERA_CAPTURED_JPG_PHOTO_PATH);
//        FilesHandler.SaveToTIFFile(mOriginalImage, Constants.CAMERA_CAPTURED_PHOTO_PATH);
        mUserCropFinished = new Button(this);
        mUserCropFinished.setText("Done");
        mUserCropFinished.setOnClickListener(this);
        mDragRectView = new DragRectView(this);
        BillAreaDetector areaDetector = new BillAreaDetector();

        if (!areaDetector.GetBillCorners(mOriginalImage , mTopLeft, mTopRight, mButtomRight, mButtomLeft)) {
            Log.d(this.getClass().getSimpleName(), "Failed ot get bounding rectangle automatically.");
            mDragRectView.TopLeft = null;
            mDragRectView.TopRight = null;
            mDragRectView.ButtomLeft = null;
            mDragRectView.ButtomRight = null;
        }
        else {
            int x = (int) Math.round((720.0/mOriginalImage.getWidth())*mTopLeft.x);
            int y = (int) Math.round((1118.0/mOriginalImage.getHeight())*mTopLeft.y);
            mDragRectView.TopLeft = new android.graphics.Point(x, y);

            x = (int) Math.round((720.0/mOriginalImage.getWidth())*mTopRight.x);
            y = (int) Math.round((1118.0/mOriginalImage.getHeight())*mTopRight.y);
            mDragRectView.TopRight = new android.graphics.Point(x, y);

            x = (int) Math.round((720.0/mOriginalImage.getWidth())*mButtomRight.x);
            y = (int) Math.round((1118.0/mOriginalImage.getHeight())*mButtomRight.y);
            mDragRectView.ButtomRight = new android.graphics.Point(x, y);

            x = (int) Math.round((720.0/mOriginalImage.getWidth())*mButtomLeft.x);
            y = (int) Math.round((1118.0/mOriginalImage.getHeight())*mButtomLeft.y);
            mDragRectView.ButtomLeft = new android.graphics.Point(x, y);
        }

        BitmapDrawable bitmapDrawable = new BitmapDrawable(mOriginalImage);
        mDragRectView.setBackground(bitmapDrawable);
        mDragRectView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        mUserCropFinished.setLayoutParams(params);
        mMainLayout.addView(mDragRectView);
        mMainLayout.addView(mUserCropFinished);
    }
}




