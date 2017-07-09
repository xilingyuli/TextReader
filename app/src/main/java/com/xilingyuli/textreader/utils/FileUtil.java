package com.xilingyuli.textreader.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by xilingyuli on 17-7-5.
 */

public class FileUtil {
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory()+ File.separator + "TextReader" + File.separator;
    public static boolean requestWritePermission(Activity activity){
        if (Build.VERSION.SDK_INT >= 23) {
            int checkPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (checkPermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                return false;
            }
        }
        return true;
    }
    public static boolean saveFile(String name, String content)
    {
        if(name==null||name.isEmpty())
            return false;
        FileOutputStream fos = null;
        try {
            File dir = new File(ROOT_PATH);
            if(!dir.exists())
                dir.mkdirs();
            File file = new File(ROOT_PATH+name);
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.flush();
            return true;
        }catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        finally {
            try {
                if(fos!=null)
                    fos.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static String readFile(String path)
    {
        if(path==null||path.isEmpty())
            return "";
        FileInputStream fis = null;
        byte[] buffer;
        try {
            fis = new FileInputStream(path);
            buffer = new byte[fis.available()];
            fis.read(buffer);
            return new String(buffer,"gbk");
        }catch (Exception e)
        {
            e.printStackTrace();
            return "";
        }
        finally {
            try {
                if(fis!=null)
                    fis.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static File[] listTxts(String path)
    {
        return new File(path).listFiles((file, s) -> s.endsWith(".txt"));
    }
    public static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                return null;
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }
}
