package com.cjm.fileshare.tcp;

import android.util.Log;

import com.cjm.fileshare.bean.HostInfo;
import com.cjm.fileshare.config.MessageConfig;
import com.cjm.fileshare.config.SystemConfig;
import com.cjm.fileshare.listener.TCPInteractionListener;
import com.cjm.fileshare.util.CloseUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientReceiveThread extends Thread {
    private static final String TAG = "ClientReceiveThread";

    private TCPInteractionListener tcpInteractionListener;

    private Socket          clientSocket;   // 连接 server 的 socket
    private DataInputStream socketDIS;      // socket 输入流

    private boolean isRunning = true;

    public ClientReceiveThread(Socket socket, TCPInteractionListener tcpInteractionListener) {
        this.clientSocket           = socket;
        this.tcpInteractionListener = tcpInteractionListener;
        try {
            socketDIS = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        } catch (IOException e) {
            CloseUtil.closeAll(socketDIS, clientSocket);
            isRunning = false;
        }
    }

    @Override
    public void run() {
        String msg;
        while (isRunning) {
            try {
                msg = socketDIS.readUTF();
                handleMsgFromServer(msg);
            } catch (IOException e) {
                Log.e(TAG, "read error: " + e.getMessage());
                CloseUtil.closeAll(socketDIS, clientSocket); // 异常关闭
                socketDIS    = null;
                clientSocket = null;
                break;
            }
        }
        CloseUtil.closeAll(socketDIS, clientSocket);        // 正常关闭
    }

    /**
     * 处理接收到的来自 server or 其他client 的消息, 格式为：
     *  Msg:'msg'   |   Join:'msg'  |   Exit:'msg'  |   Users:'msg' |   File:'msg'
     * @param msg 标准化格式消息
     */
    private void handleMsgFromServer(String msg) {
        if (msg == null || "".equals(msg)) {
            return;
        }
        String content = msg.substring(msg.indexOf(":") + 1);
        if(msg.startsWith(MessageConfig.RESPONSE_HEAD_NAME)) {
             HostInfo serverInfo = new HostInfo(content, clientSocket.getInetAddress().getHostAddress(), clientSocket.getPort());
             tcpInteractionListener.onConnSuccess(serverInfo);
        } else if (msg.startsWith(MessageConfig.RESPONSE_HEAD_MSG)) {   // 普通消息
            tcpInteractionListener.onInfoNotify(content);
        } else if (msg.startsWith(MessageConfig.RESPONSE_HEAD_JOIN)) {  // 客户端连接 -> Join:hostInfo.toString()
            HostInfo joinHost = HostInfo.createHostInfoByStr(content);
            tcpInteractionListener.onClientJoin(joinHost);
        } else if (msg.startsWith(MessageConfig.RESPONSE_HEAD_EXIT)) {  // 客户端离开 -> Exit:hostInfo.getName()
            tcpInteractionListener.onClientExit(content);
        } else if (msg.startsWith(MessageConfig.RESPONSE_HEAD_USERS)) { // 在线所有客户端信息
            // tcpInteractionListener.usersInfoNotify(content);
        } else if (msg.startsWith(MessageConfig.RESPONSE_HEAD_FILE)) {  // 接收文件
            receiveFile(msg);
        }
    }

    /**
     *
     * @param msg "File:'file_name' @'target' -'size'
     */
    private void receiveFile(String msg) {
        String fileName     = msg.substring(msg.indexOf(":") + 1, msg.indexOf("@") - 1);
        long   fileSize     = Long.parseLong(msg.substring(msg.lastIndexOf("-") + 1));

        tcpInteractionListener.onFileSaveBegin(fileName);   // 文件保存开始提醒

        int t;
        long psSize = 0;
        byte[] buf = new byte[SystemConfig.BUFFER_SIZE];
        DataOutputStream fileDOS = null;
        try {
            fileDOS = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(SystemConfig.SAVE_DIR_PATH, fileName))));
            while ((t = socketDIS.read(buf)) != -1) {
                psSize += t;
                fileDOS.write(buf, 0, t);
                fileDOS.flush();
                Log.i(TAG, "\r" + fileName + " 下载: " + (psSize * 100L) / fileSize + "%");
                if (psSize >= fileSize) {
                    break;
                }
            }
            fileDOS.close();      // 关闭文件输出流
            tcpInteractionListener.onFileSaveEnd(fileName); // 文件结束开始提醒
        } catch (Exception e) {
            Log.e(TAG, "文件下载失败: " + e.getMessage());
            CloseUtil.closeAll(fileDOS, socketDIS, clientSocket);
        }
    }
}
