package com.cjm.fileshare.config;


import com.cjm.fileshare.util.FileUtil;

public class SystemConfig {
    private static String SEND_DIR_NAME = "0_send";
    private static String SAVE_DIR_NAME = "0_save";

    public static String SEND_DIR_PATH = FileUtil.getRootPath() + "/" + SEND_DIR_NAME;
    public static String SAVE_DIR_PATH = FileUtil.getRootPath() + "/" + SAVE_DIR_NAME;

    public static final int BUFFER_SIZE       = 1024 * 2;

    public static final int FILE_DIALOG_SINGLE_STYLE = 0; // 单选模式
    public static final int FILE_DIALOG_MULTI_STYLE  = 1; // 多选模式
}
