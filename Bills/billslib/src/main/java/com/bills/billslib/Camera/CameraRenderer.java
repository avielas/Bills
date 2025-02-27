/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bills.billslib.Camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.TextureView;

import com.bills.billslib.Camera.filter.BarrelBlurFilter;
import com.bills.billslib.Camera.filter.CameraFilter;
import com.bills.billslib.Camera.filter.OriginalFilter;
import com.bills.billslib.Camera.filter.TiltShiftBlurFilter;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.R;
import com.bills.billslib.Utilities.Utilities;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.UUID;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;


public class CameraRenderer implements Runnable, TextureView.SurfaceTextureListener {
    private static final String Tag = CameraRenderer.class.getName();
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int DRAW_INTERVAL = 1000 / 30;

    private Thread _renderThread;
    private Context _context;
    private SurfaceTexture _surfaceTexture;
    private int _gwidth, _gheight;

    private EGLDisplay _eglDisplay;
    private EGLSurface _eglSurface;
    private EGLContext _eglContext;
    private EGL10 _egl10;
    private UUID _sessionId;

    private Camera _camera;
    private SurfaceTexture _cameraSurfaceTexture;
    private int _cameraTextureId;
    private CameraFilter _selectedFilter;
    private int _selectedFilterId = R.id.noFilter;
    private SparseArray<CameraFilter> _cameraFilterMap = new SparseArray<>();
//    String _imagePathToSave;

    private String mFlashMode = Camera.Parameters.FLASH_MODE_AUTO;

    public byte[] PictureData;

    private IOnCameraFinished _cameraListener = null;

    public CameraRenderer(UUID sessionId, Context context) {
        _context = context;
        _sessionId = sessionId;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        _gwidth = -width;
        _gheight = -height;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (_camera != null) {
            _camera.stopPreview();
            _camera.release();
        }
        if (_renderThread != null && _renderThread.isAlive()) {
            _renderThread.interrupt();
        }
        CameraFilter.release();

        return true;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (_renderThread != null && _renderThread.isAlive()) {
            _renderThread.interrupt();
        }
        _renderThread = new Thread(this);

        _surfaceTexture = surface;
        _gwidth = -width;
        _gheight = -height;

        // Open camera
        Pair<Camera.CameraInfo, Integer> backCamera = getBackCamera();
        final int backCameraId = backCamera.second;
        _camera = Camera.open(backCameraId);
        Camera.Parameters p = _camera.getParameters();

        /*** set capture and preview resolution ***/
        p.setPictureFormat(ImageFormat.JPEG);

        List<Camera.Size> supportedPictureSizes = p.getSupportedPictureSizes();
        Camera.Size captureSize = GetCaptureSpecificResolution(supportedPictureSizes);
        p.setPictureSize(captureSize.width, captureSize.height);

        List<Camera.Size> supportedPreviewSizes = p.getSupportedPreviewSizes();
        Camera.Size previewSize = GetAppropriatePreviewResolution(supportedPreviewSizes, captureSize);
        p.setPreviewSize(previewSize.width, previewSize.height);
        /*********** end ***********/

        p.setFlashMode(mFlashMode);
        _camera.setParameters(p);
        _renderThread.start();
    }

    private Camera.Size GetAppropriatePreviewResolution(List<Camera.Size> sizes, Camera.Size captureSize) {
        Camera.Size size = sizes.get(0);
        long product = captureSize.height * captureSize.width;
        long currProductRatio;
        long minProductRatio = Long.MAX_VALUE;
        int captureSizeGcd = Utilities.Gcd(captureSize.height, captureSize.width);
        int captureSizeRatioNumerator = captureSize.height / captureSizeGcd;
        int captureSizeRatioDenominator = captureSize.width / captureSizeGcd;

        for (Camera.Size currSize : sizes) {
            int currSizeGcd = Utilities.Gcd(currSize.height, currSize.width);
            int currSizeRatioNumerator = currSize.height / currSizeGcd;
            int currSizeRatioDenominator = currSize.width / currSizeGcd;

            if(currSizeRatioNumerator == captureSizeRatioNumerator &&
               currSizeRatioDenominator == captureSizeRatioDenominator){
                currProductRatio = Math.abs(product - (currSize.height * currSize.width));
                if(currProductRatio < minProductRatio){
                    size = currSize;
                    minProductRatio = currProductRatio;
                }
            }
        }

        if(minProductRatio == Long.MAX_VALUE){
            BillsLog.Log(_sessionId, LogLevel.Warning, "Preview resolution setted to some random (" + sizes.get(0) +
                            ") value due to failure of GetAppropriatePreviewResolution", LogsDestination.BothUsers, Tag);
            return sizes.get(0);
        }

        return size;
    }

    /***
     * THe following function implemented to replace GetCaptureSpecificResolution due to the following feature:
     * #115 to define and implement standard resolution to use at all phones
     * we are looking for (height = 2448, width = 3264) because it a
     * @param sizes
     * @return
     */
    private Camera.Size GetCaptureSpecificResolution(List<Camera.Size> sizes) {
        Camera.Size size = sizes.get(0);
        for (Camera.Size currSize : sizes) {
            size = currSize.width == 3264 && currSize.height == 2448 ? currSize : size;
        }
        if(size.width != 3264 || size.height != 2448){
            size = GetNearestResolution(sizes);
        }
        return size;
    }

