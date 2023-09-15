package com.cjm.fileshare.util;

import android.icu.math.BigDecimal;
import android.os.Environment;
import android.util.Log;

import com.cjm.fileshare.bean.FileInfo;
import com.cjm.fileshare.bean.HostInfo;
import com.cjm.fileshare.config.MyApplication;
import com.cjm.fileshare.config.SystemConfig;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class FileUtil {
    private static final String TAG = "FileUtil";

    /**
     * @return 根工作路径
     */
    public static String getRootPath() {
        File appRootDocPath = (MyApplication.getContext()).getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (appRootDocPath != null && appRootDocPath.exists()) {
            return appRootDocPath.getAbsolutePath();
        } else {
            return null;
        }
    }

    public static boolean createDirInRoot(String dirAbsPath) {
        File file = new File(dirAbsPath);
        if (!file.exists()) {
            return file.mkdir();
        } else {
            return true;
        }
    }

    public static List<FileInfo> getFileInfoList(String dirPath) {
        File file = new File(dirPath);
        List<FileInfo> fileInfoList = null;
        if(file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                fileInfoList = new LinkedList<>();
                for (File f : files) {
                    fileInfoList.add(new FileInfo(f.getName(), getFileSize(f.length())));
                }
            } else {
                Log.e(TAG, "文件夹内为空!");
            }
        } else {
            Log.e(TAG, dirPath + " 路径错误!");
        }
        return fileInfoList;
    }

    private static String getFileSize(long size) {
        double value = (double) size;
        if (value < 1024) {
            return value + "B";
        } else {
            value = new BigDecimal(value / 1024).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
        }

        if (value < 1024) {
            return value + "KB";
        } else {
            value = new BigDecimal(value / 1024).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
        }
        if (value < 1024) {
            return value + "MB";
        } else {
            value = new BigDecimal(value / 1024).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
            return value + "GB";
        }

    }
}
