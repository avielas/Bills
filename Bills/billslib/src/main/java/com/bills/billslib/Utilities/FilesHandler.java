package com.bills.billslib.Utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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

    public static byte[] ReadFromTXTFile(String path) throws IOException {
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

//    public static Bitmap GetBitmapFromUri(Uri uri) {
//        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
//        Bitmap bitmap = null;
//        bitmapOptions.inMutable = true;
//        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
//        TiffBitmapFactory.Options options = new TiffBitmapFactory.Options();
//        options.inAvailableMemory = 1024 * 1024 * 10 * 3; //30 mb
//        File file = new File(uri.getPath());
//        bitmap = TiffBitmapFactory.decodeFile(file, options);
//        return bitmap;
//    }
//    public static Bitmap GetBitmapFromTifFile() {
//        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
//        Bitmap bitmap = null;
//        bitmapOptions.inMutable = true;
//        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
//        File pictureFile = new File(Constants.CAMERA_CAPTURED_PHOTO_PATH);
//
//        TiffBitmapFactory.Options options = new TiffBitmapFactory.Options();
//        options.inAvailableMemory = 1024 * 1024 * 10 * 3; //30 mb
//        bitmap = TiffBitmapFactory.decodeFile(pictureFile, options);
//        return FilesHandler.Rotating(bitmap);
//    }
//    public static boolean SaveToTIFFile(Bitmap bmp, String path) {
//        TiffSaver.SaveOptions options = new TiffSaver.SaveOptions();
//        options.author = "bills";
//        options.copyright = "bills copyright";
//        boolean saved = TiffSaver.saveBitmap(path, bmp, options);
//        return saved;
//    }
}
