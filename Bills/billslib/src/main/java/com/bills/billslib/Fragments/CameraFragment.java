package com.bills.billslib.Fragments;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bills.billslib.R;
import com.bills.billslib.Camera.CameraRenderer;
import com.bills.billslib.Camera.IOnCameraFinished;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnCameraFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class CameraFragment extends Fragment implements View.OnClickListener, IOnCameraFinished {
    protected String Tag = CameraFragment.class.getName();

    //Camera Renderer
    private CameraRenderer mRenderer;

    //Camera Elements
    private TextureView mCameraPreviewView = null;
    private Button mCameraCaptureButton = null;

    //selection order: auto->on->off
    private Button mCameraFlashMode = null;
    private Integer mCurrentFlashMode = R.drawable.camera_screen_flash_auto;

    protected OnCameraFragmentInteractionListener mListener;

    public CameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCameraFragmentInteractionListener) {
            mListener = (OnCameraFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCameraFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mRenderer = new CameraRenderer(getContext());
        mRenderer.SetOnCameraFinishedListener(this);

        mCameraPreviewView = (TextureView) getView().findViewById(R.id.camera_textureView);
        mCameraPreviewView.setSurfaceTextureListener(mRenderer);
        mCameraPreviewView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
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

        mCameraCaptureButton = (Button) getView().findViewById(R.id.camera_capture_button);
        mCameraCaptureButton.setOnClickListener(this);

        mCameraFlashMode = (Button)getView().findViewById(R.id.camera_flash_mode);
        mCameraFlashMode.setBackgroundResource(mCurrentFlashMode);
        mCameraFlashMode.setTag(mCurrentFlashMode);
        mCameraFlashMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCurrentFlashMode == R.drawable.camera_screen_flash_auto){
                    mCurrentFlashMode = R.drawable.camera_screen_flash_on;
                    mRenderer.SetFlashMode(Camera.Parameters.FLASH_MODE_ON);

                }else if(mCurrentFlashMode == R.drawable.camera_screen_flash_on){
                    mCurrentFlashMode = R.drawable.camera_screen_flash_off;
                    mRenderer.SetFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }else {
                    mCurrentFlashMode = R.drawable.camera_screen_flash_auto;
                    mRenderer.SetFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                }
                mCameraFlashMode.setBackgroundResource(mCurrentFlashMode);
            }
        });
    }

    @Override
    public void onClick(View v) {
        mRenderer.setAutoFocus();
        mRenderer.takePicture();
    }

    @Override
    public void OnCameraFinished(final byte[] image) {
        mListener.onCameraSuccess(image);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnCameraFragmentInteractionListener {
        void onCameraSuccess(byte[] image);
}
}
