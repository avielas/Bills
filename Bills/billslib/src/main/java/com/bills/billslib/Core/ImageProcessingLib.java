package com.bills.billslib.Core;

import android.graphics.Bitmap;
import android.util.Log;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.MutableBoolean;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
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
    private static final String Tag = ImageProcessingLib.class.getName();;
    private enum StructureElement {
        NONE,
        HORIZONTAL_LINE,
        VERTICAL_LINE,
        RECTANGULAR,
        ELLIPTICAL,
        CROSS_SHAPED
    }

    public static Mat WarpPerspective(final Mat inputMat, final Point topLeft, final Point topRight,
                                      final Point buttomRight, final Point buttomLeft) throws Exception {
        final Mat[] outputMatToReturn = {null};
        int newWidth = (int) Math.max(buttomRight.x - buttomLeft.x, topRight.x - topLeft.x);
        int newHeight = (int) Math.max(buttomRight.y - topRight.y, buttomLeft.y - topLeft.y);
        int xBegin = (int) Math.min(topLeft.x, buttomLeft.x);
        int yBegin = (int) Math.min(topLeft.y, topRight.y);
        org.opencv.core.Rect roi = new org.opencv.core.Rect(xBegin, yBegin, newWidth, newHeight);
        final Mat resizedMat = new Mat(inputMat, roi);
        topLeft.x = topLeft.x - xBegin;
        topLeft.y = topLeft.y - yBegin;
        topRight.x = topRight.x - xBegin;
        topRight.y = topRight.y - yBegin;
        buttomRight.x = buttomRight.x - xBegin;
        buttomRight.y = buttomRight.y - yBegin;
        buttomLeft.x = buttomLeft.x - xBegin;
        buttomLeft.y = buttomLeft.y - yBegin;

        final MutableBoolean result = new MutableBoolean(false);
        final Double outputImageWidth = Math.max(Math.abs(topLeft.x - topRight.x), Math.abs(buttomLeft.x - buttomRight.x));
        final Double outputImageHeight = Math.max(Math.abs(topLeft.y - buttomLeft.y), Math.abs(topRight.y - buttomRight.y));

        Thread t  = new Thread(new Runnable() {
            @Override
            public void run() {
                Mat startM = null;
                Mat outputMat = null;
                Mat endM = null;
                Mat perspectiveTransform = null;
                try {
                    if (!OpenCVLoader.initDebug()) {
                        // Handle initialization error
                    }

                    int inputImageWidth = resizedMat.width();
                    int inputImageHeight = resizedMat.height();

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
                    Imgproc.warpPerspective(resizedMat, outputMat, perspectiveTransform, new Size(inputImageWidth, inputImageHeight), param);

                    outputMatToReturn[0] = outputMat.clone();
                    result.Set(true);
                }
                catch (Exception ex){
                    Log.d(Tag, "Failed to warp perspective. Error: " + ex.getMessage());
                    result.Set(false);
                } finally {
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
        if(result.Get()) {
            return outputMatToReturn[0];
        }
        throw new Exception("Failed to warp perspective");
    }

    public static void PreprocessingForTM(Mat rgba) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        AdaptiveThreshold(rgba, 60, 10.0);
        Erode(rgba, 1, 5, StructureElement.VERTICAL_LINE.toString());
    }

    public static void PreprocessingForParsing(Mat rgba) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        AdaptiveThreshold(rgba, 100, 33.0);
        Dilate(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Dilate(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
//        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
//        Erode(rgba, 1, 2, StructureElement.RECTANGULAR.toString());
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

    private static void RemoveHorizontalLines(Mat rgbaCopy, Mat rgba) {
        Mat edges = new Mat(rgbaCopy.size(), CvType.CV_8UC1);
        Mat lines = new Mat();
        AdaptiveThreshold(rgbaCopy, 60, 45.0);
        Imgproc.Canny(rgbaCopy, rgbaCopy, 80, 120);
        String pathToSave = Constants.IMAGES_PATH;
        Bitmap newBill = Bitmap.createBitmap(rgbaCopy.width(), rgbaCopy.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbaCopy, newBill);
//        Utilities.SaveToPNGFile(newBill, pathToSave + "/newBill.jpg");
        int threshold = 20;
        int minLineSize = 0;
        int lineGap = 10;
        Imgproc.HoughLinesP(rgbaCopy, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);
        for (int x = 0; x < lines.cols(); x++) {

            double[] vec = lines.get(0, x);
            double[] val = new double[4];

            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];

            System.out.println("Coordinates: x1=>"+x1+" y1=>"+y1+" x2=>"+x2+" y2=>"+y2);
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);

            Imgproc.line(rgba, start, end, new Scalar(0,255, 0, 255), 3);
        }
    }
}
