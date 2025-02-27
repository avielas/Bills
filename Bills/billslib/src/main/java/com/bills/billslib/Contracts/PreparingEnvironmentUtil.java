package com.bills.billslib.Contracts;

import android.content.Context;
import android.content.res.AssetManager;

import com.bills.billslib.Utilities.Utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by michaelvalershtein on 16/04/2017.
 */

public class PreparingEnvironmentUtil {
    private static final String TAG = PreparingEnvironmentUtil.class.getName();;

    public static void PrepareTesseract(Context context) {
        try {
            Utilities.CreateDirectory(Constants.IMAGES_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        copyAssetFolder(context.getAssets(), Constants.TESSDATA, Constants.TESSERACT_SAMPLE_DIRECTORY + Constants.TESSDATA);
    }

    public static boolean IsRunningOnEmulator(String buildManufacturer, String buildModel) {
        return IsGenymotionEmulator(buildManufacturer) || IsBuildModelContainsEmulatorHints(buildModel);
    }

    private static boolean IsGenymotionEmulator(String buildManufacturer) {
        return buildManufacturer != null &&
                (buildManufacturer.contains("Genymotion") || buildManufacturer.equals("unknown"));
    }

    private static boolean IsBuildModelContainsEmulatorHints(String buildModel) {
        return buildModel.startsWith("sdk")
                || "google_sdk".equals(buildModel)
                || buildModel.contains("Emulator")
                || buildModel.contains("Android SDK")
                || buildModel.contains("API");
    }

    private static boolean copyAssetFolder(AssetManager assetManager,
                                           String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            File path = new File(toPath);
            if(!path.exists()){
                path.mkdirs();
            }
            boolean res = true;
            for (String file : files) {
                if(new File(toPath + "/" + file).exists())
                    continue;
                if (file.contains("."))
                    res &= copyAsset(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                else
                    res &= copyAssetFolder(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager,
                                     String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
}
