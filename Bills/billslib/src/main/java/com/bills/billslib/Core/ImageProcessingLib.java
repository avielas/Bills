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

    public static Bitmap PreprocessingForTemplateMatcherBitmap(Bitmap image) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        Mat rgba = new Mat();
        Bitmap processed = Bitmap.createBitmap(image);
        Utils.bitmapToMat(processed, rgba);
        AdaptiveThreshold(rgba, 60, 45.0);
        Erode(rgba, 1, 4, StructureElement.VERTICAL_LINE.toString());
        Utils.matToBitmap(rgba, processed);
        rgba.release();
        return processed;
    }

    public static Mat PreprocessingForTemplateMatcherMAT(Mat rgba) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        AdaptiveThreshold(rgba, 60, 45.0);
        Erode(rgba, 1, 4, StructureElement.VERTICAL_LINE.toString());
        return rgba;
    }

    public static Bitmap PreprocessingForParsingBeforeTMBitmap(Bitmap image) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        Mat rgba = new Mat();
        Bitmap processed = Bitmap.createBitmap(image);
        Utils.bitmapToMat(processed, rgba);
        AdaptiveThreshold(rgba, 100, 33.0);
//        Erode(rgba, 1, 4, StructureElement.VERTICAL_LINE.toString());
        Utils.matToBitmap(rgba, processed);
        rgba.release();
        return processed;
    }

    public static Mat PreprocessingForParsingBeforeTMMAT(Mat rgba) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        AdaptiveThreshold(rgba, 100, 33.0);
        return rgba;
    }

    public static Bitmap PreprocessingForParsingBitmap(Bitmap image) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        Mat rgba = new Mat();
        Bitmap processed = Bitmap.createBitmap(image);
        Utils.bitmapToMat(processed, rgba);
//        AdaptiveThreshold(rgba, 100, 33.0);
        Dilate(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Dilate(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Utils.matToBitmap(rgba, processed);
        rgba.release();
        return processed;
    }

    public static Mat PreprocessingForParsingMAT(Mat rgba) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        Dilate(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Dilate(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        return rgba;
    }

    public static void AdaptiveThreshold(Mat rgba, int blockSize, double C){
        /*** convert block size to odd number according to opencv specs ***/
        int blockSizeToOddNumber = blockSize%2 == 0 ? blockSize-1 : blockSize;
        /****************/
        Mat gray = new Mat();
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.adaptiveThreshold(gray, rgba, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, blockSizeToOddNumber, C);
        gray.release();
    }

    public static void Erode(Mat rgba, int iterations, int kernelSize, String structureElementType){
        /**** run dilate which behave as erode because of *********/
        /**** using THRESH_BINARY instead of THRESH_BINARY_INV ****/
        /**** on AdaptiveThreshold function ***********************/
        Mat verticalStructure = GetKernel(structureElementType, kernelSize);
        Imgproc.dilate(rgba, rgba, verticalStructure, new Point(-1,-1), iterations);
        verticalStructure.release();
    }

    public static void Dilate(Mat rgba, int iterations, int kernelSize, String structureElementType){
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
