package com.bills.billslib.Core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;


import com.bills.billslib.Contracts.MutableBoolean;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Created by michaelvalershtein on 18/04/2017.
 */

public class ImageProcessingLib {
    private static final String Tag = "ImageProcessingLib";
    private enum StructureElement {
        NONE,
        HORIZONTAL_LINE,
        VERTICAL_LINE,
        RECTANGULAR,
        ELLIPTICAL,
        CROSS_SHAPED
    }

    public static boolean WarpPerspective(final Bitmap inputImage, final Bitmap outputImage,
                                          final Point topLeft, final Point topRight, final Point buttomRight, final Point buttomLeft) {
        final MutableBoolean result = new MutableBoolean(false);
        final Double outputImageWidth = Math.max(Math.abs(topLeft.x - topRight.x), Math.abs(buttomLeft.x - buttomRight.x));
        final Double outputImageHeight = Math.max(Math.abs(topLeft.y - buttomLeft.y), Math.abs(topRight.y - buttomRight.y));



        Thread t  = new Thread(new Runnable() {
            @Override
            public void run() {
                Mat inputMat = null;
                Mat startM = null;
                Mat outputMat = null;
                Mat endM = null;
                Mat perspectiveTransform = null;
                try {
                    if (!OpenCVLoader.initDebug()) {
                        // Handle initialization error
                    }

                    int inputImageWidth = inputImage.getWidth();
                    int inputImageHeight = inputImage.getHeight();

                    inputMat = new Mat(inputImageHeight, inputImageWidth, CvType.CV_8UC4);
                    Utils.bitmapToMat(inputImage, inputMat);


                    List<Point> source = InitializePerspectiveTransformPoints(
                            new Point(topLeft.x, topLeft.y),
                            new Point(topRight.x, topRight.y),
                            new Point(buttomRight.x, buttomRight.y),
                            new Point(buttomLeft.x, buttomLeft.y));
                    startM = Converters.vector_Point2f_to_Mat(source);

                    outputMat = new Mat(outputImageHeight.intValue(), outputImageWidth.intValue(), CvType.CV_8UC4);

                    List<Point> dest = InitializePerspectiveTransformPoints(
                            new Point(0, 0),
                            new Point(outputImageWidth, 0),
                            new Point(outputImageWidth, outputImageHeight),
                            new Point(0, outputImageHeight));

                    endM = Converters.vector_Point2f_to_Mat(dest);

                    perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

                    int param = Imgproc.INTER_AREA;
                    Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(inputImageWidth, inputImageHeight), param);

                    Utils.matToBitmap(outputMat, outputImage);
                    result.Set(true);
                }
                catch (Exception ex){
                    Log.d(Tag, "Failed to warp perspective. Error: " + ex.getMessage());
                    result.Set(false);
                } finally {
                    if (inputMat != null) {
                        inputMat.release();
                    }
                    if (startM != null) {
                        startM.release();
                    }
                    if (outputMat != null) {
                        outputMat.release();
                    }
                    if (endM != null) {
                        endM.release();
                    }
                    if (perspectiveTransform != null) {
                        perspectiveTransform.release();
                    }
                }
            }
        });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result.Get();
    }

