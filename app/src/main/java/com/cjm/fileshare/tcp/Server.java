package com.cjm.fileshare.tcp;

import android.util.Log;

import com.cjm.fileshare.bean.HostInfo;
import com.cjm.fileshare.config.InetConfig;
import com.cjm.fileshare.config.MessageConfig;
import com.cjm.fileshare.config.SystemConfig;
import com.cjm.fileshare.listener.TCPInteractionListener;
import com.cjm.fileshare.util.CloseUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class Server extends Thread {
    private static final String TAG = "Server";

    private TCPInteractionListener tcpInteractionListener;  // 用于和 activity 交互
    private ServerSocket           serverSocket;

    private HashSet<HostTask> hosts;         // 所有连接者信息
    private HostInfo          serverInfo;    // 作为服务端的信息

    private boolean toExit = false;     // 中断标志位

    public Server(HostInfo serverInfo, TCPInteractionListener tcpInteractionListener) {
        this.serverInfo             = serverInfo;
        this.tcpInteractionListener = tcpInteractionListener;
    }

    @Override
    public void run() {
        hosts = new HashSet<>();
        try {
            serverSocket = new ServerSocket(InetConfig.SERVER_PORT);
        } catch (IOException e) {
            Log.e(TAG, "绑定端口失败:" + e.getMessage());
        }

        while (!toExit) {
            Socket socket;
            try {
                socket = serverSocket.accept();         // when serverSocket.close() it will throws exception
                HostTask user = new HostTask(socket);   // 新连接
                new Thread(user).start();
                hosts.add(user);                        // 保存客户端信息
            } catch (IOException e) {
                Log.e(TAG, "服务器断开~ " + e.getMessage());
                if (hosts != null) {
                    hosts.clear();
                    hosts = null;
                }
                serverSocket = null; // 回收内存
                break;
            }
        }
        Log.i(TAG, "Server is end!!!");
    }
    /**
     * if user want to close the server, call this method;
     * it will recycle the server's resource
     */
    public void exit() {
        this.toExit = true;
        if (hosts != null) {
            hosts.clear();
            hosts = null;
        }
        CloseUtil.closeAll(serverSocket);
    }

    /**
     * 开放 API, Activity 调用 server 向 client(s) 发送消息
     * @param msg 消息实体
     */
    public void onSendInfo(String msg, boolean isFromServer) {
        msg = MessageConfig.CMD_SEND_INFO + msg; // 格式化消息格式
        handleSendInfo(msg, serverInfo, isFromServer);
    }


    /**
     * 开放 API, Activity 调用 server 向 client(s) 发送文件
     * @param filePath 文件绝对路径
     * @param targetName 目标主机名
     */
    public void onSendFile(String filePath, String targetName) {
        File file = new File(filePath);
        if (file.exists()) {
            String fileName = file.getName();
            tcpInteractionListener.onFileSendBegin(fileName);  // 显示发送文件
            DataInputStream fileDIS;
            try {
                fileDIS = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
                String stdMsg = MessageConfig.getSendFileMsg(fileName, targetName, file.length());
                handleSendFile(stdMsg, fileDIS);
                fileDIS.close();                 // 关闭文件读入流
            } catch (IOException e) {
                Log.e(TAG, "get exception: " + e.getMessage());
            }
            tcpInteractionListener.onFileSendEnd(fileName);
        } else {
            Log.e(TAG, "文件不存在!");
        }

    }


    /**
     * 处理 "发送消息" ;
     * @param msg 格式为："sendi 'content' @'target' or "sendi 'content'"
     */
    private void handleSendInfo(String msg, HostInfo sourceHost, boolean isFromServer) {
        if (msg.contains("@")) { // 独发消息
            int i = msg.indexOf(" ");
            int j = msg.lastIndexOf("@");
            String content = msg.substring(i + 1, j);  // 消息内容
            String target  = msg.substring(j + 1);     // 消息目标
            HostTask user;
            if (serverInfo.getName().equals(target)) {               // 独发给 server 本身的消息
                tcpInteractionListener.onInfoNotify(sourceHost.getName() + " : " + content);
                Log.i(TAG, "server receive msg: " + content + " from " + sourceHost.getName());
            } else if ((user = getHostByName(target)) != null) {    // 独发给其他client 的消息
                Log.i(TAG, "向 " + sourceHost.getName() + " 发送消息: " + content);
                user.sendInfoToClient(MessageConfig.RESPONSE_HEAD_MSG + sourceHost.getName() + " : " + content);
            }
        } else {  // 群发消息
            String content = msg.substring(msg.indexOf(" "));
            if (!isFromServer) {
                tcpInteractionListener.onInfoNotify(sourceHost.getName() + " : " + content);  // 通知server显示
            }
            sendInfoToOtherClients(MessageConfig.RESPONSE_HEAD_MSG + sourceHost.getName() + " : " + content, sourceHost.getName());
        }
    }

    /**
     * 向所有在线（除了本身）者发送消息：新加入 client & 消息群发
     * @param msg 消息内容
     * @param sourceHostName 消息发送者, 用于判断是否为自己
     */
    private void sendInfoToOtherClients(String msg, String sourceHostName) {
        for (HostTask client : hosts) {
            if (!client.getHostInfo().getName().equals(sourceHostName)) {
                client.sendInfoToClient(msg);
            }
        }
    }

    /**
     * 处理 发送文件的命令
     * @param msg 格式为： "sendf 'file_name' @'target' -'size'"
     * @throws IOException from sendInfoToClient
     */
    private void handleSendFile(String msg, DataInputStream sourceDataIS) throws IOException {
        String targetClientName = msg.substring(msg.lastIndexOf("@") + 1, msg.lastIndexOf("-") - 1);  // 目标 client 名字
        long fileSize = Long.parseLong(msg.substring(msg.lastIndexOf("-") + 1));                          // 文件大小

        if (targetClientName.equals(serverInfo.getName())) { // 发送给 server 本身
            String fileName = msg.substring(msg.indexOf(" ") + 1, msg.indexOf("@") - 1);
            DataOutputStream fileDOS = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(SystemConfig.SAVE_DIR_PATH, fileName))));
            tcpInteractionListener.onFileSaveBegin(fileName); //接收开始提醒
            int t;
            long psSize = 0;
            byte[] buf = new byte[SystemConfig.BUFFER_SIZE];
            while ((t = sourceDataIS.read(buf)) != -1) {
                psSize += t;
                fileDOS.write(buf, 0, t);
                fileDOS.flush();
                Log.i(TAG, "\r" + fileName +" 已经接收: " + (psSize * 100L) / fileSize + "%");
                if (psSize >= fileSize) {
                    break;
                }
            }
            fileDOS.close();    // 关闭文件输出流
            Log.i(TAG, "保存成功");
            tcpInteractionListener.onFileSaveEnd(fileName);  // 接收结束提醒
        } else {                               // 发送给 某客户端
            HostTask target = getHostByName(targetClientName);
            if (target != null) {
                msg = msg.replaceFirst(MessageConfig.CMD_SEND_FILE, MessageConfig.RESPONSE_HEAD_FILE); // "File:'file_name' @'target' -size"
                target.sendInfoToClient(msg); // 通知 目标客户端
                int t;
                int psSize = 0;
                byte[] buf = new byte[SystemConfig.BUFFER_SIZE];
                while((t = sourceDataIS.read(buf)) != -1) {  // 读取内容 from 发送端
                    target.sendFileBufToClient(buf, t);      // 写入内容 to   接收端
                    psSize += t;
                    if (psSize == fileSize) {
                        break;
                    }
                }
            } else {
                Log.e(TAG, "目标不存在!");
            }
        }
    }

    /**
     * 获取在线 client列表 （含 server 在内)
     * server 只需调用 getClientList() 即可获取 所有在线用户信息;
     * @return list of clients
     */
    public List<HostInfo> getClientList() {
        List<HostInfo> clients = new LinkedList<>();
        clients.add(serverInfo.copy());                // 添加 server
        for (HostTask user : hosts) {                  // 添加其他连接 client
            clients.add(user.getHostInfo().copy());
        }
        return clients;
    }

    /**
     * 获取在线用户信息 (包括 服务器 在内); 注意： 返回值带标识头 Users
     * @return string
     */
    private String getClientsInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(MessageConfig.RESPONSE_HEAD_USERS);              // 带 response 标识头
        sb.append(serverInfo.toString()).append("\n");             // 含 Server 在内
        for (HostTask user : hosts) {
            sb.append(user.hostInfo.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 通过 HostInfo.name 获取 在线中的 client
     * @param name from msg
     * @return HostTask or null
     */
    private HostTask getHostByName(String name) {
        for (HostTask user : hosts) {
            if (user.hostInfo.getName().equals(name)) {
                return user;
            }
        }
        return null;
    }

    private class HostTask implements Runnable {
        private final Socket     socket;
        private final HostInfo   hostInfo;                // 本客户端信息: name & ip & port
        private DataInputStream  clientDataIS = null;
        private DataOutputStream clientDataOS = null;

        private boolean isRunning = true;

        HostTask(Socket socket) {
            this.socket = socket;
            hostInfo = new HostInfo("", socket.getInetAddress().getHostAddress(), socket.getPort());
            try {
                clientDataIS = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                clientDataOS = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                String name = clientDataIS.readUTF();
                hostInfo.setName(name);
                this.sendInfoToClient(MessageConfig.RESPONSE_HEAD_NAME + serverInfo.getName());                      // 向连接的用户发送服务器的名字
                sendInfoToOtherClients(MessageConfig.RESPONSE_HEAD_JOIN + hostInfo.toString(), hostInfo.getName());  // 向其他用户群发新用户连接; Join:'content'
                tcpInteractionListener.onClientJoin(hostInfo);  // activity UI 显示
                Log.i(TAG, "获取用户 name = " + name);
            } catch (IOException e) {
                release();
                Log.e(TAG, "用户连接失败!");
            }
        }

        @Override
        public void run() {
            String msg = "";
            while(isRunning) {
                try {
                    msg = clientDataIS.readUTF();
                    int ansCode = handleMessageFromClient(msg);
                    if (ansCode == -1) { // msg is "exit"
                        break;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "连接中断: " + e.getMessage());
                    e.printStackTrace();
                    release();
                }
            }
        }

        /**
         * 处理来自客户端的 指令
         * @param msg read from client
         * @return 处理结果: 0 -> 输入为 null; -1 -> exit; 1 -> ok
         */
        private int handleMessageFromClient(String msg) throws IOException {
            Log.i(TAG, "server receive msg from client: " + msg);
            if (msg == null || "".equals(msg)) {
                return 0;
            } else if (MessageConfig.CMD_EXIT.equals(msg)) {          // exit
                socket.shutdownInput();
                socket.shutdownOutput();
                tcpInteractionListener.onClientExit(hostInfo.getName()); // 通知 Activity 有 client 离开
                release();
                return -1;
            } else if (MessageConfig.CMD_SHOW_GROUP.equals(msg)) {    // show
                sendInfoToClient(getClientsInfo());
            } else if (msg.startsWith(MessageConfig.CMD_SEND_INFO)) { // start with "sendi"
                handleSendInfo(msg, hostInfo, false);
            } else if (msg.startsWith(MessageConfig.CMD_SEND_FILE)) { // start with "sendf"
                handleSendFile(msg, clientDataIS);
            }
            return 1;
        }

        /**
         * 向 客户端socket 发送文件 buf
         */
        void sendFileBufToClient(byte[] buf, int len) {
            try {
                clientDataOS.write(buf, 0, len);
                clientDataOS.flush();
            } catch (IOException e) {
                Log.e(TAG, "发送buf 失败:" + e.getMessage());
            }

        }

        /**
         * 直接向 客户端 发送消息
         * @param msg 消息实体
         */
        void sendInfoToClient(String msg) {
            try {
                clientDataOS.writeUTF(msg);
                clientDataOS.flush();
            } catch (IOException e) {
                Log.e(TAG, "send info to client is error: " + e.getMessage());
            }

        }

        HostInfo getHostInfo() { return hostInfo; }

        /**
         * break the while & 删掉记录 & 回收资源 & 通知 Activity
         */
        private void release() {
            isRunning = false;
            sendInfoToOtherClients(MessageConfig.RESPONSE_HEAD_EXIT + hostInfo.getName(), hostInfo.getName());
            hosts.remove(this);
            CloseUtil.closeAll(clientDataIS, clientDataOS, socket);
        }
    }
}
