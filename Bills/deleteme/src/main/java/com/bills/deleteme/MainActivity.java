package com.bills.deleteme;

import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    LinearLayout mainLayout = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = (LinearLayout)findViewById(R.id.mainLayout);



    }

    private String Tag = "BillAreaDetector";

    private  int _histSizeNum = 64;
    private  int _bucketSize = 256 / _histSizeNum;

    public boolean GetBillCorners(Bitmap source, Point topLeft, Point topRight, Point buttomRight, Point buttomLeft){
        Mat image = null;
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

            image  = new Mat(source.getWidth(), source.getHeight(), CvType.CV_8UC4);
            Utils.bitmapToMat(source, image);

            newImage = new Mat(image.rows(), image.cols(), image.type());
            Imgproc.cvtColor(image, newImage, Imgproc.COLOR_RGBA2GRAY);

            List<Mat> listOfMat = new ArrayList<Mat>();
            listOfMat.add(newImage);
            histMat = new Mat();

            mask = new Mat();
            channels = new MatOfInt(0);
            histsize = new MatOfInt(_histSizeNum);
            ranges = new MatOfFloat(0.0f,256.0f);
            Imgproc.calcHist(listOfMat, channels, mask, histMat, histsize, ranges);

            float[] hist = new float[_histSizeNum];
            histMat.get(0, 0, hist);

            int thresh = GetThresholdIndex(hist, _histSizeNum) * _bucketSize;

            Log.d(Tag, "Threshold for area detection: " + thresh);
            Imgproc.threshold(newImage, newImage, thresh, 255, Imgproc.THRESH_BINARY);

            ImageView imageView = new ImageView(this);
            Bitmap bitmap = Bitmap.createBitmap(source);
            Utils.matToBitmap(newImage, bitmap);
            imageView.setImageBitmap(bitmap);

            Imgproc.dilate(newImage, newImage,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10,10)),
                    new Point(-1,-1),
                    2);
            Imgproc.erode(newImage, newImage,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10,10)),
                    new Point(-1,-1),
                    2);

            ArrayList<MatOfPoint> contours = new ArrayList<>();
            emptyMat = new Mat();
            Imgproc.findContours(newImage, contours, emptyMat, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            final int width = newImage.rows();
            final int height = newImage.cols();
            int matArea = width * height;

            double maxContoursArea = Double.MIN_VALUE;
            for (int i = 0; i < contours.size(); i++) {
                if(Imgproc.contourArea(contours.get(i)) < matArea * 0.1)
                {
                    contours.remove(i);
                    continue;
                }
                if(maxContoursArea < Imgproc.contourArea(contours.get(i))){
                    maxContoursArea = Imgproc.contourArea(contours.get(i));
                }
            }
            for (int i = 0; i < contours.size(); i++) {
                double contoursArea = Imgproc.contourArea(contours.get(i));
                approx = new MatOfPoint2f();
                contour = new MatOfPoint2f(contours.get(i).toArray());
                double epsilon = Imgproc.arcLength(contour, true) * 0.1;
                Imgproc.approxPolyDP(contour, approx, epsilon, true);
                if (Math.abs(contoursArea) < matArea * 0.01 || Math.abs(contoursArea) > matArea * 0.99) {
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
                return true;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            Log.d(Tag, "An exception accured while trying to find bill corners. Error: " + ex.getMessage());
        }
        finally {
            if(image != null) {
                image.release();
            }
            if(newImage != null) {
                newImage.release();
            }
            if(contrast != null) {
                contrast.release();
            }
            if(destinationImage != null) {
                destinationImage.release();
            }
            if(monoChromeMat != null) {
                monoChromeMat.release();
            }
            if(approx != null) {
                approx.release();
            }
            if(contour != null) {
                contour.release();
            }
            if(monoChromeMatCopy != null) {
                monoChromeMatCopy.release();
            }
            if(emptyMat != null) {
                emptyMat.release();
            }
            if(approxPoint != null){
                approxPoint.release();
            }
        }
        return false;

    }

    private int GetThresholdIndex(float[] hist, int histSizeNum) {
        float maxValue = Float.MIN_VALUE;
        int max1Index = Integer.MIN_VALUE;
        int max2Index = Integer.MIN_VALUE;
        //find largest peak
        for(int h=0; h<histSizeNum; h++) {
            if(maxValue < hist[h]){
                maxValue = hist[h];
                max1Index = h;
            }
            Log.d(Tag, "" + hist[h]);
        }

        maxValue = Float.MIN_VALUE;
        //find second largest peak
        for(int h=1; h<histSizeNum - 1; h++) {
            if(maxValue < hist[h] &&
                    hist[h] >= hist[h - 1]&&
                    hist[h] >= hist[h + 1]&&
                    h!= max1Index){
                maxValue = hist[h];
                max2Index = h;
            }
        }

        int minIndex = Integer.MAX_VALUE;
        float minValue = Float.MAX_VALUE;
        //find lowest point between the two peaks
        for(int h = max1Index < max2Index ? max1Index : max2Index;
            h < (max1Index > max2Index ? max1Index : max2Index);
            h++) {
            if(minValue > hist[h]){
                minValue = hist[h];
                minIndex = h;
            }
        }
        return minIndex;
    }

    private double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }
}
