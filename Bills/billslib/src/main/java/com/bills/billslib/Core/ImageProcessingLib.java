package com.bills.billslib.Core;

import android.graphics.Bitmap;
import android.util.Log;


import com.bills.billslib.Contracts.MutableBoolean;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

/**
 * Created by michaelvalershtein on 18/04/2017.
 */

public class ImageProcessingLib {
    private final String Tag = this.getClass().getSimpleName();

    public boolean WarpPerspective(final Bitmap inputImage, final Bitmap outputImage,
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

    public static Bitmap PreprocessingForTemplateMatcher(Bitmap image) {
        Mat rgba = new Mat();
        //TODO check if image must be immutable. if image is mutable, returned Bitmap is the same object as image`
        Bitmap processed = Bitmap.createBitmap(image);
        Utils.bitmapToMat(processed, rgba);
        AdaptiveThreshold(rgba, 60, 45.0);
        Erode(rgba, 1,3);
        Dilate(rgba, 1,3);
//        Dilate(rgba, 2,3);
//        Erode(rgba, 2,3);
//        Erode(rgba, 1,3);
//        Dilate(rgba, 1,3);
        Utils.matToBitmap(rgba, processed);
        return processed;
    }

    public static Bitmap PreprocessingForParsing(Bitmap image) {
        Mat rgba = new Mat();
        //TODO check if image must be immutable. if image is mutable, returned Bitmap is the same object as image
        Bitmap processed = Bitmap.createBitmap(image);
        Utils.bitmapToMat(processed, rgba);
        AdaptiveThreshold(rgba, 100, 33.0);
//        Erode(rgba, 1,2);
//        Dilate(rgba, 1,2);
//        Erode(rgba, 2,3);
//        Erode(rgba, 1,3);
//        Dilate(rgba, 1,3);
        Utils.matToBitmap(rgba, processed);
        return processed;
    }

    private static List<Point> InitializePerspectiveTransformPoints(Point topLeft, Point topRight, Point buttomRight, Point buttomLeft) {
        List<Point> points = new ArrayList<>();
        points.add(topLeft);
        points.add(topRight);
        points.add(buttomRight);
        points.add(buttomLeft);
        return points;
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

    private static void Erode(Mat rgba, int iterations, int kernelSize){
        /**** run dilate which behave as erode because of *********/
        /**** using THRESH_BINARY instead of THRESH_BINARY_INV ****/
        /**** on AdaptiveThreshold function ***********************/
        Imgproc.dilate(rgba, rgba,
                Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize)),
                new Point(-1,-1), iterations);
    }

    private static void Dilate(Mat rgba, int iterations, int kernelSize){
        /**** run erode which behave as dilate because of *********/
        /**** using THRESH_BINARY instead of THRESH_BINARY_INV ****/
        /**** on AdaptiveThreshold function ***********************/
        Imgproc.erode(rgba, rgba,
                Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize)),
                new Point(-1,-1),
                iterations);
    }

}
