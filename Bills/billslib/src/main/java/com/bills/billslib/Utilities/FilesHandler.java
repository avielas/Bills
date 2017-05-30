package com.bills.billslib.Utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.ImageProcessingLib;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by avielavr on 5/8/2017.
 */

public class FilesHandler {
    private static String Tag = "FilesHandler";

    public static boolean SaveToJPGFile(Bitmap bmp, String path){
        FileOutputStream out = null;
        try {
            File file = new File(path);
            if(file.exists()){
                file.delete();
            }
            out = new FileOutputStream(path);

            // bmp is your Bitmap instance, PNG is a lossless format, the compression factor (100) is ignored
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static boolean SaveToTXTFile(byte[] image, String path){
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path));
            bos.write(image);
            bos.flush();
            bos.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }

    public static byte[] BitmapToByteArray(Bitmap bitmap) throws IOException {
        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        try {
            bitmap.copyPixelsToBuffer(byteBuffer);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return byteBuffer.array();
    }

    public static Bitmap ByteArrayToBitmap(byte[] bytes){
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = null;
        try{
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bitmapOptions);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static byte[] ImageTxtFile2ByteArray(String path) throws IOException {
        File file = new File(path);
        int size = (int) file.length();
        byte[] image = new byte[size];
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        bis.read(image);
        bis.close();
        return image;
    }

    public static Bitmap Rotating(Bitmap image) {
        //rotating bitmap due to Samsung camera bug
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotated = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
        image.recycle();
        return rotated;
    }

    public static Mat BytesToMat(byte[] bytes) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        return Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
    }

    /**
     *
     * @param fileFullName txt file full name on device
     * @return list of string with file lines
     * @throws IOException
     */
    public static List<String> ReadTxtFile(String fileFullName) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileFullName));
        // do reading, usually loop until end of file reading
        List<String> lines = new ArrayList<>();
        String line = bufferedReader.readLine();
        while (line != null) {
            lines.add(line);
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        return lines;
    }

    public static void RotateClockwise90(Mat src, Mat dest) {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        Core.flip(src.t(), dest, 1);
    }

    public static Bitmap GetRotatedBill(String billFullName) throws IOException {
        byte[] bytes = ImageTxtFile2ByteArray(billFullName);
        Bitmap bitmap = ByteArrayToBitmap(bytes);
        bitmap = FilesHandler.Rotating(bitmap);
        return bitmap;
    }

//    public static Bitmap GetWarpedBill(String billFullName) throws IOException {
//        Bitmap bitmap = GetRotatedBill(billFullName);
//        BillAreaDetector areaDetector = new BillAreaDetector();
//        Point mTopLeft = new Point();
//        Point mTopRight = new Point();
//        Point mButtomLeft = new Point();
//        Point mButtomRight = new Point();
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(Tag, "Failed to initialize OpenCV.");
//            return null;
//        }
//        Mat mat = new Mat();
//        Utils.bitmapToMat(bitmap, mat);
//        if (!areaDetector.GetBillCorners(mat , mTopLeft, mTopRight, mButtomRight, mButtomLeft)) {
//            Log.d("Error", "Failed ot get bounding rectangle automatically.");
//            return bitmap;
//        }
//        /** Preparing Warp Perspective Dimensions **/
//        Bitmap warpedBitmap = null;
//        try{
//            Mat warpedBitmapReturned = ImageProcessingLib.WarpPerspective(mat, mTopLeft, mTopRight, mButtomRight, mButtomLeft);
//            warpedBitmap = Bitmap.createBitmap(warpedBitmapReturned.width(), warpedBitmapReturned.height(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(warpedBitmapReturned, warpedBitmap);
////            FilesHandler.SaveToJPGFile(warpedBitmap, Constants.PREPROCESSED_CAPTURED_PHOTO_PATH);
//        }
//        catch (Exception ex){
//            Log.d("Error", "Failed to warp perspective");
//            return bitmap;
//        }
//        bitmap.recycle();
//        return warpedBitmap;
//    }

    public static Mat GetWarpedBillMat(String billFullName) throws IOException {
        Bitmap bitmap = GetRotatedBill(billFullName);
        BillAreaDetector areaDetector = new BillAreaDetector();
        Point mTopLeft = new Point();
        Point mTopRight = new Point();
        Point mButtomLeft = new Point();
        Point mButtomRight = new Point();
        if (!OpenCVLoader.initDebug()) {
            Log.d(Tag, "Failed to initialize OpenCV.");
            return null;
        }
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        if (!areaDetector.GetBillCorners(mat , mTopLeft, mTopRight, mButtomRight, mButtomLeft)) {
            Log.d("Error", "Failed ot get bounding rectangle automatically.");
            return mat;
        }
        /** Preparing Warp Perspective Dimensions **/
        Mat warpedBitmapReturned;
        try{
            warpedBitmapReturned = ImageProcessingLib.WarpPerspective(mat, mTopLeft, mTopRight, mButtomRight, mButtomLeft);
//            warpedBitmap = Bitmap.createBitmap(warpedBitmapReturned.width(), warpedBitmapReturned.height(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(warpedBitmapReturned, warpedBitmap);
//            FilesHandler.SaveToJPGFile(warpedBitmap, Constants.PREPROCESSED_CAPTURED_PHOTO_PATH);
        }
        catch (Exception ex){
            Log.d("Error", "Failed to warp perspective");
            return mat;
        }
        bitmap.recycle();
        return warpedBitmapReturned;
    }
}
