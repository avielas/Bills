package com.bills.deleteme;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.bills.billslib.Core.BillAreaDetector;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

//TODO: sync database and storage timestamps
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = (ImageView)findViewById(R.id.imageView);

        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.image2);

        Point topLeft = new Point();
        Point topRight = new Point();
        Point buttomRight = new Point();
        Point buttomLeft = new Point();

        if(!OpenCVLoader.initDebug()){
            Toast.makeText(this, "RRRRRRRRR", Toast.LENGTH_SHORT).show();
        }

//        Mat imageMat = new Mat();
//        Utils.bitmapToMat(image, imageMat);
//        if(!BillAreaDetector.GetBillCorners(imageMat, topLeft, topRight, buttomRight, buttomLeft)){
//            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
//        }
//        Toast.makeText(this, "Succeed", Toast.LENGTH_LONG).show();

        if(!OpenCVLoader.initDebug()){
            Log.d("","");
        }

        Mat imageMat = new Mat();
        Utils.bitmapToMat(image, imageMat);
        Mat grayscaleMat = new Mat(imageMat.rows(), imageMat.cols(), imageMat.type());

        Imgproc.cvtColor(imageMat, grayscaleMat, Imgproc.COLOR_RGBA2GRAY);

        Mat binaryGlareVerticalKernelImageMat = new Mat();
        Mat binaryGlareHorizontalKernelImageMat = new Mat();

        Imgproc.threshold(grayscaleMat, binaryGlareVerticalKernelImageMat, 229, 255, Imgproc.THRESH_BINARY);
        Imgproc.threshold(grayscaleMat, binaryGlareHorizontalKernelImageMat, 229, 255, Imgproc.THRESH_BINARY);

        Mat dilateHorizontalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(image.getWidth()/3, 10));
        Mat erodeHorizontalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

        Mat dilateVerticalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, image.getWidth()/3));
        Mat erodeVerticalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));

        //create masks for horizontal and vertical kernels for the glare image erosion and dilation
        List<MatOfPoint> horizontalBounds = new ArrayList<>();
        List<MatOfPoint> verticalBounds = new ArrayList<>();
        Point topLeftPoint = new Point(0,0);
        Point topRightPoint = new Point(imageMat.width()-1,0);
        Point buttomLeftPoint = new Point(0,imageMat.height());
        Point buttomRightPoint = new Point(imageMat.width(), imageMat.height());
        Point center = new Point(imageMat.width()/2, imageMat.height()/2);

        //create bounds for horizontal and vertical kernel masks
        verticalBounds.add(new MatOfPoint(topLeftPoint, topRightPoint, center));
        verticalBounds.add(new MatOfPoint(center, buttomRightPoint, buttomLeftPoint));

        Mat verticalMask = new Mat(imageMat.rows(), imageMat.cols(), binaryGlareHorizontalKernelImageMat.type(), Scalar.all(255));
        Imgproc.fillPoly(verticalMask, verticalBounds, new Scalar(0));
        Imgproc.threshold(verticalMask, verticalMask, 100, 255, Imgproc.THRESH_BINARY);

        horizontalBounds.add(new MatOfPoint(topLeftPoint, center, buttomLeftPoint));
        horizontalBounds.add(new MatOfPoint(topRightPoint, buttomRightPoint, center));

        Mat horizontalMask = new Mat(imageMat.rows(), imageMat.cols(), binaryGlareHorizontalKernelImageMat.type(), Scalar.all(255));
        Imgproc.fillPoly(horizontalMask, horizontalBounds, new Scalar(0));
        Imgproc.threshold(horizontalMask, horizontalMask, 100, 255, Imgproc.THRESH_BINARY);

        //get vertical and horizontal kernel glare images
        Core.bitwise_and(horizontalMask, binaryGlareHorizontalKernelImageMat, binaryGlareHorizontalKernelImageMat);
        Core.bitwise_and(verticalMask, binaryGlareVerticalKernelImageMat, binaryGlareVerticalKernelImageMat);



        Imgproc.erode(binaryGlareVerticalKernelImageMat, binaryGlareVerticalKernelImageMat, erodeVerticalKernel, new Point(-1,-1), 3);
        Imgproc.dilate(binaryGlareVerticalKernelImageMat, binaryGlareVerticalKernelImageMat, dilateVerticalKernel, new Point(-1,-1), 3);

        Imgproc.erode(binaryGlareHorizontalKernelImageMat, binaryGlareHorizontalKernelImageMat, erodeHorizontalKernel, new Point(-1, -1), 3);
        Imgproc.dilate(binaryGlareHorizontalKernelImageMat, binaryGlareHorizontalKernelImageMat, dilateHorizontalKernel, new Point(-1, -1), 3);

        Mat binaryGlareImageMat = new Mat();
        Core.add(binaryGlareHorizontalKernelImageMat, binaryGlareVerticalKernelImageMat, binaryGlareImageMat);

        Mat binaryRegularImageMat = new Mat();
        Imgproc.threshold(grayscaleMat, binaryRegularImageMat, 156, 255, Imgproc.THRESH_BINARY);

        Core.subtract(binaryRegularImageMat, binaryGlareImageMat, binaryRegularImageMat);

        Utils.matToBitmap(binaryRegularImageMat, image);

        if(!BillAreaDetector.GetBillCorners(imageMat, topLeft, topRight, buttomRight, buttomLeft)){
            Log.d("", "Failed");
        }else {
            Log.d("", "Succeed");
        }
        imageView.setImageBitmap(image);
    }
}
