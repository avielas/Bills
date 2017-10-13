package com.bills.billslib.Utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.bills.billslib.Contracts.Constants;
import com.bills.billslib.Contracts.Enums.LogLevel;
import com.bills.billslib.Core.BillAreaDetector;
import com.bills.billslib.Core.BillsLog;
import com.bills.billslib.Core.ImageProcessingLib;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * Created by avielavr on 5/8/2017.
 */

public class FilesHandler {
    private static String Tag = FilesHandler.class.getName();

    public static boolean SaveToPNGFile(Bitmap bmp, String path){
        FileOutputStream out = null;
        try {
            File file = new File(path);
            if(file.exists()){
                file.delete();
            }

            File folder = new File(file.getParent());
            if(!folder.exists())
            {
                Boolean isSuccess = folder.mkdirs();
                if(!isSuccess) {
                    BillsLog.Log(Tag, LogLevel.Error, "Can't create directory(ies)");
                    return false;
                }
            }
            out = new FileOutputStream(path);

            // bmp is your Bitmap instance, PNG is a lossless format, the compression factor (100) is ignored
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            BillsLog.Log(Tag, LogLevel.Info, "SaveToPNGFile success!");
            return true;
        } catch (Exception e) {
            BillsLog.Log(Tag, LogLevel.Error, e.getMessage());
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                BillsLog.Log(Tag, LogLevel.Error, "Can't free output stream: " + e.getMessage());
                return false;
            }
        }
    }

    public static boolean SaveToTXTFile(byte[] image, String fileFullName){
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(fileFullName));
            bos.write(image);
            bos.flush();
            bos.close();
        }
        catch (Exception e)
        {
            BillsLog.Log(Tag, LogLevel.Error, "Can't create directory(ies): " + e.getMessage());
            return false;
        }finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    BillsLog.Log(Tag, LogLevel.Error, "Can't free allocated buffer: " + e.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    public static byte[] ImageTxtFile2ByteArray(String path) throws IOException {
        File file = new File(path);
        int size = (int) file.length();
        byte[] image = new byte[size];
        BufferedInputStream bis = null;
        try{
            bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(image);
        }catch (Exception e){
            BillsLog.Log(Tag, LogLevel.Error, "failed to read input stream: " + e.getMessage());
            return null;
        }finally {
            if(bis != null){
                bis.close();
            }
        }
        return image;
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

    public static Mat GetRotatedBillMat(String billFullName) throws Exception {
        byte[] bytes = ImageTxtFile2ByteArray(billFullName);
        return Bytes2MatAndRotateClockwise90(bytes);
    }

    public static Mat Bytes2MatAndRotateClockwise90(byte[] bytes) throws Exception {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            throw new Exception("Failed to initialize OpenCVLoader");
        }
        Mat bgrMat = null;
        MatOfByte matOfByte = new MatOfByte(bytes);
        Mat jpegData = new Mat(1, bytes.length, CvType.CV_8UC1);
        try{
            jpegData.put(0, 0, bytes);
            bgrMat = Imgcodecs.imdecode(jpegData, Imgcodecs.IMREAD_COLOR);
            Imgproc.cvtColor(bgrMat, bgrMat, Imgproc.COLOR_RGB2BGRA, 4);
            Mat dst = new Mat(bgrMat.height(), bgrMat.width(), bgrMat.type());
            Core.flip(bgrMat.t(), dst, 1);
            return dst;
        }
        catch (Exception e){
            throw e;
        }
        finally{
            matOfByte.release();
            jpegData.release();
            bgrMat.release();
        }
    }

    /**
     * Set output stream for 'System.out.println'. The test prints just to file.
     * Read TEST_README for more info
     * @throws FileNotFoundException
     */
    public static void SetOutputStream(String filneName){
        File file = new File(filneName);
        PrintStream printStreamToFile = null;
        try {
            printStreamToFile = new PrintStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.setOut(printStreamToFile);
    }

    public static String GetLastCapturedBillPath() {
        File f = new File(Constants.IMAGES_PATH);
        File[] listFiles = f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String fileName = file.getName();
                return fileName.startsWith("ocrBytes") && fileName.endsWith(".txt");
            }
        });

        //sorting to take the last capture which took by Bills app
        Arrays.sort( listFiles, new Comparator()
        {
            public int compare(Object o1, Object o2) {
                if (((File)o1).lastModified() > ((File)o2).lastModified()) {
                    return -1;
                } else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
                    return +1;
                } else {
                    return 0;
                }
            }
        });
        return listFiles[0].getPath();
    }

    public static Bitmap ConvertFirebaseBytesToBitmap(byte[] bytes, Integer itemWidth, Integer itemHeight){
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        Bitmap commonItemBitmap = Bitmap.createBitmap(itemWidth, itemHeight, Bitmap.Config.ARGB_8888);
        commonItemBitmap.copyPixelsFromBuffer(buffer);
        return commonItemBitmap;
    }

    public static boolean SaveMatToPNGFile(Mat mat, String path){
        Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        try {
            return SaveToPNGFile(bmp, path);
        }
        catch (Exception e){
            return false;
        }
        finally {
            bmp.recycle();
        }
    }
}
