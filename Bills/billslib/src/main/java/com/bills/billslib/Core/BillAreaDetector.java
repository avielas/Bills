package com.bills.billslib.Core;

import android.app.Activity;
import android.graphics.Bitmap;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Contracts.Enums.LogsDestination;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by michaelvalershtein on 17/04/2017.
 */

public class BillAreaDetector {
    private String Tag = BillAreaDetector.class.getName();

    private static int _histSizeNum = 64;
    private static int _bucketSize = 256 / _histSizeNum;
    private UUID _sessionId;

    private Activity mActivity;
    private ImageClassifier mClassifier = null;
    public BillAreaDetector(final UUID sessionId, Activity activity) {
        _sessionId = sessionId;
        try {
            mActivity = activity;
            mClassifier = new ImageClassifierFloatHED(activity);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public boolean GetBillCorners(final Mat source, Point topRight, Point buttomRight, Point buttomLeft, Point topLeft) {
        Mat newImage = null;
        Mat contrast = null;
        Mat destinationImage = null;
        Mat monoChromeMat = null;
        MatOfPoint2f approx = null;
        MatOfPoint2f contour = null;
        Mat monoChromeMatCopy = null;
        Mat emptyMat = null;
        MatOfPoint approxPoint = null;

        Mat mask = null;
        MatOfInt channels = null;
        Mat histMat = null;
        MatOfInt histsize = null;
        MatOfFloat ranges = null;

        try {
            if (!OpenCVLoader.initDebug()) {
                Log.d(Tag, "Failed to initialize OpenCV.");
                return false;
            }

            Bitmap bmp = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(source, bmp);

            Bitmap resizedBmp = Bitmap.createScaledBitmap(bmp, 480, 480, false);
            bmp.recycle();

            if(mClassifier == null){
                return false;
            }

            SpannableStringBuilder textToShow = new SpannableStringBuilder();
            mClassifier.classifyFrame(resizedBmp, textToShow);

            float[][][][] output = mClassifier.getArray(3);
            int[] convertedOutput = new int[480*480];

            float min = 9999999;
            float max = 0;
            for(int i=0; i<480; ++i){
                for(int j=0; j<480; ++j){
                    output[0][i][j][0] = 1 - output[0][i][j][0];
                    if(output[0][i][j][0] > max){
                        max = output[0][i][j][0];
                    }

                    if(output[0][i][j][0] < min){
                        min = output[0][i][j][0];
                    }
                }
            }

            float delta = max - min;
            float binSize = delta / 255;


            for(int i=0; i<480; ++i){
                for(int j=0; j<480; ++j){
                    int valueToReplicate = ((Float)((output[0][i][j][0] - min) / binSize)).intValue();

                    convertedOutput[480*i+j] = 0xff000000|(valueToReplicate<<16)| (valueToReplicate <<8) | valueToReplicate;
                }
            }

/*
            //uncomment for debugging
            bmp = Bitmap.createBitmap(convertedOutput, 480, 480, Bitmap.Config.ARGB_8888);
            final Bitmap tmpBmp = bmp;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String STORAGE_DIRECTORY = Environment.getExternalStorageDirectory().toString();
                    String IMAGES_PATH = STORAGE_DIRECTORY + "/TesseractSample/imgs";

                    String billPath = IMAGES_PATH + "/originalProcessed.jpg";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mActivity.requestPermissions( new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                    }

                    Utilities.SaveToPNGFile(_sessionId, tmpBmp, billPath);
                    tmpBmp.recycle();

                }
            });
*/

            final Bitmap newBmp = Bitmap.createScaledBitmap(bmp, source.width(), source.height(), false);
            Utils.bitmapToMat(newBmp, source);
/*

            //uncomment for debugging
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String STORAGE_DIRECTORY = Environment.getExternalStorageDirectory().toString();
                    String IMAGES_PATH = STORAGE_DIRECTORY + "/TesseractSample/imgs";

                    String billPath = IMAGES_PATH + "/resizedProcessed.jpg";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mActivity.requestPermissions( new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                    }

                    Utilities.SaveToPNGFile(_sessionId, newBmp, billPath);

                }
            });
*/

            newImage = source.clone();
            Imgproc.cvtColor(newImage, newImage, Imgproc.COLOR_RGBA2GRAY);

            List<Mat> listOfMat = new ArrayList<Mat>();
            listOfMat.add(newImage);
            histMat = new Mat();

            mask = new Mat();
            channels = new MatOfInt(0);
            histsize = new MatOfInt(_histSizeNum);
            ranges = new MatOfFloat(0.0f, 256.0f);
            Imgproc.calcHist(listOfMat, channels, mask, histMat, histsize, ranges);

            float[] hist = new float[_histSizeNum];
            histMat.get(0, 0, hist);

            int thresh = GetThresholdIndex(hist, _histSizeNum) * _bucketSize;
            //optimal threshold for post-HED images.
            thresh = 70;

            Log.d(Tag, "Threshold for area detection: " + thresh);

            Size kernelSize = new Size(30, 30);

            Imgproc.erode(newImage, newImage,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize),
                    new Point(-1, -1),
                    4);

            Imgproc.dilate(newImage, newImage,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize),
                    new Point(-1, -1),
                    4);

            Imgproc.threshold(newImage, newImage, thresh, 255, Imgproc.THRESH_BINARY);

            Imgproc.erode(newImage, newImage,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize),
                    new Point(-1, -1),
                    2);

            Imgproc.dilate(newImage, newImage,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, kernelSize),
                    new Point(-1, -1),
                    2);

