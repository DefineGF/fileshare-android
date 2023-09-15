package com.cjm.fileshare.tcp;

import android.util.Log;

import com.cjm.fileshare.bean.HostInfo;
import com.cjm.fileshare.config.InetConfig;
import com.cjm.fileshare.listener.TCPInteractionListener;
import com.cjm.fileshare.util.CloseUtil;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.LockSupport;

/**
 * Client c = new Client();
 * c.addClientInteractionListener(xxx);
 * c.start();
 */
public class Client extends Thread {
    private static final String TAG = "Client";

    private HostInfo clientInfo;
    private Socket   clientSocket = null;
    private String   targetIP;

    private ClientSendThread        clientSendThread = null;     // client 发送进程
    private TCPInteractionListener  tcpInteractionListener;
    private boolean isExit = false;

    /**
     *
     * @param hostInfo 主机信息
     * @param targetIP server ip
     * @param tcpInteractionListener 监听
     */
    public Client(HostInfo hostInfo, String targetIP, TCPInteractionListener tcpInteractionListener) {
        this.clientInfo             = hostInfo;
        this.targetIP               = targetIP;
        this.tcpInteractionListener = tcpInteractionListener;
    }

    @Override
    public void run() {
        try {
            clientSocket = new Socket(targetIP, InetConfig.SERVER_PORT);
        } catch (IOException e) {
            CloseUtil.closeAll(clientSocket);
            Log.e(TAG, "连接失败: " + e.getMessage());
            return;
        }
        clientInfo.setPort(clientSocket.getLocalPort()); // 连接上 server 之后 可见端口

        // 发送进程
        clientSendThread  = new ClientSendThread(clientSocket, clientInfo.getName());
        clientSendThread.start();

        // 接收进程
        ClientReceiveThread clientReceiveThread = new ClientReceiveThread(clientSocket, this.tcpInteractionListener);
        clientReceiveThread.start();

        do {
            LockSupport.park();
        } while(!isExit);
        Log.i(TAG, "client is end");
    }

    /**
     * 向 Activity 提供断开 client 接口
     */
    public void exit() {
        clientSendThread.onExit();
        isExit = true;
        LockSupport.unpark(new Thread(this));
    }

    /**
     * 开放 API
     * 向 activity 提供 "向服务器发送信息的接口"
     * @param msg 消息实体
     */
    public void onSendInfo(String msg) {
        clientSendThread.onSendInfo(msg);
    }

    /**
     * 开放 API
     * 向 Activity 提供 发送文件的接口
     * @param filePath 文件绝对地址
     * @param target 文件接受者名字
     */
    public void onSendFile(String filePath, String target) {
        tcpInteractionListener.onFileSendBegin(filePath);

        clientSendThread.onSendFile(filePath, target);

        tcpInteractionListener.onFileSendEnd(filePath);
    }
}
