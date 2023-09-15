package com.cjm.fileshare.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;


public class PermissionUtil {
    public static final int WRITE_REQUEST = 101;
    public static boolean   WRITE_AGREE = false;

    //封装以备回心转意时复用
    public static void myRequestWritePermission(Activity activity){
        if(!WRITE_AGREE){
            int wrote = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(wrote != PackageManager.PERMISSION_GRANTED){
                activity.requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_REQUEST
                );
            }
        }
    }
}