            Utils.matToBitmap(newImage, newBmp);
/*

            //uncomment for debugging
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String STORAGE_DIRECTORY = Environment.getExternalStorageDirectory().toString();
                    String IMAGES_PATH = STORAGE_DIRECTORY + "/TesseractSample/imgs";

                    String billPath = IMAGES_PATH + "/originalProcessedBinary.jpg";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mActivity.requestPermissions( new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                    }

                    Utilities.SaveToPNGFile(_sessionId, newBmp, billPath);

                }
            });
*/

            ArrayList<MatOfPoint> contours = new ArrayList<>();
            emptyMat = new Mat();
            Imgproc.findContours(newImage, contours, emptyMat, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            final int width = newImage.rows();
            final int height = newImage.cols();
            int matArea = width * height;

            double maxContoursArea = Double.MIN_VALUE;
            for (int i = 0; i < contours.size(); i++) {
                if (Imgproc.contourArea(contours.get(i)) < matArea * 0.1) {
                    contours.remove(i);
                    continue;
                }
                if (maxContoursArea < Imgproc.contourArea(contours.get(i))) {
                    maxContoursArea = Imgproc.contourArea(contours.get(i));
                }
            }
            for (int i = 0; i < contours.size(); i++) {
                double contoursArea = Imgproc.contourArea(contours.get(i));
                approx = new MatOfPoint2f();
                contour = new MatOfPoint2f(contours.get(i).toArray());
                double epsilon = Imgproc.arcLength(contour, true) * 0.1;
                Imgproc.approxPolyDP(contour, approx, epsilon, true);
                if (Math.abs(contoursArea) < matArea * 0.01 || Math.abs(contoursArea) > matArea * 0.95) {
                    approx.release();
                    contour.release();
                    continue;
                }
                approxPoint = new MatOfPoint(approx.toArray());
                if (!Imgproc.isContourConvex(approxPoint)) {
                    approx.release();
                    contour.release();
                    approxPoint.release();
                    continue;
                }
                Imgproc.drawContours(newImage, contours, i, new Scalar(0, 255, 0));

                List<Point> points = approx.toList();
                int pointCount = points.size();
                LinkedList<Double> cos = new LinkedList<>();
                for (int j = 2; j < pointCount + 1; j++) {
                    cos.addLast(angle(points.get(j % pointCount), points.get(j - 2), points.get(j - 1)));
                }

                double mincos = Double.MAX_VALUE;
                double maxcos = Double.MIN_VALUE;
                for (Double val : cos) {
                    if (mincos > val) {
                        mincos = val;
                    }
                    if (maxcos < val) {
                        maxcos = val;
                    }
                }

                //we are assuming that the bill was shot vertically
                if (points.size() == 4) {
                    Collections.sort(points, new Comparator<Point>() {
                        @Override
                        public int compare(Point o1, Point o2) {
                            if (o1.x > o2.x) {
                                return 1;
                            } else if (o1.x == o2.x) {
                                return 0;
                            } else {
                                return -1;
                            }
                        }
                    });

                    if (points.get(0).y < points.get(1).y) {
                        topLeft.x = points.get(0).x;
                        topLeft.y = points.get(0).y;
                        buttomLeft.x = points.get(1).x;
                        buttomLeft.y = points.get(1).y;
                    } else {
                        topLeft.x = points.get(1).x;
                        topLeft.y = points.get(1).y;
                        buttomLeft.x = points.get(0).x;
                        buttomLeft.y = points.get(0).y;
                    }

                    if (points.get(2).y < points.get(3).y) {
                        topRight.x = points.get(2).x;
                        topRight.y = points.get(2).y;
                        buttomRight.x = points.get(3).x;
                        buttomRight.y = points.get(3).y;
                    } else {
                        topRight.x = points.get(3).x;
                        topRight.y = points.get(3).y;
                        buttomRight.x = points.get(2).x;
                        buttomRight.y = points.get(2).y;
                    }
                }
                BillsLog.Log(_sessionId, LogLevel.Info, "Got bill corners: " +
                        "Top Left: " + topLeft +
                        "; Top Right: " + topRight +
                        "; Buttom Right: " + buttomRight +
                        "; Buttom Left: " + buttomLeft, LogsDestination.BothUsers, Tag);
                return true;
            }
        } catch (Exception e) {
            String logMessage = "An exception occurred while trying to find bill corners. \nStackTrace: " + e.getStackTrace() + "\nException Message: " + e.getMessage();
            BillsLog.Log(_sessionId, LogLevel.Error, logMessage, LogsDestination.BothUsers, Tag);
        } finally {
            if (newImage != null) {
                newImage.release();
            }
            if (contrast != null) {
                contrast.release();
            }
            if (destinationImage != null) {
                destinationImage.release();
            }
            if (monoChromeMat != null) {
                monoChromeMat.release();
            }
            if (approx != null) {
                approx.release();
            }
            if (contour != null) {
                contour.release();
            }
            if (monoChromeMatCopy != null) {
                monoChromeMatCopy.release();
            }
            if (emptyMat != null) {
                emptyMat.release();
            }
            if (approxPoint != null) {
                approxPoint.release();
            }
        }
        return false;
    }

    private int GetThresholdIndex(float[] hist, int histSizeNum) {
        float maxValue = Float.MIN_VALUE;
        int max1Index = Integer.MIN_VALUE;
        int max2Index = Integer.MIN_VALUE;
        String histToPrint = "";

        //find largest peak
        for (int h = 0; h < histSizeNum; h++) {
            if (maxValue < hist[h]) {
                maxValue = hist[h];
                max1Index = h;
            }
            histToPrint += hist[h] + ";";
        }

        Log.d(Tag, histToPrint);

        maxValue = Float.MIN_VALUE;
        //find second largest peak
        for (int h = 1; h < histSizeNum - 1; h++) {
            if (maxValue < hist[h] &&
                    hist[h] >= hist[h - 1] &&
                    hist[h] >= hist[h + 1] &&
                    h != max1Index) {
                maxValue = hist[h];
                max2Index = h;
            }
        }

        int minIndex = Integer.MAX_VALUE;
        float minValue = Float.MAX_VALUE;
        //find lowest point between the two peaks
        for (int h = max1Index < max2Index ? max1Index : max2Index;
             h < (max1Index > max2Index ? max1Index : max2Index);
             h++) {
            if (minValue > hist[h]) {
                minValue = hist[h];
                minIndex = h;
            }
        }
        return 70;
    }

    private static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    private static void RemoveGlare(Mat grayscaleMat, int threshold) {
        Mat resizedImageMat = new Mat();
        Size newSize = new Size(grayscaleMat.width() / 4, grayscaleMat.height() / 4);
        Imgproc.resize(grayscaleMat, resizedImageMat, newSize);

        Mat binaryGlareVerticalKernelImageMat = new Mat();
        Mat binaryGlareHorizontalKernelImageMat = new Mat();


        Mat dilateHorizontalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(resizedImageMat.width() / 2, 5));
        Mat erodeHorizontalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

        Mat dilateVerticalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, resizedImageMat.height() / 2));
        Mat erodeVerticalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

        Mat verticalMask = new Mat(resizedImageMat.rows(), resizedImageMat.cols(), binaryGlareHorizontalKernelImageMat.type(), Scalar.all(255));
        Mat horizontalMask = new Mat(resizedImageMat.rows(), resizedImageMat.cols(), binaryGlareHorizontalKernelImageMat.type(), Scalar.all(255));

        Mat binaryGlareImageMat = new Mat();

        try {
            Imgproc.threshold(resizedImageMat, binaryGlareVerticalKernelImageMat, 229, 255, Imgproc.THRESH_BINARY);
            Imgproc.threshold(resizedImageMat, binaryGlareHorizontalKernelImageMat, 229, 255, Imgproc.THRESH_BINARY);

            //create masks for horizontal and vertical kernels for the glare image erosion and dilation
            List<MatOfPoint> horizontalBounds = new ArrayList<>();
            List<MatOfPoint> verticalBounds = new ArrayList<>();
            Point topLeftPoint = new Point(0, 0);
            Point topRightPoint = new Point(resizedImageMat.width() - 1, 0);
            Point buttomLeftPoint = new Point(0, resizedImageMat.height());
            Point buttomRightPoint = new Point(resizedImageMat.width(), resizedImageMat.height());
            Point center1 = new Point(resizedImageMat.width() / 3, resizedImageMat.height() / 3);
            Point center2 = new Point(resizedImageMat.width() / 3 * 2, resizedImageMat.height() / 3);
            Point center3 = new Point(resizedImageMat.width() / 3 * 2, resizedImageMat.height() / 3 * 2);
            Point center4 = new Point(resizedImageMat.width() / 3, resizedImageMat.height() / 3 * 2);

            //create bounds for horizontal and vertical kernel masks
            verticalBounds.add(new MatOfPoint(topLeftPoint, topRightPoint, center1, center2));
            verticalBounds.add(new MatOfPoint(center4, center3, buttomRightPoint, buttomLeftPoint));

            Imgproc.fillPoly(verticalMask, verticalBounds, new Scalar(0));
            Imgproc.threshold(verticalMask, verticalMask, 100, 255, Imgproc.THRESH_BINARY);

            horizontalBounds.add(new MatOfPoint(topLeftPoint, center1, center4, buttomLeftPoint));
            horizontalBounds.add(new MatOfPoint(topRightPoint, buttomRightPoint, center3, center2));

            Imgproc.fillPoly(horizontalMask, horizontalBounds, new Scalar(0));
            Imgproc.threshold(horizontalMask, horizontalMask, 100, 255, Imgproc.THRESH_BINARY);

            //get vertical and horizontal kernel glare images
            Core.bitwise_and(horizontalMask, binaryGlareHorizontalKernelImageMat, binaryGlareHorizontalKernelImageMat);
            Core.bitwise_and(verticalMask, binaryGlareVerticalKernelImageMat, binaryGlareVerticalKernelImageMat);

            Imgproc.erode(binaryGlareVerticalKernelImageMat, binaryGlareVerticalKernelImageMat, erodeVerticalKernel, new Point(-1, -1), 3);
            Imgproc.dilate(binaryGlareVerticalKernelImageMat, binaryGlareVerticalKernelImageMat, dilateVerticalKernel, new Point(-1, -1), 3);

            Imgproc.erode(binaryGlareHorizontalKernelImageMat, binaryGlareHorizontalKernelImageMat, erodeHorizontalKernel, new Point(-1, -1), 3);
            Imgproc.dilate(binaryGlareHorizontalKernelImageMat, binaryGlareHorizontalKernelImageMat, dilateHorizontalKernel, new Point(-1, -1), 3);

            Core.add(binaryGlareHorizontalKernelImageMat, binaryGlareVerticalKernelImageMat, binaryGlareImageMat);

            Imgproc.erode(binaryGlareImageMat, binaryGlareImageMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10)), new Point(-1, -1), 1);
            Imgproc.dilate(binaryGlareImageMat, binaryGlareImageMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10)), new Point(-1, -1), 3);

            Imgproc.erode(binaryGlareImageMat, binaryGlareImageMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)), new Point(-1, -1), 1);
            Imgproc.dilate(binaryGlareImageMat, binaryGlareImageMat, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)), new Point(-1, -1), 3);

            Imgproc.threshold(resizedImageMat, resizedImageMat, threshold, 255, Imgproc.THRESH_BINARY);

            Core.subtract(resizedImageMat, binaryGlareImageMat, resizedImageMat);

            Imgproc.resize(resizedImageMat, grayscaleMat, new Size(grayscaleMat.width(), grayscaleMat.height()));

        } catch (Exception ex) {
            throw ex;
        } finally {
            binaryGlareVerticalKernelImageMat.release();
            binaryGlareHorizontalKernelImageMat.release();
            dilateHorizontalKernel.release();
            erodeHorizontalKernel.release();
            dilateVerticalKernel.release();
            erodeVerticalKernel.release();
            verticalMask.release();
            horizontalMask.release();
            binaryGlareImageMat.release();
        }
    }
}
