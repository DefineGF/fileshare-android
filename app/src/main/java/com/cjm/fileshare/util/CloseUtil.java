package com.cjm.fileshare.util;

import android.util.Log;

import java.io.Closeable;

public class CloseUtil {
    private static final String TAG = "CloseUtil";
    public static void closeAll(Closeable...io) {
        for (Closeable closeable : io) {
            try {
                if(closeable!=null) {
                    closeable.close();
                }
            }catch (Exception e) {
                Log.e(TAG, closeable.getClass() + " 回收失败: " + e.getMessage());
            }
        }
    }
}