//    //TODO: change the function to NOT change source Bitmap
//    public static boolean GetBillCorners(Bitmap source, Point topLeft, Point topRight, Point buttomRight, Point buttomLeft){
//        Mat image = null;
//        Mat newImage = null;
//        Mat contrast = null;
//        Mat destinationImage = null;
//        Mat monoChromeMat = null;
//        MatOfPoint2f approx = null;
//        MatOfPoint2f contour = null;
//        Mat monoChromeMatCopy = null;
//        Mat emptyMat = null;
//        MatOfPoint approxPoint = null;
//
//        Mat mask = null;
//        MatOfInt channels = null;
//        Mat histMat = null;
//        MatOfInt histsize = null;
//        MatOfFloat ranges = null;
//        try {
//            if (!OpenCVLoader.initDebug()) {
//                Log.d(Tag, "Failed to initialize OpenCV.");
//                return false;
//            }
//
//            image  = new Mat(source.getWidth(), source.getHeight(), CvType.CV_8UC4);
//            Utils.bitmapToMat(source, image);
//
//            newImage = new Mat(image.rows(), image.cols(), image.type());
//            Imgproc.cvtColor(image, newImage, Imgproc.COLOR_RGBA2GRAY);
//
//            List<Mat> listOfMat = new ArrayList<Mat>();
//            listOfMat.add(newImage);
//            histMat = new Mat();
//            int histSizeNum = 64;
//            int bucketSize = 256 / histSizeNum;
//            mask = new Mat();
//            channels = new MatOfInt(0);
//            histsize = new MatOfInt(histSizeNum);
//            ranges = new MatOfFloat(0.0f,256.0f);
//            Imgproc.calcHist(listOfMat, channels, mask, histMat, histsize, ranges);
//
//            float[] hist = new float[histSizeNum];
//            histMat.get(0, 0, hist);
//
//            int thresh = GetThresholdIndex(hist, histSizeNum) * bucketSize;
//
//            Imgproc.threshold(newImage, newImage, thresh, 255, Imgproc.THRESH_BINARY);
//
//            Imgproc.dilate(newImage, newImage,
//                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10,10)),
//                    new Point(-1,-1),
//                    2);
//            Imgproc.erode(newImage, newImage,
//                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10,10)),
//                    new Point(-1,-1),
//                    2);
//
//            ArrayList<MatOfPoint> contours = new ArrayList<>();
//            emptyMat = new Mat();
//            Imgproc.findContours(newImage, contours, emptyMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//            final int width = newImage.rows();
//            final int height = newImage.cols();
//            int matArea = width * height;
//
//            double maxContoursArea = Double.MIN_VALUE;
//            for (int i = 0; i < contours.size(); i++) {
//                if(Imgproc.contourArea(contours.get(i)) < matArea * 0.1)
//                {
//                    contours.remove(i);
//                    continue;
//                }
//                if(maxContoursArea < Imgproc.contourArea(contours.get(i))){
//                    maxContoursArea = Imgproc.contourArea(contours.get(i));
//                }
//            }
//            for (int i = 0; i < contours.size(); i++) {
//                double contoursArea = Imgproc.contourArea(contours.get(i));
//                approx = new MatOfPoint2f();
//                contour = new MatOfPoint2f(contours.get(i).toArray());
//                double epsilon = Imgproc.arcLength(contour, true) * 0.1;
//                Imgproc.approxPolyDP(contour, approx, epsilon, true);
//                if (Math.abs(contoursArea) < matArea * 0.01 || Math.abs(contoursArea) > matArea * 0.99) {
//                    approx.release();
//                    contour.release();
//                    continue;
//                }
//                approxPoint = new MatOfPoint(approx.toArray());
//                if (!Imgproc.isContourConvex(approxPoint)) {
//                    approx.release();
//                    contour.release();
//                    approxPoint.release();
//                    continue;
//                }
//                Imgproc.drawContours(newImage, contours, i, new Scalar(0, 255, 0));
//
//                List<Point> points = approx.toList();
//                int pointCount = points.size();
//                LinkedList<Double> cos = new LinkedList<>();
//                for (int j = 2; j < pointCount + 1; j++) {
//                    cos.addLast(angle(points.get(j % pointCount), points.get(j - 2), points.get(j - 1)));
//                }
//
//                double mincos = Double.MAX_VALUE;
//                double maxcos = Double.MIN_VALUE;
//                for (Double val : cos) {
//                    if (mincos > val) {
//                        mincos = val;
//                    }
//                    if (maxcos < val) {
//                        maxcos = val;
//                    }
//                }
//
//                //we are assuming that the bill was shot vertically
//                if (points.size() == 4) {
//                    Collections.sort(points, new Comparator<Point>() {
//                        @Override
//                        public int compare(Point o1, Point o2) {
//                            if (o1.x > o2.x) {
//                                return 1;
//                            } else if (o1.x == o2.x) {
//                                return 0;
//                            } else {
//                                return -1;
//                            }
//                        }
//                    });
//
//                    if (points.get(0).y < points.get(1).y) {
//                        topLeft.x = points.get(0).x;
//                        topLeft.y = points.get(0).y;
//                        buttomLeft.x = points.get(1).x;
//                        buttomLeft.y = points.get(1).y;
//                    } else {
//                        topLeft.x = points.get(1).x;
//                        topLeft.y = points.get(1).y;
//                        buttomLeft.x = points.get(0).x;
//                        buttomLeft.y = points.get(0).y;
//                    }
//
//                    if (points.get(2).y < points.get(3).y) {
//                        topRight.x = points.get(2).x;
//                        topRight.y = points.get(2).y;
//                        buttomRight.x = points.get(3).x;
//                        buttomRight.y = points.get(3).y;
//                    } else {
//                        topRight.x = points.get(3).x;
//                        topRight.y = points.get(3).y;
//                        buttomRight.x = points.get(2).x;
//                        buttomRight.y = points.get(2).y;
//                    }
//                }
//                return true;
//            }
//        }
//        catch (Exception ex)
//        {
//            ex.printStackTrace();
//        }
//        finally {
//            if(image != null) {
//                image.release();
//            }
//            if(newImage != null) {
//                newImage.release();
//            }
//            if(contrast != null) {
//                contrast.release();
//            }
//            if(destinationImage != null) {
//                destinationImage.release();
//            }
//            if(monoChromeMat != null) {
//                monoChromeMat.release();
//            }
//            if(approx != null) {
//                approx.release();
//            }
//            if(contour != null) {
//                contour.release();
//            }
//            if(monoChromeMatCopy != null) {
//                monoChromeMatCopy.release();
//            }
//            if(emptyMat != null) {
//                emptyMat.release();
//            }
//            if(approxPoint != null){
//                approxPoint.release();
//            }
//        }
//        Log.d(Tag, "Failed to get bounding rectangle.");
//        return false;
//    }
//

    public static Bitmap PreprocessingForTemplateMatcher(Bitmap image) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        Mat rgba = new Mat();
        Bitmap processed = Bitmap.createBitmap(image);
        Utils.bitmapToMat(processed, rgba);
        AdaptiveThreshold(rgba, 60, 45.0);
