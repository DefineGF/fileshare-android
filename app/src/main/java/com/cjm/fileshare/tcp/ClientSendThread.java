package com.cjm.fileshare.tcp;

import android.util.Log;

import com.cjm.fileshare.config.MessageConfig;
import com.cjm.fileshare.config.SystemConfig;
import com.cjm.fileshare.util.CloseUtil;
import com.cjm.fileshare.util.StringUtil;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.LockSupport;

public class ClientSendThread extends Thread {
    private static final String TAG = "ClientSendThread";

    private Socket           clientSocket;   // 连接成功的 socket
    private DataOutputStream dataOS;         // socket 输出流

    private boolean          isExit = false; // 退出标志

    ClientSendThread(Socket clientSocket, String clientName) {
        this.clientSocket = clientSocket;
        try {
            dataOS = new DataOutputStream(clientSocket.getOutputStream());
            sendInfoToServer(clientName);   // client 将名字 发送给 客户端
        } catch (IOException e) {
            CloseUtil.closeAll(dataOS, clientSocket);
            Log.e(TAG, "连接中断: " + e.getMessage());
        }
    }

    /**
     * 阻塞的死循环，等待 Activity 调用开放 API
     */
    @Override
    public void run() {
        do {
            LockSupport.park();
        } while(!isExit);
        Log.i(TAG, "ClientSendThread is end");
    }

    /**
     * Client 退出：通知server & 设置标志位 & 资源回收 & 中断循环
     */
    void onExit() {
        this.sendInfoToServer(MessageConfig.CMD_EXIT); // 通知服务端 我要走啦
        isExit = true;
        CloseUtil.closeAll(dataOS, clientSocket);
        LockSupport.unpark(this);
        Log.i(TAG, "exit!");
    }

    /**
     * 开放 API, Activity 调用 onSendInfo，先标准化信息，再发送
     * @param content 真正消息实体
     */
    void onSendInfo(String content)  {
        Log.i(TAG, "向服务端发送消息: " + content);
        String msg = MessageConfig.CMD_SEND_INFO + content;
        this.sendInfoToServer(msg);
    }

    /**
     * 向服务器发送文件：通知（onSendInfo())  & 文件内容 (sendFileToServer())
     * @param fileAbsPath 发送文件的绝对路径
     * @param target 目标服务器 名字
     */
    void onSendFile(String fileAbsPath, String target) {
        File file = new File(fileAbsPath);
        if (!file.exists()) {
            Log.e(TAG, "文件不存在!");
            return;
        }
        String fileName = StringUtil.getNameFromPath(fileAbsPath);
        String notifyServerFileInfo = MessageConfig.getSendFileMsg(fileName, target, file.length()); // sendf file_name @target -o file_size
        this.sendInfoToServer(notifyServerFileInfo); // 先通知服务端
        this.sendFileToServer(file);                 // 发送文件内容
    }


    /**
     * 向服务端发送 消息
     * @param msg 标准消息内容
     */
    private void sendInfoToServer(String msg)  {
        try {
            dataOS.writeUTF(msg);
            dataOS.flush();
        } catch (IOException e) {
            CloseUtil.closeAll(dataOS, clientSocket);
            Log.e(TAG, "消息发送失败: " + e.getMessage());
        }
        Log.i(TAG, "消息发送成功!");
    }

    /**
     * 向服务端发送 文件内容
     * @param file 准备发送的文件
     */
    private void sendFileToServer(File file) {
        if (file == null)
            return;
        String fileName = file.getName();
        long   fileSize = file.length();
        int t;
        int psSize = 0;
        byte[] sdBuf = new byte[SystemConfig.BUFFER_SIZE];
        DataInputStream fileDataIS = null;
        try {
            fileDataIS = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            while ((t = fileDataIS.read(sdBuf)) != -1) {
                psSize += t;
                dataOS.write(sdBuf, 0, t);
                Log.i(TAG, "\t" + fileName + " 已经发送：" + (psSize * 100L / fileSize) + "%");
            }
            dataOS.flush();
        } catch (IOException e) {
            Log.e(TAG, "文件传输失败: " + e.getMessage());
        } finally {
            if (fileDataIS != null) {
                try {
                    fileDataIS.close();
                } catch (IOException e) {
                    Log.e(TAG, "文件关闭失败: " + e.getMessage());
                }
            }
        }
        Log.i(TAG, fileName + " 发送成功!");
    }
}