    private Camera.Size GetNearestResolution(List<Camera.Size> sizes) {
        long product = 3264 * 2448;
        Camera.Size size = sizes.get(0);
        long minProductRatio = Long.MAX_VALUE;
        long currProductRatio;

        for (Camera.Size currSize : sizes) {
            currProductRatio = Math.abs(product - (currSize.height * currSize.width));
            size = currProductRatio < minProductRatio ? currSize : size;
        }

        if(minProductRatio == Long.MAX_VALUE){
            BillsLog.Log(_sessionId, LogLevel.Warning, "Capture resolution setted to some random (" + sizes.get(0) +
                    ") value due to failure of GetAppropriatePreviewResolution", LogsDestination.BothUsers, Tag);
            return sizes.get(0);
        }

        return size;
    }

//    private Camera.Size GetMaxCameraResolution(List<Camera.Size> listSize) {
//        Camera.Size maxSize = listSize.get(listSize.size()-1);;
//        for (Camera.Size size : listSize) {
//            maxSize = size.width*size.height > maxSize.width*maxSize.height ? size : maxSize;
//        } //height = 2448, width = 3264
//        return maxSize;
//    }
    
    public void set_selectedFilter(int id)   {
        _selectedFilterId = id;
        _selectedFilter = _cameraFilterMap.get(id);
        if (_selectedFilter != null)
            _selectedFilter.onAttach();
    }

    @Override
    public void run() {
        _camera.startPreview();
        initGL(_surfaceTexture);

        // Setup camera filters map
        _cameraFilterMap.append(R.id.noFilter, new OriginalFilter(_context));
        _cameraFilterMap.append(R.id.barrelBlurFilter, new BarrelBlurFilter(_context));
        _cameraFilterMap.append(R.id.tiltShiftBlurFilter, new TiltShiftBlurFilter(_context));
        set_selectedFilter(_selectedFilterId);

        // Create texture for camera preview
        _cameraTextureId = MyGLUtils.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        _cameraSurfaceTexture = new SurfaceTexture(_cameraTextureId);

        // Start camera preview
        try {
            _camera.setPreviewTexture(_cameraSurfaceTexture);
            _camera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }

        // Render loop
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (_gwidth < 0 && _gheight < 0)
                    GLES20.glViewport(0, 0, _gwidth = -_gwidth, _gheight = -_gheight);

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Update the camera preview texture
                synchronized (this) {
                    _cameraSurfaceTexture.updateTexImage();
                }

                // Draw camera preview
                _selectedFilter.draw(_cameraTextureId, _gwidth, _gheight);

                // Flush
                GLES20.glFlush();
                _egl10.eglSwapBuffers(_eglDisplay, _eglSurface);

                Thread.sleep(DRAW_INTERVAL);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        _cameraSurfaceTexture.release();
        GLES20.glDeleteTextures(1, new int[]{_cameraTextureId}, 0);
    }

    private void initGL(SurfaceTexture texture) {
        _egl10 = (EGL10) EGLContext.getEGL();

        _eglDisplay = _egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (_eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed " +
                    android.opengl.GLUtils.getEGLErrorString(_egl10.eglGetError()));
        }

        int[] version = new int[2];
        if (!_egl10.eglInitialize(_eglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed " +
                    android.opengl.GLUtils.getEGLErrorString(_egl10.eglGetError()));
        }

        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };

        EGLConfig eglConfig = null;
        if (!_egl10.eglChooseConfig(_eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("eglChooseConfig failed " +
                    android.opengl.GLUtils.getEGLErrorString(_egl10.eglGetError()));
        } else if (configsCount[0] > 0) {
            eglConfig = configs[0];
        }
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        _eglContext = _egl10.eglCreateContext(_eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        _eglSurface = _egl10.eglCreateWindowSurface(_eglDisplay, eglConfig, texture, null);

        if (_eglSurface == null || _eglSurface == EGL10.EGL_NO_SURFACE) {
            int error = _egl10.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(Tag, "eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW");
                return;
            }
            throw new RuntimeException("eglCreateWindowSurface failed " +
                    android.opengl.GLUtils.getEGLErrorString(error));
        }

        if (!_egl10.eglMakeCurrent(_eglDisplay, _eglSurface, _eglSurface, _eglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " +
                    android.opengl.GLUtils.getEGLErrorString(_egl10.eglGetError()));
        }
    }

    private Pair<Camera.CameraInfo, Integer> getBackCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        final int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return new Pair<>(cameraInfo, i);
            }
        }
        return null;
    }

    public void setAutoFocus() {
        _camera.autoFocus(null);
    }

    public void takePicture() {
        _camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {
                _camera.takePicture(null, null, mPicture);
            }
        });
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            _camera.stopPreview();
            _cameraListener.OnCameraFinished(data);}
    };

    public void SetOnCameraFinishedListener(IOnCameraFinished listener){
        _cameraListener = listener;
    }

    public void SetFlashMode(String flashMode){
        if(flashMode != Camera.Parameters.FLASH_MODE_AUTO &&
                flashMode != Camera.Parameters.FLASH_MODE_OFF &&
                flashMode != Camera.Parameters.FLASH_MODE_ON){
            throw new InvalidParameterException("Invalid flash mode: " + flashMode);
        }

        mFlashMode = flashMode;
        Camera.Parameters p = _camera.getParameters();
        p.setFlashMode(mFlashMode);
        _camera.setParameters(p);
    }
}