//        Erode(rgba, 1, 4, StructureElement.VERTICAL_LINE.toString());
        Utils.matToBitmap(rgba, processed);
        return processed;
    }

    public static Bitmap PreprocessingForParsingBeforeTM(Bitmap image) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        Mat rgba = new Mat();
        Bitmap processed = Bitmap.createBitmap(image);
        Utils.bitmapToMat(processed, rgba);
        AdaptiveThreshold(rgba, 100, 33.0);
//        Erode(rgba, 1, 4, StructureElement.VERTICAL_LINE.toString());
        Utils.matToBitmap(rgba, processed);
        return processed;
    }

    public static Bitmap PreprocessingForParsing(Bitmap image) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        Mat rgba = new Mat();
        Bitmap processed = Bitmap.createBitmap(image);
        Utils.bitmapToMat(processed, rgba);
//        AdaptiveThreshold(rgba, 100, 33.0);
        Utils.matToBitmap(rgba, processed);
        return processed;
    }

    private static void AdaptiveThreshold(Mat rgba, int blockSize, double C){
        /*** convert block size to odd number according to opencv specs ***/
        int blockSizeToOddNumber = blockSize%2 == 0 ? blockSize-1 : blockSize;
        /****************/
        Mat gray = new Mat();
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.adaptiveThreshold(gray, rgba, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, blockSizeToOddNumber, C);
        gray.release();
    }

    private static void Erode(Mat rgba, int iterations, int kernelSize, String structureElementType){
        /**** run dilate which behave as erode because of *********/
        /**** using THRESH_BINARY instead of THRESH_BINARY_INV ****/
        /**** on AdaptiveThreshold function ***********************/
        Mat verticalStructure = GetKernel(structureElementType, kernelSize);
        Imgproc.dilate(rgba, rgba, verticalStructure, new Point(-1,-1), iterations);
        verticalStructure.release();
    }

    private static void Dilate(Mat rgba, int iterations, int kernelSize, String structureElementType){
        /**** run erode which behave as dilate because of *********/
        /**** using THRESH_BINARY instead of THRESH_BINARY_INV ****/
        /**** on AdaptiveThreshold function ***********************/
        Mat horizontalStructure = GetKernel(structureElementType, kernelSize);
        Imgproc.erode(rgba, rgba, horizontalStructure, new Point(-1,-1), iterations);
        horizontalStructure.release();
    }

    private static Mat GetKernel(String structureElementType, int kernelSize) {
        switch(structureElementType)
        {
            case "HORIZONTAL_LINE":
                return Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, 1));
            case "VERTICAL_LINE":
                return Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, kernelSize));
            case "RECTANGULAR":
                return Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
            case "ELLIPTICAL":
                return Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(kernelSize, kernelSize));
            case "CROSS_SHAPED":
                return Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(kernelSize, kernelSize));
            default:
                //RECTANGULAR;
                return Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
        }
    }

    private static List<Point> InitializePerspectiveTransformPoints(Point topLeft, Point topRight, Point buttomRight, Point buttomLeft) {
        List<Point> points = new ArrayList<>();
        points.add(topLeft);
        points.add(topRight);
        points.add(buttomRight);
        points.add(buttomLeft);
        return points;
    }
}
