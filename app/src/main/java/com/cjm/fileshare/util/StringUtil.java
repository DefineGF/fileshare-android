package com.cjm.fileshare.util;

import com.cjm.fileshare.config.MessageConfig;

public class StringUtil {
    /**
     * 命令行格式化
     * @param in 命令行
     * @return 格式化之后的命令行：首尾无空格 & 中间无连续空格
     */
    public static String stringStandard(String in) {
        return in.trim().replaceAll("\\s{1,}", " ");
    }

    /**
     * 命令行提取 文件路径
     * @param message such as "sendf file-path @target"
     * @return file_path
     */
    public static String getPathFromCmd(String message) { // such as sendf F://text.txt @li
        message = stringStandard(message);
        int startI = message.indexOf(" ");
        int endI   = message.lastIndexOf("@");
        return message.substring(startI + 1, endI - 1);
    }


    /**
     * 文件路径中获取文件名
     * @param path 文件路径
     * @return 文件名
     */
    public static String getNameFromPath(String path) { // 有 '\\' 和 '/' 两种形式
        int i1 = path.lastIndexOf("/");
        int i2 = path.lastIndexOf("\\");
        i1 = Math.max(i1, i2);
        return path.substring(i1 + 1);
    }

    /**
     * 生成标准的 发送给 server 的命令
     * @param msg 来自控制台的 命令行
     * @param size 文件大小
     * @return 生成通知服务端的标准信息格式
     */
    public static String generateSendFMsg(String msg, long size) {
        String path     = getPathFromCmd(msg);
        String fileName = getNameFromPath(path);
        return MessageConfig.CMD_SEND_FILE + fileName + " " + msg.substring(msg.lastIndexOf("@")) + " -" + size;
    }

}
