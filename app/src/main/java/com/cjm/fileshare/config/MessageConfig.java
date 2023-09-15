package com.cjm.fileshare.config;

public class MessageConfig {
    // client 请求头
    public static final String CMD_HELP       = "help";
    public static final String CMD_IPCONFIG   = "ipconfig";
    public static final String CMD_SHOW_GROUP = "show";
    public static final String CMD_LINK       = "link";
    public static final String CMD_SEND_INFO  = "sendi ";
    public static final String CMD_SEND_FILE  = "sendf ";
    public static final String CMD_EXIT       = "exit";

    // server 响应头
    public static final String RESPONSE_HEAD_USERS = "Users:";
    public static final String RESPONSE_HEAD_JOIN  = "Join:";
    public static final String RESPONSE_HEAD_EXIT  = "Exit:";
    public static final String RESPONSE_HEAD_MSG   = "Msg:";
    public static final String RESPONSE_HEAD_FILE  = "File:";
    public static final String RESPONSE_HEAD_NAME  = "Name:";

    public static String getSendFileMsg(String fileName, String target, long size) {
        return CMD_SEND_FILE + fileName + " @" + target + " -" + size;
    }

    public static String getCmdContent() {
        return "\t" + CMD_IPCONFIG + " -> 'ipconfig' to show local host ip(s)" + "\n" +
                "\t" + CMD_EXIT + " -> 'exit' to close tcp connection" + "\n" +
                "\t" + CMD_SHOW_GROUP + " -> 'show' to show connectors" + "\n" +
                "\t" + CMD_LINK + " -> 'link <target_ip>' to connect one friend" + "\n" +
                "\t" + CMD_SEND_INFO + " -> 'sendi xxx' @<name> or 'sendi xxx' to send messages" + "\n" +
                "\t" + CMD_SEND_FILE + " -> 'sendf <file_path> @<name>' to send file";
    }
}
